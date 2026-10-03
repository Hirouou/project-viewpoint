package local.vpinteriors;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import viewpoint.packs.ModelPacks;
import viewpoint.packs.PackBind;
import viewpoint.platform.LiveSettings;
import zombie.iso.sprite.IsoSprite;

/** Keep only this project's nearby authored assets in the otherwise sprite-only Potato preset. */
public final class PotatoPackCompatibility {
    private static final String MOD_ID="ViewpointFurnitureFix";
    private static final int NEAR_RING=1;
    private static final Field PRESET=field("viewpoint.platform.GraphicsPresets","preset");
    private static final Field RING=field("viewpoint.packs.ModelPacks","RING");
    private static final Field PACKS=field("viewpoint.packs.ModelPacks","packs");
    private static final Field MOD=field("viewpoint.packs.ModelPacks$Pack","modId");
    private static final Field ON=field("viewpoint.packs.ModelPacks$Pack","on");
    private static final Field BINDS=field("viewpoint.packs.ModelPacks$Pack","binds");
    private static final Method REBIND=method("viewpoint.packs.ModelPacks","rebind");
    private static final int POTATO=potatoIndex();
    private static final LiveSettings.Toggle ENABLED=LiveSettings.toggle(
        "vpinteriors.potatoModels","Project models in Potato","World/Model packs",true);
    private static volatile int generation=Integer.MIN_VALUE;
    private static volatile Map<String,PackBind> owned=Map.of();
    private static boolean scoped;
    private static boolean scopeKnown;

    static {
        ENABLED.describe("Draw this project's authored models in the nearest chunk ring in Potato. "
            +"Other packs keep the preset's sprite fallback. Disable to keep Potato fully sprite-only.");
    }

    private static Field field(String type,String name) {
        try {Field value=Class.forName(type).getDeclaredField(name);value.setAccessible(true);return value;}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static Method method(String type,String name) {
        try {Method value=Class.forName(type).getDeclaredMethod(name);value.setAccessible(true);return value;}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static int potatoIndex() {
        try {
            String[] names=(String[])field("viewpoint.platform.GraphicsPresets","NAMES").get(null);
            for(int i=0;i<names.length;i++)if("Potato".equals(names[i]))return i;
            return -1;
        }catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    private static boolean requested() throws IllegalAccessException {
        Object selected=PRESET.get(null);
        return ENABLED.get()&&POTATO>=0&&selected instanceof LiveSettings.Choice
            &&((LiveSettings.Choice)selected).get()==POTATO
            &&((LiveSettings.Number)RING.get(null)).getInt()==0;
    }
    private static Map<String,PackBind> projectBindings() throws IllegalAccessException {
        int current=ModelPacks.generation();
        if(generation==current)return owned;
        synchronized(PotatoPackCompatibility.class) {
            if(generation!=current) {
                Map<String,PackBind> names=new HashMap<>();
                for(Object pack:(List<?>)PACKS.get(null))
                    if(MOD_ID.equals(MOD.get(pack))&&ON.getBoolean(pack))
                        for(var entry:((Map<?,?>)BINDS.get(pack)).entrySet())
                            if(entry.getKey() instanceof String&&entry.getValue() instanceof PackBind)
                                names.put((String)entry.getKey(),(PackBind)entry.getValue());
                owned=Map.copyOf(names);
                generation=current;
            }
            return owned;
        }
    }
    public static int ring(int original) throws IllegalAccessException {
        if(original>=0||!requested())return original;
        return projectBindings().isEmpty()?original:NEAR_RING;
    }
    public static PackBind binding(Object sprite,PackBind original) throws ReflectiveOperationException {
        if(!requested())return original;
        Map<String,PackBind> project=projectBindings();
        if(project.isEmpty())return original;
        String name=sprite instanceof String?(String)sprite:
            sprite instanceof IsoSprite?((IsoSprite)sprite).name:null;
        return name==null?null:project.get(name);
    }
    /** Publish a normal pack generation change, so native ChunkCache also rebuilds on equal-radius transitions. */
    public static void frame() throws ReflectiveOperationException {
        boolean next=requested()&&!projectBindings().isEmpty();
        boolean changed=scopeKnown?next!=scoped:next;
        scoped=next;scopeKnown=true;
        if(changed) {
            REBIND.invoke(null);
        }
    }
}
