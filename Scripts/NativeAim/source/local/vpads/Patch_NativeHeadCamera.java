package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.characters.IsoGameCharacter;
import viewpoint.core.Frame;
@Patch(className="viewpoint.input.Controls",methodName="eye",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_NativeHeadCamera {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Argument(0) IsoGameCharacter player,@Advice.Argument(1) Frame frame){NativeHeadCamera.follow(player,frame);}
}
