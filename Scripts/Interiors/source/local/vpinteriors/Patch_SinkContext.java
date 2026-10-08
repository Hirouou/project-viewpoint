package local.vpinteriors;
import me.zed_0xff.zombie_buddy.Patch;
import net.bytebuddy.asm.Advice;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import viewpoint.world.Recipe;
@Patch(className="viewpoint.world.PackGather",methodName="object",warmUp=true,IKnowWhatIAmDoing=true)
public class Patch_SinkContext {
    @Advice.OnMethodEnter(suppress=Throwable.class)
    public static int enter(@Advice.Argument(0) IsoObject object,@Advice.FieldValue("recipe") Recipe recipe){
        if(object==null)return -1;
        SinkCounterSelector.begin(object);return ExteriorAssets.count(recipe);
    }
    @Advice.OnMethodExit(onThrowable=Throwable.class,suppress=Throwable.class)
    public static void leave(@Advice.Enter int firstModel,@Advice.Return boolean modelled,
        @Advice.Thrown Throwable thrown,@Advice.FieldValue("recipe") Recipe recipe,
        @Advice.Argument(0) IsoObject object,@Advice.Argument(1) IsoSprite sprite,
        @Advice.Argument(2) float x,@Advice.Argument(3) float y,@Advice.Argument(4) float z,
        @Advice.Argument(5) int square){
        try{if(modelled&&thrown==null){
            ExteriorAssets.preserve(recipe,firstModel,object,sprite,square);
        }}
        finally{if(firstModel>=0)SinkCounterSelector.end();}
    }
}
