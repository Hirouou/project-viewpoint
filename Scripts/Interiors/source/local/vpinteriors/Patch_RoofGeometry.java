package local.vpinteriors;

import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import viewpoint.world.TileMesh;
import zombie.iso.sprite.IsoSprite;

@Patch(className="viewpoint.world.TileMeshes",methodName="create",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_RoofGeometry {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Argument(0) IsoSprite sprite,
                             @Advice.Argument(2) IsoSprite crossedOverlay,
                             @Advice.Return(readOnly=false) TileMesh mesh) {
        mesh=RoofGeometry.select(sprite,crossedOverlay,mesh);
    }
}
