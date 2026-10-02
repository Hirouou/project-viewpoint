package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
@Patch(className="viewpoint.core.View",methodName="fovY",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_ADSZoom {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Argument(0) boolean thirdPerson,@Advice.Return(readOnly=false) float fov){
        if(!thirdPerson&&NativeBodyAim.wants())fov=(float)(2*Math.atan(Math.tan(fov*.5)/(1+.15*NativeBodyAim.aimBlend())));
    }
}
