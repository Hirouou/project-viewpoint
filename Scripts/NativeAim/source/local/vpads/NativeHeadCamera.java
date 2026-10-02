package local.vpads;
import zombie.characters.*;
import zombie.core.skinnedmodel.model.Model;
import zombie.iso.Vector3;
import viewpoint.core.Frame;
/** Uses the same animated head as the real body, including pickup, crouch and stomp. */
public final class NativeHeadCamera {
    private NativeHeadCamera(){}
    static void followCaptured(IsoPlayer player,Frame frame,float x,float y,float z,float angle){
        var animation=player.getAnimationPlayer();int index=animation.getSkinningBoneIndex("Bip01_Head",-1);if(index<0)return;
        var head=animation.getModelTransformAt(index);
        Vector3 position=new Vector3();position.set(head.m03,head.m13,head.m23);
        Model.vectorToWorldCoords(x,y,z,angle,position);
        frame.eyeX=frame.camX-position.x;frame.eyeZ=frame.camY-position.y;
        frame.eyeY=(position.z-frame.camZ)*2.4494896f+.10f;frame.eyeLean=0;
    }
    public static void follow(IsoGameCharacter player,Frame frame){
        if(!(player instanceof IsoPlayer local)||!local.isLocalPlayer()||!ViewpointADSTest.isFirstPerson()
                ||player.isDead()||player.isSeatedInVehicle()||!player.hasAnimationPlayer())return;
        var animation=player.getAnimationPlayer();if(!animation.isReady())return;
        int head=animation.getSkinningBoneIndex("Bip01_Head",-1);if(head<0)return;
        Vector3 position=new Vector3();Model.boneToWorldCoords(player,head,position);
        if(!Float.isFinite(position.x)||!Float.isFinite(position.y)||!Float.isFinite(position.z)
                ||Math.abs(position.x-player.getX())>2||Math.abs(position.y-player.getY())>2||Math.abs(position.z-player.getZ())>2)return;
        frame.eyeX=frame.camX-position.x;frame.eyeZ=frame.camY-position.y;
        frame.eyeY=(position.z-frame.camZ)*2.4494896f+.10f;
        frame.eyeLean=0;
    }
}
