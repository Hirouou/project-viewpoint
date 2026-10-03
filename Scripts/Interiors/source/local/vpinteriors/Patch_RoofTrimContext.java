package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.iso.IsoGridSquare;
@Patch(className="viewpoint.world.WorldMesher",methodName="square",warmUp=true,IKnowWhatIAmDoing=true)
public final class Patch_RoofTrimContext {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static IsoGridSquare enter(@Advice.Argument(0) IsoGridSquare square){return RoofTrimGeometry.enter(square);}
    @Advice.OnMethodExit(onThrowable=Throwable.class,suppress=Throwable.class)
    public static void exit(@Advice.Enter IsoGridSquare previous){RoofTrimGeometry.restore(previous);}
}
