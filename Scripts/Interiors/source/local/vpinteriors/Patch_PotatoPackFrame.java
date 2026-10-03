package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
@Patch(className="viewpoint.packs.ModelPacks",methodName="frame",warmUp=true,IKnowWhatIAmDoing=true)
public final class Patch_PotatoPackFrame {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after() throws ReflectiveOperationException {
        PotatoPackCompatibility.frame();
    }
}
