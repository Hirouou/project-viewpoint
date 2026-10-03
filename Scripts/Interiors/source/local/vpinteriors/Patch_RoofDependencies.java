package local.vpinteriors;

import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.iso.IsoCell;

@Patch(className="viewpoint.world.ChunkCache",methodName="update",warmUp=true,IKnowWhatIAmDoing=true)
public final class Patch_RoofDependencies {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static void enter(@Advice.Argument(1) IsoCell cell) {
        RoofDependencies.frame(cell);
    }
}
