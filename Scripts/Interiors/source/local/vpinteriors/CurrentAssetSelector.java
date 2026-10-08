package local.vpinteriors;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import viewpoint.packs.PackBind;
import viewpoint.render.PackModel;

/** Rendering-only model placement; native objects retain interaction and lights. */
public final class CurrentAssetSelector {
    private static final Map<String,PackBind> cache=new ConcurrentHashMap<>();
    public static void load(Path pack)throws Exception{
        cache.clear();
        SurfaceSupport.load(pack);
    }
    public static PackBind select(IsoSprite sprite,PackBind original){
        IsoObject object=SinkCounterSelector.current();
        if(original==null||sprite==null||object==null)return original;
        String id=SinkCounterSelector.id(original);
        if(id.startsWith("pz_rug_"))
            return replace(original,original.model,original.z-SinkCounterSelector.rise(object));
        return SurfaceSupport.select(object,sprite,original);
    }
    private static PackBind replace(PackBind source,PackModel model,float height){
        float angle=SinkCounterSelector.angle(source);
        String key=model.index+":"+angle+":"+source.x+":"+source.y+":"+height+":"+source.scale;
        if(cache.size()>1024)cache.clear();
        return cache.computeIfAbsent(key,k->new PackBind(model,angle,source.x,source.y,height,source.scale));
    }
    private CurrentAssetSelector(){}
}
