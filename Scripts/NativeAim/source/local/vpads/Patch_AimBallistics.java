package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.characters.IsoGameCharacter;
import zombie.core.physics.BallisticsController;
import zombie.iso.Vector3;

@Patch(className="zombie.core.physics.BallisticsController",methodName="update",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_AimBallistics {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.This BallisticsController controller,
                             @Advice.FieldValue("isoGameCharacter") IsoGameCharacter player,
                             @Advice.FieldValue("isInitialized") boolean initialized,
                             @Advice.FieldValue("convertedMuzzleDirection") Vector3 converted,
                             @Advice.FieldValue("targetPosition") Vector3 target) {
        AimRay.update(controller,player,initialized,converted,target);
    }
}
