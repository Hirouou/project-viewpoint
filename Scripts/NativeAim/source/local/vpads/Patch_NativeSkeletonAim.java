package local.vpads;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
@Patch(className="viewpoint.input.ArmsPitch",methodName="posed",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_NativeSkeletonAim {
    @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class,suppress=Throwable.class)
    public static boolean before(@Advice.Argument(0) AnimationPlayer animation){return NativeSkeletonAim.posed(animation);}
}
