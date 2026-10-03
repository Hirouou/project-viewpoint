package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import viewpoint.world.TileMesh;
import zombie.iso.sprite.IsoSprite;
@Patch(className="viewpoint.world.WorldMesher",methodName="overlay",warmUp=true,IKnowWhatIAmDoing=true)
public final class Patch_RoofTrimOverlay {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static void enter(@Advice.Argument(2) IsoSprite part,@Advice.Argument(value=3,readOnly=false) TileMesh mesh,@Advice.Argument(value=4,readOnly=false) boolean wall){TileMesh profile=RoofTrimGeometry.profile(part);if(profile!=null){mesh=profile;wall=false;}}
}
