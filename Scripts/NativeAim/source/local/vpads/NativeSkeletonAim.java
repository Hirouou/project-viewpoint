package local.vpads;

import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.util.vector.Matrix4f;
import zombie.characters.*;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.*;

/** Pose before native hand attachments are built: one skeleton for body, sleeves and gun. */
public final class NativeSkeletonAim {
    private static Method forward,pivot,resetSkinTransforms;
    private static Field lookPitch,lookYaw;
    private static Object lastOwner;
    private static long lastTick;
    private static float amount;
    static volatile float totalPitch,spinePitch;
    static volatile boolean active;
    static volatile String reason="idle";
    private static boolean reported;
    private NativeSkeletonAim(){}
    public static float blend(){return active?amount*amount*(3-2*amount):0;}
    static void invalidatePalettes(AnimationPlayer animation)throws ReflectiveOperationException{init();resetSkinTransforms.invoke(animation);}
    /** Align against the actual body angle passed to ModelCapture, after animation updates. */
    public static void capture(IsoPlayer player,AnimationPlayer animation,float renderedAngle)throws ReflectiveOperationException{
        init();
        float[] direction=aimDirection(player.getForwardDirectionX(),player.getForwardDirectionY(),renderedAngle,lookYaw.getFloat(null),true);
        Matrix4f[] original=new Matrix4f[animation.getModelTransformsCount()];int[] parents=new int[original.length];
        for(int i=0;i<original.length;i++){
            original[i]=new Matrix4f(animation.getModelTransformAt(i));
            SkinningBone parent=animation.getSkinningData().getBoneAt(i).parent;parents[i]=parent==null?-1:parent.index;
        }
        Matrix4f[] changed=Arrays.stream(original).map(Matrix4f::new).toArray(Matrix4f[]::new);
        NativeSightAlignment.apply(player,animation,changed,direction[0],direction[1],clamp(lookPitch.getFloat(null),(float)Math.PI/2),blend());
        Matrix4f[] locals=localChanges(original,changed,parents);
        for(int i=0;i<changed.length;i++)if(locals[i]!=null){animation.getModelTransformAt(i).load(changed[i]);animation.boneTransforms[i].mul(locals[i],new Matrix4f());}
        invalidatePalettes(animation);
    }

