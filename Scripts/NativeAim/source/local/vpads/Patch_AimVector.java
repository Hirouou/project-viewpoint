package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.characters.IsoGameCharacter;
import zombie.core.physics.BallisticsController;

@Patch(className="zombie.core.physics.BallisticsController",methodName="updateAimingVector",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_AimVector {
    @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class,suppress=Throwable.class)
    public static boolean before(@Advice.Argument(0) IsoGameCharacter player,
                                  @Advice.Argument(1) BallisticsController.AimingVectorParameters out) {
        return AimRay.aim(player,out);
    }
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Enter boolean replaced,@Advice.Return(readOnly=false) boolean valid) {
        if(replaced)valid=true;
    }
}
