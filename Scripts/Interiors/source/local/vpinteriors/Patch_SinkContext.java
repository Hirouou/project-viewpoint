package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.iso.IsoObject;
@Patch(className="viewpoint.world.PackGather",methodName="object",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_SinkContext {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static boolean enter(@Advice.Argument(0) IsoObject object){SinkCounterSelector.begin(object);return object!=null;}
    @Advice.OnMethodExit(onThrowable=Throwable.class,suppress=Throwable.class)
    public static void leave(@Advice.Enter boolean pushed){if(pushed)SinkCounterSelector.end();}
}
