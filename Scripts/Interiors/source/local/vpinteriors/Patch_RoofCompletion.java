package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.iso.IsoChunk;
import viewpoint.world.Recipe;
@Patch(className="viewpoint.world.WorldMesher",methodName="gather",warmUp=true,IKnowWhatIAmDoing=true)
public final class Patch_RoofCompletion {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void leave(@Advice.Argument(0) IsoChunk chunk,@Advice.Argument(1) int level,@Advice.Return Recipe recipe) {
        RoofCompletion.append(recipe,chunk,level);
    }
}
