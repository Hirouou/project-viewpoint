package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import viewpoint.world.TileMesh;
import viewpoint.visibility.Owner;
@Patch(className="viewpoint.world.WorldMesher",methodName="emit",warmUp=true,IKnowWhatIAmDoing=true)
public final class Patch_RoofTrimVisibility {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static int enter(@Advice.Argument(1) TileMesh mesh,@Advice.FieldValue(value="owner",readOnly=false) int owner){int previous=owner;if(RoofTrimGeometry.exterior(mesh))owner=Owner.NONE;return previous;}
    @Advice.OnMethodExit(onThrowable=Throwable.class,suppress=Throwable.class)
    public static void exit(@Advice.Enter int previous,@Advice.FieldValue(value="owner",readOnly=false) int owner){owner=previous;}
}
