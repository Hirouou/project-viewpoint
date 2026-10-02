package local.vpads;

import java.nio.file.*;
import java.util.*;
import org.joml.*;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.*;
import zombie.scripting.objects.ModelAttachment;

/** Aligns the actual mesh's rear/front sights, moving native hands and attachment bones together. */
public final class NativeSightAlignment {
    record Sights(Vector3f rear,Vector3f front,Vector3f up){}
    record Pose(Map<String,Matrix4f> joints,Matrix4f weapon){}
    private static final Map<String,Sights> sights=new HashMap<>();
    private static final Map<String,GripLock> authoredGrips=new HashMap<>();
    public static volatile String status="not aimed";
    public static volatile float sightDistance=Float.NaN;
    public static volatile boolean handgunGrip;
    static String rejection="";
    private static Object sessionOwner,sessionWeapon;
    private static final DistanceLock distanceLock=new DistanceLock();
    private static final GripLock gripLock=new GripLock();
    private NativeSightAlignment(){}
    /** Keep a feasible distance for the entire hold; idle animation must not pump the gun. */
    static final class DistanceLock {
        private float value=Float.NaN;
        void reset(){value=Float.NaN;}
        float choose(float low,float high){
            if(!Float.isFinite(low)||!Float.isFinite(high)||low>high)throw new IllegalArgumentException("Invalid arm reach interval");
            float margin=java.lang.Math.min(.015f,(high-low)*.25f);
            if(!Float.isFinite(value)||value<low||value>high){
                float wanted=Float.isFinite(value)?value:.22f;
                value=java.lang.Math.max(low+margin,java.lang.Math.min(high-margin,wanted));
            }
            return value;
        }
    }
    static final class GripLock {
        private Matrix4f right,left;
        void reset(){right=null;left=null;}
        void referenceRelative(Matrix4f r,Matrix4f l){right=new Matrix4f(r);left=new Matrix4f(l);}
        void reference(Map<String,Matrix4f> joints,Matrix4f gun){
            Matrix4f inverse=new Matrix4f(gun).invert();
            right=new Matrix4f(inverse).mul(joints.get("Bip01_R_Hand"));
            left=new Matrix4f(inverse).mul(joints.get("Bip01_L_Hand"));
        }
        Matrix4f right(Map<String,Matrix4f> joints,Matrix4f gun){
            return right==null?joints.get("Bip01_R_Hand"):new Matrix4f(gun).mul(right);
        }
        Matrix4f hand(Map<String,Matrix4f> joints,Matrix4f gun,float blend){
            // Authored grip, never a random frame of the independent support track.
            return left==null?joints.get("Bip01_L_Hand"):new Matrix4f(gun).mul(left);
        }
    }
    static void reset(){distanceLock.reset();gripLock.reset();sessionOwner=null;sessionWeapon=null;sightDistance=Float.NaN;handgunGrip=false;}
    public static void load(Path file)throws Exception{
        sights.clear();for(String line:Files.readAllLines(file)){if(line.isBlank())continue;String[] a=line.split("\\|");sights.put(a[0],new Sights(vector(a[1]),vector(a[2]),vector(a[3])));}
        authoredGrips.clear();
        for(String line:Files.readAllLines(file.resolveSibling("handgun-grips.txt"))){
            if(line.isBlank())continue;String[] a=line.split("\\|");if(a.length!=3)throw new IllegalArgumentException("Handgun grip record");
            GripLock grip=new GripLock();grip.referenceRelative(matrix(a[1]),matrix(a[2]));
            if(authoredGrips.put(a[0],grip)!=null)throw new IllegalArgumentException("Duplicate handgun grip");
        }
    }
    static Matrix4f matrix(String text){
        String[] fields=text.split(",");if(fields.length!=16)throw new IllegalArgumentException("Grip matrix size");
        float[] values=new float[16];for(int i=0;i<16;i++)values[i]=Float.parseFloat(fields[i]);
        Matrix4f m=new Matrix4f().set(values);
        if(!m.isFinite()||java.lang.Math.abs(m.determinant()-1)>1e-3f)throw new IllegalArgumentException("Grip must use native rotation axes");
        return m;
    }
    static GripLock authoredGrip(String item){
        GripLock source=authoredGrips.get(item);if(source==null)return null;
        GripLock copy=new GripLock();copy.referenceRelative(source.right,source.left);return copy;
    }
    static Vector3f vector(String s){String[] a=s.split(",");return new Vector3f(Float.parseFloat(a[0]),Float.parseFloat(a[1]),Float.parseFloat(a[2]));}
    static Matrix4f column(org.lwjgl.util.vector.Matrix4f a){return new Matrix4f(a.m00,a.m10,a.m20,a.m30,a.m01,a.m11,a.m21,a.m31,a.m02,a.m12,a.m22,a.m32,a.m03,a.m13,a.m23,a.m33);}
    static void row(Matrix4f a,org.lwjgl.util.vector.Matrix4f b){
        b.m00=a.m00();b.m10=a.m01();b.m20=a.m02();b.m30=a.m03();
        b.m01=a.m10();b.m11=a.m11();b.m21=a.m12();b.m31=a.m13();
        b.m02=a.m20();b.m12=a.m21();b.m22=a.m22();b.m32=a.m23();
        b.m03=a.m30();b.m13=a.m31();b.m23=a.m32();b.m33=a.m33();
    }
    static Matrix4f gunMatrix(ModelInstance gun,AnimationPlayer animation,org.lwjgl.util.vector.Matrix4f[] posed){
        Map<String,Matrix4f> joints=new HashMap<>();
        for(var entry:animation.getSkinningData().boneIndices.entrySet())joints.put(entry.getKey(),column(posed[entry.getValue()]));
        return gunMatrix(gun,joints);
    }
    static Matrix4f gunMatrix(ModelInstance gun,Map<String,Matrix4f> joints){
        ModelInstance parent=gun.parent;if(parent==null||gun.model==null)return null;
        ModelAttachment mount=parent.getAttachmentById(gun.attachmentNameParent);
        if(mount==null&&gun.parentBoneName!=null)mount=parent.getAttachmentById(gun.parentBoneName);
        String bone=mount==null?gun.parentBoneName:mount.getBone();Matrix4f joint=joints.get(bone);
        if(joint==null)return null;Matrix4f out=new Matrix4f(joint),temporary=new Matrix4f();
        if(mount!=null)out.mul(ModelInstanceRenderData.makeAttachmentTransform(mount,temporary));
        ModelAttachment self=gun.getAttachmentById(gun.attachmentNameSelf);
        if(self==null&&gun.parentBoneName!=null)self=gun.getAttachmentById(gun.parentBoneName);
        if(self!=null){ModelInstanceRenderData.makeAttachmentTransform(self,temporary);if(ModelInstanceRenderData.invertAttachmentSelfTransform)temporary.invert();out.mul(temporary);}
        ModelInstanceRenderData.postMultiplyMeshTransform(out,gun.model.mesh);
        if(gun.scale!=1)out.scale(gun.scale);return out;
    }
    public static void apply(IsoPlayer player,AnimationPlayer animation,org.lwjgl.util.vector.Matrix4f[] posed,float fx,float fz,float pitch,float blend){
        status="unsupported weapon";if(player.getPrimaryHandItem()==null){reset();return;}
        if(sessionOwner!=player||sessionWeapon!=player.getPrimaryHandItem()){
            reset();sessionOwner=player;sessionWeapon=player.getPrimaryHandItem();
        }
        Sights markers=sights.get(player.getPrimaryHandItem().getFullType());ModelInstance gun=player.primaryHandModel;
        if(markers==null||gun==null)return;
        Matrix4f gunModel=gunMatrix(gun,animation,posed);int head=animation.getSkinningBoneIndex("Bip01_Head",-1);
        if(gunModel==null||head<0){status="attachment unavailable";return;}
        Map<String,Matrix4f> joints=new LinkedHashMap<>();for(var entry:animation.getSkinningData().boneIndices.entrySet())joints.put(entry.getKey(),column(posed[entry.getValue()]));
        // Viewpoint Camera.eye: head + .10 vertical + .12 horizontal facing, at model scale 1.5.
        Vector3f eye=column(posed[head]).getTranslation(new Vector3f()).add(fx*.08f,.1f/1.5f,fz*.08f);
        Vector3f direction=new Vector3f(fx*(float)java.lang.Math.cos(pitch),(float)java.lang.Math.sin(pitch),fz*(float)java.lang.Math.cos(pitch));
        Vector3f up=new Vector3f(-fx*(float)java.lang.Math.sin(pitch),(float)java.lang.Math.cos(pitch),-fz*(float)java.lang.Math.sin(pitch));
        handgunGrip=!player.getPrimaryHandItem().isTwoHandWeapon();
        if(handgunGrip&&gripLock.right==null){
            GripLock reference=authoredGrips.get(player.getPrimaryHandItem().getFullType());
            if(reference==null){status="handgun grip calibration unavailable";return;}
            // These are native-axis wrists relative to the approved actual gun
            // object, including its editable object offset. No Blender-axis
            // Prop1 is passed into the game's attachment reconstruction.
            gripLock.referenceRelative(reference.right,reference.left);
        }
        NativeAimMotion.Target motion=NativeAimMotion.target(eye,direction,up);
        Pose corrected=align(joints,gunModel,markers,motion.eye(),motion.direction(),motion.up(),blend,distanceLock,handgunGrip?gripLock:null);
        if(corrected==null){status="sight target beyond arm reach";return;}
        for(var entry:animation.getSkinningData().boneIndices.entrySet())row(corrected.joints().get(entry.getKey()),posed[entry.getValue()]);
        status=blend>=.999f?"rear/front sights aligned to eye":"raising sight alignment";
    }
    static Matrix3f basis(Vector3f f,Vector3f up){Vector3f right=new Vector3f(f).cross(up).normalize(),y=new Vector3f(right).cross(f).normalize();return new Matrix3f().setColumn(0,right).setColumn(1,y).setColumn(2,new Vector3f(f).negate());}
    static Pose align(Map<String,Matrix4f> joints,Matrix4f gun,Sights marker,Vector3f eye,Vector3f direction,Vector3f up,float blend){
        return align(joints,gun,marker,eye,direction,up,blend,null);
    }
    static Pose align(Map<String,Matrix4f> joints,Matrix4f gun,Sights marker,Vector3f eye,Vector3f direction,Vector3f up,float blend,DistanceLock hold){
        return align(joints,gun,marker,eye,direction,up,blend,hold,null);
    }
    static Pose align(Map<String,Matrix4f> joints,Matrix4f gun,Sights marker,Vector3f eye,Vector3f direction,Vector3f up,float blend,DistanceLock hold,GripLock grip){
        rejection="";Vector3f rear=gun.transformPosition(marker.rear,new Vector3f()),front=gun.transformPosition(marker.front,new Vector3f());
        Vector3f sourceForward=new Vector3f(front).sub(rear).normalize(),sourceUp=gun.transformDirection(marker.up,new Vector3f()).normalize();
        Matrix3f rotation=basis(new Vector3f(direction).normalize(),up).mul(basis(sourceForward,sourceUp).transpose());
        Matrix4f delta=new Matrix4f(rotation);Vector3f rotatedRear=rotation.transform(rear,new Vector3f());
        Matrix4f left=grip==null?joints.get("Bip01_L_Hand"):grip.hand(joints,gun,blend);
        Matrix4f right=grip==null?joints.get("Bip01_R_Hand"):grip.right(joints,gun);
        float low=.06f,high=.50f;
        // Choose one reachable sight distance for BOTH hands, keeping the grip relationship intact.
        for(String side:new String[]{"R","L"}){
            String prefix="Bip01_"+side+"_";
            Matrix4f a=joints.get(prefix+"UpperArm"),b=joints.get(prefix+"Forearm"),c=joints.get(prefix+"Hand");if(a==null||b==null||c==null)return null;
            Vector3f shoulder=a.getTranslation(new Vector3f()),elbow=b.getTranslation(new Vector3f()),hand=c.getTranslation(new Vector3f());
            float reach=shoulder.distance(elbow)+elbow.distance(hand)-.0001f;
            Vector3f gripHand=(side.equals("L")?left:right).getTranslation(new Vector3f());
            Vector3f offset=rotation.transform(gripHand,new Vector3f()).sub(rotatedRear).add(eye).sub(shoulder);
            float projection=offset.dot(direction),disc=projection*projection-offset.lengthSquared()+reach*reach;
            if(disc<0){rejection=side+" disc="+disc+" offset="+offset+" reach="+reach;return null;}float root=(float)java.lang.Math.sqrt(disc);low=java.lang.Math.max(low,-projection-root);high=java.lang.Math.min(high,-projection+root);
        }
        if(low>high){rejection="no shared reach interval "+low+".."+high;return null;}
        float distance=hold==null?java.lang.Math.max(low,java.lang.Math.min(high,.22f)):hold.choose(low,high);
        if(hold==distanceLock)sightDistance=distance;
        delta.setTranslation(new Vector3f(eye).fma(distance,direction).sub(rotatedRear));
        Map<String,Matrix4f> goals=new LinkedHashMap<>();for(String side:new String[]{"R","L"}){String hand="Bip01_"+side+"_Hand";goals.put(hand,new Matrix4f(delta).mul(side.equals("L")?left:right));}
        var result=grip==null?NativeArmIK.align(joints,goals,blend)
            :NativeArmIK.align(joints,goals,blend,cameraRight(direction,up),up);
        if(grip!=null&&grip.right!=null){
            // Props are separate animation branches. Put the held gun at the same
            // target as the calibrated wrists instead of inheriting idle prop drift.
            for(String name:new String[]{"Bip01_Prop1"})
                if(joints.containsKey(name))result.joints().put(name,new Matrix4f(delta).mul(joints.get(name)));
        }
        return new Pose(result.joints(),new Matrix4f(delta).mul(gun));
    }
    // ModelCapture.place has determinant < 0 (scale -X). A screen-right
    // vector expressed in native model coordinates is UP cross FORWARD.
    static Vector3f cameraRight(Vector3f direction,Vector3f up){return new Vector3f(up).cross(direction).normalize();}
}
