package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
@Patch(className="viewpoint.packs.ModelPacks",methodName="ring",warmUp=true,IKnowWhatIAmDoing=true)
public final class Patch_PotatoPackRing {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Return(readOnly=false) int ring) throws IllegalAccessException {
        ring=PotatoPackCompatibility.ring(ring);
    }
}
