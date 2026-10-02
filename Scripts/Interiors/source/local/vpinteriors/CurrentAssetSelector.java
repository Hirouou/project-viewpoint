package local.vpinteriors;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoSprite;
import viewpoint.packs.PackBind;
import viewpoint.render.PackModel;
import viewpoint.render.PackModels;

/** Rendering-only state and height selection; native objects retain interaction and lights. */
public final class CurrentAssetSelector {
    private static final Map<String,PackModel> windows=new ConcurrentHashMap<>();
    private static final Map<String,PackBind> cache=new ConcurrentHashMap<>();
    public static void load(Path pack){
        windows.clear();cache.clear();
        for(String state:new String[]{"closed","open","broken","glass_removed"}){
            Path file=pack.resolve("pz_window_wooden_"+state+".obj");
            if(Files.isRegularFile(file))windows.put(state,PackModels.of(file.toFile()));
        }
        PackModels.publish();
    }
    public static PackBind select(IsoSprite sprite,PackBind original){
        IsoObject object=SinkCounterSelector.current();
        if(original==null||sprite==null||object==null)return original;
        String id=SinkCounterSelector.id(original);
        if(id.startsWith("pz_rug_"))
            return replace(original,original.model,original.z-SinkCounterSelector.rise(object));
        if(id.startsWith("pz_window_wooden_")){
            String state=state(object,sprite);PackModel model=windows.get(state);
            if(model!=null)return replace(original,model,original.z-SinkCounterSelector.rise(object));
        }
        return original;
    }
    static String state(IsoObject object,IsoSprite sprite){
        if(object instanceof IsoWindow w){
            if(w.isGlassRemoved())return "glass_removed";
            if(w.isSmashed()||w.isDestroyed())return "broken";
            return w.IsOpen()?"open":"closed";
        }
        if(sprite.name!=null&&sprite.name.startsWith("fixtures_windows_01_")){
            try{int index=Integer.parseInt(sprite.name.substring(20));
                return new String[]{"closed","open","broken","glass_removed"}[(index%8)/2];
            }catch(RuntimeException ignored){}
        }
        return "closed";
    }
    private static PackBind replace(PackBind source,PackModel model,float height){
        float angle=SinkCounterSelector.angle(source);
        String key=model.index+":"+angle+":"+source.x+":"+source.y+":"+height+":"+source.scale;
        if(cache.size()>1024)cache.clear();
        return cache.computeIfAbsent(key,k->new PackBind(model,angle,source.x,source.y,height,source.scale));
    }
    private CurrentAssetSelector(){}
}
