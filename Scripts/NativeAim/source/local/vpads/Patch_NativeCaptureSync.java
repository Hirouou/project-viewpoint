package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import viewpoint.core.Frame;
@Patch(className="viewpoint.models.ModelCapture",methodName="capture",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_NativeCaptureSync {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static void before(@Advice.Argument(0) Frame frame,@Advice.Argument(1) ModelSlotRenderData slot,
            @Advice.Argument(2) IsoGameCharacter player,@Advice.Argument(4) Object pose,
            @Advice.Argument(5) float x,@Advice.Argument(6) float y,@Advice.Argument(7) float z,@Advice.Argument(8) float angle){
        NativeCaptureSync.before(frame,slot,player,pose,x,y,z,angle);
    }
}
