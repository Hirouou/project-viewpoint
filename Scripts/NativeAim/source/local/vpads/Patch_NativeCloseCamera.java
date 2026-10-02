package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import viewpoint.render.FrameContext;
import viewpoint.platform.GlProgram;
@Patch(className="viewpoint.render.FrameContext",methodName="camera",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_NativeCloseCamera {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.This FrameContext frame,@Advice.Argument(0) GlProgram program){NativeCloseRendering.camera(frame,program);}
}
