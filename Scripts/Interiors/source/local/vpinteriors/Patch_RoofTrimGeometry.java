package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import viewpoint.world.TileMesh;
import zombie.iso.sprite.IsoSprite;
@Patch(className="viewpoint.world.TileMeshes",methodName="create",warmUp=true,IKnowWhatIAmDoing=true)
public final class Patch_RoofTrimGeometry {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void exit(@Advice.Argument(0) IsoSprite sprite,@Advice.Argument(2) IsoSprite crossed,@Advice.Return(readOnly=false) TileMesh result){result=RoofTrimGeometry.select(sprite,crossed,result);}
}
