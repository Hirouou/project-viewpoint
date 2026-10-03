package local.vpinteriors;
import java.lang.reflect.*;
import java.util.*;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.core.textures.Texture;
import viewpoint.world.TileMesh;

/** Reuses the actual native TileMeshes hull for perpendicular intersection tests. */
public final class RoofNativeVertices {
    private static final Map<IsoSprite,float[]> cache=new IdentityHashMap<>();
    private static final Map<Integer,float[]> canonical=new HashMap<>();
    private static Method create;private static Object cell;
    public static synchronized float[] vertices(RoofCompletionPlan.Tile tile) {
        if(tile.roof()>=0)return canonical.computeIfAbsent(tile.roof(),RoofGeometry::canonicalVertices);
        if(!(tile.tag() instanceof IsoObject object))return null;
        IsoSprite sprite=object.getSprite();if(sprite==null)return null;
        Object current=zombie.iso.IsoWorld.instance.currentCell;
        if(cell!=current||cache.size()>4096) {cache.clear();cell=current;}
        float[] held=cache.get(sprite);if(held!=null)return held;
        try {
            Texture texture=sprite.getTextureForCurrentFrame(object.getDir(),object);
            if(texture==null||!texture.isReady())return null;
            if(create==null) {create=Class.forName("viewpoint.world.TileMeshes").getDeclaredMethod("create",IsoSprite.class,Texture.class,IsoSprite.class);create.setAccessible(true);}
            TileMesh mesh=(TileMesh)create.invoke(null,sprite,texture,sprite);
            if(mesh==null||mesh.data.length==0)return null;
            held=mesh.data.clone();cache.put(sprite,held);return held;
        }catch(ReflectiveOperationException failure) {return null;}
    }
    private RoofNativeVertices() {}
}
