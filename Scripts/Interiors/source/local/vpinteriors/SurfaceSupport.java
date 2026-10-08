package local.vpinteriors;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import viewpoint.packs.*;

/** Aligns approved tabletop objects to the actual authored host surface. */
public final class SurfaceSupport {
    record Datum(String model,float height) {}
    private static final Map<String,Datum> hosts=new HashMap<>(),items=new HashMap<>();
    private static final Map<String,PackBind> cache=new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> resolving=ThreadLocal.withInitial(()->false);
    public static void load(Path pack)throws Exception {
        hosts.clear();items.clear();cache.clear();Path file=pack.resolve("surface-support.properties");
        if(!Files.isRegularFile(file))return;Properties p=new Properties();
        try(var in=Files.newBufferedReader(file)){p.load(in);}
        for(String key:p.stringPropertyNames())if(key.startsWith("host.")||key.startsWith("item.")){
            String[] v=p.getProperty(key).split(",");float h=Float.parseFloat(v[1]);
            if(!Float.isFinite(h)||Math.abs(h)>8)throw new IllegalArgumentException(key);
            (key.startsWith("host.")?hosts:items).put(key.substring(5),new Datum(v[0],h));
        }
        System.out.println("[ViewpointInteriors] Measured support surfaces: "+hosts.size()+" hosts, "+items.size()+" tabletop sprites.");
    }
    public static PackBind select(IsoObject object,IsoSprite sprite,PackBind original) {
        if(object==null||sprite==null||original==null||object.getSquare()==null||resolving.get())return original;
        Datum item=items.get(sprite.name);if(item==null||!item.model.equals(SinkCounterSelector.id(original)))return original;
        resolving.set(true);
        try {
            float support=Float.NaN;var objects=object.getSquare().getObjects();
            for(int i=0;i<objects.size();i++){
                IsoObject host=objects.get(i);if(host==null||host.getSprite()==null)continue;
                // Attached tabletop sprites share their host object and its render offset.
                if(host==object&&sprite==host.getSprite())continue;
                Datum datum=hosts.get(host.getSprite().name);if(datum==null)continue;
                PackBind binding=ModelPacks.bind(host.getSprite());
                if(binding==null||binding.model.state()!=3)continue;
                String selected=SinkCounterSelector.id(binding);
                if(!datum.model.equals(selected)&&!selected.equals(datum.model+"__sink_inset_oval")&&!selected.equals(datum.model+"__sink_inset_rect"))continue;
                float candidate=SinkCounterSelector.rise(host)+binding.z+datum.height*binding.scale;
                if(!Float.isFinite(support)||candidate>support)support=candidate;
            }
            if(!Float.isFinite(support))return original;
            float height=support-item.height*original.scale-SinkCounterSelector.rise(object);
            String key=original.model.index+":"+original.cos+":"+original.sin+":"+original.x+":"+original.y+":"+height+":"+original.scale;
            if(cache.size()>2048)cache.clear();
            return cache.computeIfAbsent(key,k->new PackBind(original.model,SinkCounterSelector.angle(original),original.x,original.y,height,original.scale));
        } finally {resolving.set(false);}
    }
    private SurfaceSupport(){}
}
