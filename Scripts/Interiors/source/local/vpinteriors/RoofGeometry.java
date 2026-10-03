package local.vpinteriors;

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import viewpoint.world.TileMesh;
import zombie.iso.sprite.IsoSprite;
import zombie.tileDepth.TileDepthTextureAssignmentManager;

/** Native roof surfaces; fallback only for empty known corner meshes. */
public final class RoofGeometry {
    private static final int MAX_ALIAS_HOPS=16;
    private static final Map<Integer,float[]> VERTICES=loadData();
    private static final Map<Integer,TileMesh> MESHES=new HashMap<>();
    private static Constructor<TileMesh> constructor;
    private RoofGeometry() {}

    private static Map<Integer,float[]> loadData() {
        Map<Integer,float[]> result=new HashMap<>();
        try(InputStream stream=RoofGeometry.class.getResourceAsStream("/local/vpinteriors/roof-geometry.bin")) {
            if(stream==null)throw new IllegalStateException("roof-geometry.bin missing");
            DataInputStream in=new DataInputStream(stream);
            if(in.readInt()!=0x56505246 || in.readInt()!=1)throw new IllegalStateException("roof geometry format");
            int count=in.readInt();
            if(count<1 || count>136)throw new IllegalStateException("roof geometry count");
            for(int k=0;k<count;k++) {
                int index=in.readInt(),length=in.readInt();
                if(index<0 || index>135 || length<24 || length>8192 || length%24!=0)
                    throw new IllegalStateException("roof geometry entry");
                float[] data=new float[length];
                for(int i=0;i<length;i++) {
                    data[i]=in.readFloat();
                    if(!Float.isFinite(data[i]))throw new IllegalStateException("non-finite roof geometry");
                }
                if(result.put(index,data)!=null)throw new IllegalStateException("duplicate roof geometry");
            }
            return result;
        }catch(Exception e) {
            System.out.println("[ViewpointInteriors] Native roof geometry unavailable: "+e);
            return Map.of();
        }
    }

    /** Copy of stride8 x/y/z, original frameX/frameY, normalX/Y/Z. */
    public static float[] canonicalVertices(int index) {
        float[] data=VERTICES.get(index);
        return data==null?null:data.clone();
    }

    /** Cached canonical mesh. Useful to construct a reflected opposite slope. */
    public static synchronized TileMesh canonical(int index) {
        if(!VERTICES.containsKey(index))return null;
        TileMesh cached=MESHES.get(index);
        if(cached!=null)return cached;
        try {
            if(constructor==null) {
                constructor=TileMesh.class.getDeclaredConstructor(float[].class);
                constructor.setAccessible(true);
            }
            TileMesh mesh=constructor.newInstance((Object)VERTICES.get(index).clone());
            MESHES.put(index,mesh);
            return mesh;
        }catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Viewpoint TileMesh constructor changed",e);
        }
    }

    private static boolean knownRoof(String name) {
        return name!=null && (name.startsWith("roofs_") || name.startsWith("e_roof_snow_"));
    }

    /** Resolve native aliases without assuming texture color families share IDs. */
    public static int canonicalIndex(IsoSprite sprite) {
        if(sprite==null)return -1;
        String name=sprite.name;
        if(name==null && sprite.tilesetName!=null && sprite.tileSheetIndex>=0)
            name=sprite.tilesetName+"_"+sprite.tileSheetIndex;
        if(!knownRoof(name))return -1;
        HashSet<String> visited=new HashSet<>();
        for(int hop=0;hop<MAX_ALIAS_HOPS && knownRoof(name) && visited.add(name);hop++) {
            if(name.startsWith("roofs_01_")) {
                try {
                    int index=Integer.parseInt(name.substring("roofs_01_".length()));
                    if(VERTICES.containsKey(index))return index;
                }catch(NumberFormatException ignored) {}
            }
            name=TileDepthTextureAssignmentManager.getInstance().getAssignedTileName("game",name);
        }
        return -1;
    }

    /** Existing nonempty meshes, floors, wall flags, and crossed overlays win. */
    public static TileMesh select(IsoSprite sprite,IsoSprite crossedOverlay,TileMesh original) {
        if(original==null || original.vertCount!=0 || crossedOverlay!=null)return original;
        int index=canonicalIndex(sprite);
        if(index<8 || index>13)return original;
        TileMesh replacement=canonical(index);
        return replacement==null?original:replacement;
    }
}
