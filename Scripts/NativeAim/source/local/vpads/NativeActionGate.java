package local.vpads;

import java.util.*;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.animation.AnimationTrack;

/** Actions always own the real skeleton. No custom animation track is activated or paused. */
public final class NativeActionGate {
    private NativeActionGate(){}
    public static String blocked(IsoGameCharacter player){
        try {
            if(player.isDead()||player.isSeatedInVehicle())return "unavailable";
            if(player.isShoving()||player.isPerformingShoveAnimation())return "shove";
            if(player.isPerformingStompAnimation())return "stomp";
            if(player.isPerformingGrappleAnimation())return "grapple";
            if(player.getCharacterActions()!=null&&!player.getCharacterActions().isEmpty())return "timed action";
            String state=player.getAnimationStateName();
            if(state!=null&&blocksName(state))return state;
            var animation=player.getAnimationPlayer();
            if(animation!=null)for(AnimationTrack track:new ArrayList<>(animation.getMultiTrack().getTracks()))
                if(track.hasClip()&&track.getBlendWeight()>.035f&&blocksName(track.getClip().name))return track.getClip().name;
            return null;
        }catch(RuntimeException e){return "animation unavailable";}
    }
    public static boolean blocksName(String value){
        String name=value.toLowerCase(Locale.ROOT);
        for(String token:new String[]{"reload","rack","unload","loadmag","insertmag","ejectmag","jam","shove","push","stomp","grapple","climb","vault","fall","sit","bandag","eat","drink"})
            if(name.contains(token))return true;
        // Weapon firing/recoil is native and remains visible; melee swings get full control.
        return name.contains("attack")&&!(name.contains("handgun")||name.contains("rifle")||name.contains("shotgun"));
    }
}