    public static boolean posed(AnimationPlayer animation) {
        if(!(animation.getIsoGameCharacter() instanceof IsoPlayer player)||!player.isLocalPlayer()
                ||!NativeBodyAim.wants(player)||!ViewpointADSTest.isViewpointActive())return false;
        String blocked=NativeActionGate.blocked(player);
        if(blocked!=null||!player.isAiming()){
            amount=0;active=false;reason=blocked==null?"lowered":blocked;NativeSightAlignment.status="not aimed";NativeSightAlignment.reset();NativeAimMotion.reset();lastTick=0;return true;
        }
        try {
            init();SkinningData skin=animation.getSkinningData();
            int spine=animation.getSkinningBoneIndex("Bip01_Spine",-1),upper=animation.getSkinningBoneIndex("Bip01_Spine1",-1);
            int[] arms={animation.getSkinningBoneIndex("Bip01_L_Clavicle",-1),animation.getSkinningBoneIndex("Bip01_R_Clavicle",-1),
                animation.getSkinningBoneIndex("Bip01_Prop1",-1),animation.getSkinningBoneIndex("Bip01_Prop2",-1)};
            if(skin==null||spine<0||upper<0||arms[0]<0||arms[1]<0)return false;
            long now=System.nanoTime();float dt=lastTick==0||lastOwner!=player?0:(float)Math.min(.1,(now-lastTick)*1e-9);
            if(lastOwner!=player)amount=0;lastOwner=player;lastTick=now;amount=Math.min(1,amount+dt/.18f);
            float blend=amount*amount*(3-2*amount);float pitch=clamp(lookPitch.getFloat(null),(float)Math.PI/2);
            Matrix4f[] original=new Matrix4f[animation.getModelTransformsCount()];int[] parents=new int[original.length];
            for(int i=0;i<original.length;i++){
                original[i]=new Matrix4f(animation.getModelTransformAt(i));
                SkinningBone parent=skin.getBoneAt(i).parent;parents[i]=parent==null?-1:parent.index;
            }
            boolean firstPerson=ViewpointADSTest.isFirstPerson();
            if(!firstPerson){NativeSightAlignment.reset();NativeAimMotion.reset();}
            else NativeAimMotion.update(player);
            float[] direction=aimDirection(player.getForwardDirectionX(),player.getForwardDirectionY(),
                animation.getRenderedAngle(),lookYaw.getFloat(null),firstPerson);
            int[] lowerBody={animation.getSkinningBoneIndex("Bip01_L_Thigh",-1),animation.getSkinningBoneIndex("Bip01_R_Thigh",-1),
                animation.getSkinningBoneIndex("Bip01_DressFront",-1),animation.getSkinningBoneIndex("Bip01_DressBack",-1)};
            Matrix4f[] changed=firstPerson
                ?solve(original,parents,spine,upper,arms,lowerBody,direction[0],direction[1],pitch,blend)
                :solveSpineOnly(original,parents,spine,upper,arms,lowerBody,direction[0],direction[1],pitch,blend);
            if(firstPerson)NativeSightAlignment.apply(player,animation,changed,direction[0],direction[1],pitch,blend);
            // Precompute locals before changing the native skeleton; exceptions cannot leave half a pose.
            Matrix4f[] locals=localChanges(original,changed,parents);
            for(int i=0;i<changed.length;i++)if(locals[i]!=null){
                animation.getModelTransformAt(i).load(changed[i]);
                animation.boneTransforms[i].mul(locals[i],new Matrix4f());
            }
            // Skin palettes may already have been requested by a render slot.
            // Rebuild all body/clothing palettes from these edited globals.
            invalidatePalettes(animation);
            active=true;reason=firstPerson?"aim":"third-person spine";totalPitch=(firstPerson?pitch:spineAngle(pitch))*blend;spinePitch=spineAngle(pitch)*blend;
            if(!reported){reported=true;System.out.println("[ViewpointADS] 0.4.9 native skeleton: continuous breathing/walking and real-shot recoil; native actions retain control.");}
            return true;
        }catch(ReflectiveOperationException|RuntimeException e){active=false;reason="pose unavailable";System.out.println("[ViewpointADS] Native pose fallback: "+e);return false;}
    }
    static float clamp(float v,float limit){return Math.max(-limit,Math.min(limit,v));}
    static float[] aimDirection(float bodyX,float bodyY,float renderedAngle,float cameraYaw,boolean firstPerson)throws ReflectiveOperationException{
        init();
        // CrosshairAim can turn the body toward nearby picks, and the held weapon
        // moves the aim origin on the next update. Do not feed that turn back into
        // the first-person sight target: it must remain on the camera's own ray.
        float x=firstPerson?(float)Math.cos(cameraYaw):bodyX;
        float y=firstPerson?(float)Math.sin(cameraYaw):bodyY;
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(renderedAngle))
            throw new IllegalArgumentException("Nonfinite aim direction");
        float[] out=new float[2];forward.invoke(null,x,y,renderedAngle,out);return out;
    }
    static Matrix4f[] localChanges(Matrix4f[] original,Matrix4f[] changed,int[] parents){
        Matrix4f[] locals=new Matrix4f[changed.length];
        for(int i=0;i<changed.length;i++)if(!same(original[i],changed[i])||(parents[i]>=0&&!same(original[parents[i]],changed[parents[i]])))
            locals[i]=parents[i]<0?new Matrix4f(changed[i]):Matrix4f.mul(changed[i],Matrix4f.invert(changed[parents[i]],new Matrix4f()),new Matrix4f());
        return locals;
    }
    static float spineAngle(float pitch){return clamp(pitch*.8f,(float)Math.toRadians(70));}
    static boolean under(int index,int[] parents,int... roots){
        for(int k=0;index>=0&&k<parents.length;k++,index=parents[index])for(int root:roots)if(root>=0&&index==root)return true;
        return false;
    }
    static Matrix4f[] solve(Matrix4f[] original,int[] parents,int spine,int upper,int[] arms,float fx,float fz,float pitch,float blend)throws ReflectiveOperationException{
        return solve(original,parents,spine,upper,arms,new int[0],fx,fz,pitch,blend);
    }
    static Matrix4f[] solve(Matrix4f[] original,int[] parents,int spine,int upper,int[] arms,int[] lowerBody,float fx,float fz,float pitch,float blend)throws ReflectiveOperationException{
        return solve(original,parents,spine,upper,arms,lowerBody,fx,fz,pitch,blend,true);
    }
    static Matrix4f[] solveSpineOnly(Matrix4f[] original,int[] parents,int spine,int upper,int[] arms,int[] lowerBody,float fx,float fz,float pitch,float blend)throws ReflectiveOperationException{
        return solve(original,parents,spine,upper,arms,lowerBody,fx,fz,pitch,blend,false);
    }
    private static Matrix4f[] solve(Matrix4f[] original,int[] parents,int spine,int upper,int[] arms,int[] lowerBody,float fx,float fz,float pitch,float blend,boolean alignArms)throws ReflectiveOperationException{
        init();Matrix4f[] out=Arrays.stream(original).map(Matrix4f::new).toArray(Matrix4f[]::new);
        float total=clamp(pitch,(float)Math.PI/2)*Math.max(0,Math.min(1,blend)),torso=spineAngle(clamp(pitch,(float)Math.PI/2))*Math.max(0,Math.min(1,blend));
        // Prop bones can be siblings of the clavicles. Include those branches in both torso turns.
        // The game's thighs are children of Spine, so ancestry alone includes both legs.
        turn(out,parents,new int[]{spine,arms[2],arms[3]},lowerBody,out[spine],fx,fz,torso*.35f);
        turn(out,parents,new int[]{upper,arms[2],arms[3]},lowerBody,out[upper],fx,fz,torso*.65f);
        // Third person retains the game's animated arms; they only inherit the torso bend.
        if(!alignArms)return out;
        Matrix4f shoulder=new Matrix4f();shoulder.m03=(out[arms[0]].m03+out[arms[1]].m03)*.5f;
        shoulder.m13=(out[arms[0]].m13+out[arms[1]].m13)*.5f;shoulder.m23=(out[arms[0]].m23+out[arms[1]].m23)*.5f;
        turn(out,parents,arms,lowerBody,shoulder,fx,fz,total-torso);
        // A small lift keeps the native aim pose higher; gun and both hands receive it together.
        for(int i=0;i<out.length;i++)if(under(i,parents,arms)&&!under(i,parents,lowerBody))out[i].m13+=.025f*blend;
        return out;
    }
    private static void turn(Matrix4f[] matrices,int[] parents,int[] roots,int[] excluded,Matrix4f origin,float fx,float fz,float angle)throws ReflectiveOperationException{
        if(Math.abs(angle)<1e-7f)return;Matrix4f rotation=new Matrix4f();
        pivot.invoke(null,fx,fz,origin.m03,origin.m13,origin.m23,angle,rotation);
        for(int i=0;i<matrices.length;i++)if(under(i,parents,roots)&&!under(i,parents,excluded))Matrix4f.mul(matrices[i],rotation,matrices[i]);
    }
    private static boolean same(Matrix4f a,Matrix4f b){
        return a.m00==b.m00&&a.m01==b.m01&&a.m02==b.m02&&a.m03==b.m03
            &&a.m10==b.m10&&a.m11==b.m11&&a.m12==b.m12&&a.m13==b.m13
            &&a.m20==b.m20&&a.m21==b.m21&&a.m22==b.m22&&a.m23==b.m23
            &&a.m30==b.m30&&a.m31==b.m31&&a.m32==b.m32&&a.m33==b.m33;
    }
    private static void init()throws ReflectiveOperationException{
        if(pivot!=null)return;Class<?> type=Class.forName("viewpoint.input.ArmsPitch");
        forward=type.getDeclaredMethod("forward",float.class,float.class,float.class,float[].class);forward.setAccessible(true);
        pivot=type.getDeclaredMethod("pivot",float.class,float.class,float.class,float.class,float.class,float.class,Matrix4f.class);pivot.setAccessible(true);
        resetSkinTransforms=AnimationPlayer.class.getDeclaredMethod("resetSkinTransforms");resetSkinTransforms.setAccessible(true);
        lookPitch=Class.forName("viewpoint.input.Look").getField("pitch");
        lookYaw=Class.forName("viewpoint.input.Look").getField("yaw");
    }
}
