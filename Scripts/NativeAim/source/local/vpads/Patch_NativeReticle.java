package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
/** Hide the screen reticle only for this companion's first-person iron-sight mode. */
@Patch(className="viewpoint.input.Controls",methodName="skipReticle",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_NativeReticle {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Argument(0) boolean localPlayer,@Advice.Return(readOnly=false) boolean skip){
        if(localPlayer&&NativeBodyAim.wants()&&NativeBodyAim.isAiming()&&ViewpointADSTest.isFirstPerson()
                &&NativeSightAlignment.status.equals("rear/front sights aligned to eye"))skip=true;
    }
}
