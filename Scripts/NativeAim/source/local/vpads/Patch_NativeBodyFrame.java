package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
@Patch(className="viewpoint.SceneDrawer",methodName="render",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_NativeBodyFrame {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static void before(@Advice.This Object drawer){NativeBodyAim.beginFrame(drawer);}
    @Advice.OnMethodExit(onThrowable=Throwable.class,suppress=Throwable.class)
    public static void after(){NativeBodyAim.endFrame();}
}
