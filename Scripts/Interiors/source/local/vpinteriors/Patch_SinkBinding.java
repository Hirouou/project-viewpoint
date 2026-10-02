package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.iso.sprite.IsoSprite;
import viewpoint.packs.PackBind;
@Patch(className="viewpoint.packs.ModelPacks",methodName="bind",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_SinkBinding {
    @Advice.OnMethodExit(suppress=Throwable.class)
    public static void after(@Advice.Argument(0) Object sprite,@Advice.Return(readOnly=false) PackBind binding) {
        if(sprite instanceof IsoSprite){
            binding=SinkCounterSelector.select((IsoSprite)sprite,binding);
            binding=CurrentAssetSelector.select((IsoSprite)sprite,binding);
        }
    }
}
