package local.vpads;

import java.util.*;
import org.joml.Matrix4f;
import zombie.characters.*;
import zombie.core.skinnedmodel.model.*;
import viewpoint.core.Frame;

/** PLAYER palettes are live; cached static attachments must use that same pose. */
public final class NativeCaptureSync {
    public static volatile int refreshed;
    public static volatile String sample="no capture";
    private NativeCaptureSync(){}
    static Matrix4f attachmentRows(Matrix4f nativeModel){return new Matrix4f(nativeModel).transpose();}
    public static void before(Frame frame,ModelSlotRenderData slot,IsoGameCharacter character,Object pose,float x,float y,float z,float angle){
        if(!(character instanceof IsoPlayer player)||!player.isLocalPlayer()||!NativeBodyAim.wants(player)
                ||!ViewpointADSTest.isFirstPerson()||!NativeBodyAim.active()||!"PLAYER".equals(String.valueOf(pose))
                ||NativeActionGate.blocked(player)!=null)return;
        var animation=player.getAnimationPlayer();if(animation==null||!animation.isReady())return;
        try{NativeSkeletonAim.capture(player,animation,angle);}
        catch(ReflectiveOperationException|RuntimeException e){sample="capture pose unavailable: "+e;return;}
        Map<String,Matrix4f> joints=new HashMap<>();
        for(var e:animation.getSkinningData().boneIndices.entrySet())
            joints.put(e.getKey(),NativeSightAlignment.column(animation.getModelTransformAt(e.getValue())));
        refreshed=0;
        int staticParts=0;boolean identitySeen=false;
        for(ModelInstanceRenderData part:slot.modelData){
            if(part.model==null||!part.model.isStatic)continue;staticParts++;
            if(part.modelInstance==player.primaryHandModel)identitySeen=true;
            if(!primaryAttachment(part.modelInstance,player.primaryHandModel))continue;
            Matrix4f attached=NativeSightAlignment.gunMatrix(player.primaryHandModel,joints);
            if(attached!=null&&attached.isFinite()){part.xfrm.set(attachmentRows(attached));refreshed++;}
        }
        sample="staticParts="+staticParts+",identitySeen="+identitySeen+",refreshed="+refreshed;
        // Camera head and live palette are sampled together on the main thread.
        NativeHeadCamera.followCaptured(player,frame,x,y,z,angle);
    }
    static boolean primaryAttachment(ModelInstance cached,ModelInstance live){
        if(cached==null||live==null)return false;
        if(cached==live)return true;
        // A kept render slot can retain the preceding ModelInstance object.
        // Match the same held mesh and parent bone, never a holstered copy.
        return cached.model!=null&&live.model!=null&&cached.model.mesh==live.model.mesh
            &&cached.model.mesh!=null&&Objects.equals(cached.parentBoneName,live.parentBoneName)
            &&Objects.equals(cached.attachmentNameParent,live.attachmentNameParent);
    }
}
