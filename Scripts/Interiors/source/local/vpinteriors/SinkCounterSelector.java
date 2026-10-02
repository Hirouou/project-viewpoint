package local.vpinteriors;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import viewpoint.packs.PackBind;
import viewpoint.packs.ModelPacks;
import viewpoint.render.PackModel;
import viewpoint.render.PackModels;

/** Rendering-only substitution. Never changes map sprites, plumbing or saves. */
public final class SinkCounterSelector {
    private static final ThreadLocal<ArrayDeque<IsoObject>> contexts=ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Boolean> resolving=ThreadLocal.withInitial(()->false);
    private static final Map<String,PackModel> cavities=new ConcurrentHashMap<>();
    private static final Map<String,PackBind> cache=new ConcurrentHashMap<>();
    public static void load(Path pack) {
        cavities.clear(); cache.clear();
        String[] styles={"modern","wooden","steel","birchwood","oak","dark","green","white","dark_oak","trailer"};
        for(String style:styles)for(String profile:new String[]{"inset_oval","inset_rect"}) {
            String id="pz_counter_"+style+"_floor__sink_"+profile;
            Path file=pack.resolve(id+".obj");
            if(Files.isRegularFile(file))cavities.put(id,PackModels.of(file.toFile()));
        }
        PackModels.publish();
        System.out.println("[ViewpointInteriors] Sink selector ready: "+cavities.size()+" countertop variants.");
    }
    public static void begin(IsoObject object){if(object!=null)contexts.get().push(object);}
    public static void end(){ArrayDeque<IsoObject> q=contexts.get();if(!q.isEmpty())q.pop();}
    public static IsoObject current(){return contexts.get().peek();}
    public static PackBind select(IsoSprite sprite,PackBind original) {
        if(original==null||sprite==null||resolving.get()||contexts.get().isEmpty())return original;
        resolving.set(true);
        try {
            IsoObject object=contexts.get().peek();String model=id(original);
            if(isCounter(model)) {
                String profile=sinkOn(object);
                if(profile==null&&object.getSquare()!=null) {
                    var objects=object.getSquare().getObjects();
                    for(int i=0;i<objects.size();i++) {
                        profile=sinkOn(objects.get(i));if(profile!=null)break;
                    }
                }
                if(profile!=null) {
                    PackModel cavity=cavities.get(model+"__sink_"+profile);
                    if(cavity!=null)return replace(original,cavity,angle(original),original.x,original.y,original.z,original.scale);
                }
            } else if(profile(sprite)!=null) {
                // An inset basin is already authored at countertop height.
                // WorldMesher adds its sprite renderYOffset separately; align
                // to the host's origin so that height is not added twice.
                IsoObject host=counterOn(object);
                if(host==null&&object.getSquare()!=null) {
                    var objects=object.getSquare().getObjects();
                    for(int i=0;i<objects.size();i++) {host=counterOn(objects.get(i));if(host!=null)break;}
                }
                if(host!=null) {
                    PackBind b=ModelPacks.bind(host.getSprite());
                    if(b!=null&&isCounter(id(b)))return replace(original,original.model,angle(b),b.x,b.y,b.z+rise(host)-rise(object),b.scale);
                }
            }
            return original;
        } finally {resolving.set(false);}
    }
    private static IsoObject counterOn(IsoObject object) {
        if(object==null||object.getSprite()==null)return null;
        PackBind b=ModelPacks.bind(object.getSprite());return b!=null&&isCounter(id(b))?object:null;
    }
    private static String sinkOn(IsoObject object) {
        if(object==null)return null;String p=profile(object.getSprite());
        if(p!=null&&ModelPacks.bind(object.getSprite())!=null)return p;
        if(object.getAttachedAnimSprite()!=null)for(IsoSpriteInstance instance:object.getAttachedAnimSprite()) {
            if(instance!=null) {p=profile(instance.parentSprite);if(p!=null&&ModelPacks.bind(instance.parentSprite)!=null)return p;}
        }
        p=profile(object.getOverlaySprite());return p!=null&&ModelPacks.bind(object.getOverlaySprite())!=null?p:null;
    }
    static String profile(IsoSprite sprite) {
        if(sprite==null||sprite.name==null||!sprite.name.startsWith("fixtures_sinks_01_"))return null;
        try {int i=Integer.parseInt(sprite.name.substring("fixtures_sinks_01_".length()));
            if(i>=0&&i<=3||i>=20&&i<=23)return "inset_oval";
            if(i>=4&&i<=11||i>=16&&i<=19)return "inset_rect";
        }catch(NumberFormatException ignored){}
        return null;
    }
    static float rise(IsoObject object){return object.getRenderYOffset()/96f*2.4494896f;}
    static String id(PackBind b){String f=b.model.obj.getName();return f.endsWith(".obj")?f.substring(0,f.length()-4):f;}
    static boolean isCounter(String model){return model.startsWith("pz_counter_")&&model.endsWith("_floor");}
    static float angle(PackBind b){return (float)Math.toDegrees(Math.atan2(b.sin,b.cos));}
    private static PackBind replace(PackBind b,PackModel model,float angle,float x,float y,float z,float scale) {
        String key=model.index+":"+angle+":"+x+":"+y+":"+z+":"+scale;
        if(cache.size()>1024)cache.clear();
        return cache.computeIfAbsent(key,k->new PackBind(model,angle,x,y,z,scale));
    }
    private SinkCounterSelector(){}
}
