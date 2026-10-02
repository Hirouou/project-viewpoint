package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.characters.IsoGameCharacter;
import zombie.iso.Vector3;

@Patch(className="zombie.core.physics.BallisticsController",methodName="calculateMuzzlePosition",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_AimMuzzle {
    @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class,suppress=Throwable.class)
    public static boolean before(@Advice.FieldValue("isoGameCharacter") IsoGameCharacter player,
                                  @Advice.Argument(0) Vector3 position,@Advice.Argument(1) Vector3 direction) {
        return AimRay.muzzle(player,position,direction);
    }
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Enter boolean replaced,@Advice.Return(readOnly=false) float distance) {
        if(replaced)distance=0f;
    }
}
