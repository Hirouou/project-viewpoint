package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;

@Patch(className="zombie.CombatManager",methodName="fireWeapon",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_NativeShotMotion {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Argument(0) HandWeapon gun,@Advice.Argument(1) IsoGameCharacter player){NativeAimMotion.fired(gun,player);}
}
