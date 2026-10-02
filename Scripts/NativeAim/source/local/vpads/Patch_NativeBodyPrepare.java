package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import org.joml.Matrix4f;
@Patch(className="viewpoint.render.WorldRenderer",methodName="begin",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_NativeBodyPrepare {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static void before(@Advice.Argument(0) Object scene,@Advice.Argument(1) Matrix4f view){NativeBodyAim.prepare(scene,view);}
}
