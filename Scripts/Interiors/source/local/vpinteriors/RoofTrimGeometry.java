package local.vpinteriors;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import viewpoint.world.TileMesh;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.sprite.IsoSprite;

/** Exact native trim IDs, thin physical profiles and shared original frame UV. */
public final class RoofTrimGeometry {
    private static final Map<String,Integer> PLANE_DATA=new HashMap<>();
    private static final Map<String,float[]> DATA=load();
    private static final Map<String,TileMesh> MESHES=new HashMap<>();
    private static final IdentityHashMap<TileMesh,Integer> PLANES=new IdentityHashMap<>();
    private static final ThreadLocal<IsoGridSquare> CONTEXT=new ThreadLocal<>();
    private static Constructor<TileMesh> constructor;
    private RoofTrimGeometry(){}
    private static Map<String,float[]> load(){
        Map<String,float[]> data=new HashMap<>();
        try(InputStream stream=RoofTrimGeometry.class.getResourceAsStream("/local/vpinteriors/roof-trim-geometry.bin")){
            if(stream==null)throw new IOException("roof-trim-geometry.bin absent");DataInputStream in=new DataInputStream(stream);
            if(in.readInt()!=0x56505452||in.readInt()!=2)throw new IOException("trim geometry header");
            int count=in.readInt();if(count<1||count>512)throw new IOException("trim count");
            for(int k=0;k<count;k++){
                String name=in.readUTF();int planes=in.readUnsignedByte(),length=in.readInt();
                if(!name.startsWith("roofs_accents_")&&!name.startsWith("object_snow_overlay_03_"))throw new IOException("trim ID");
                if(length<24||length>16384||length%24!=0)throw new IOException("trim vertex length");
                float[] vv=new float[length];for(int j=0;j<length;j++){vv[j]=in.readFloat();if(!Float.isFinite(vv[j]))throw new IOException("trim finite vertex");}
                if(data.put(name,vv)!=null)throw new IOException("duplicate trim ID");
                if(planes<1||planes>3)throw new IOException("trim wall planes");PLANE_DATA.put(name,planes);
            }
            return Collections.unmodifiableMap(data);
        }catch(Exception e){System.out.println("[ViewpointInteriors] Roof trim unavailable: "+e);return Map.of();}
    }
    public static Set<String> names(){return DATA.keySet();}
    public static float[] vertices(String name){float[] data=DATA.get(name);return data==null?null:data.clone();}
    public static synchronized TileMesh mesh(String name){
        float[] data=DATA.get(name);if(data==null)return null;TileMesh cached=MESHES.get(name);if(cached!=null)return cached;
        try{
            if(constructor==null){constructor=TileMesh.class.getDeclaredConstructor(float[].class);constructor.setAccessible(true);}
            TileMesh result=constructor.newInstance((Object)data.clone());
            MESHES.put(name,result);PLANES.put(result,PLANE_DATA.get(name));return result;
        }catch(ReflectiveOperationException e){throw new IllegalStateException("TileMesh API changed",e);}
    }
    private static String name(IsoSprite sprite){
        if(sprite==null)return null;if(sprite.name!=null)return sprite.name;
        return sprite.tilesetName==null?null:sprite.tilesetName+"_"+sprite.tileSheetIndex;
    }
    public static TileMesh profile(IsoSprite sprite){return mesh(name(sprite));}
    public static TileMesh select(IsoSprite sprite,IsoSprite crossed,TileMesh original){
        if(sprite==null||crossed!=null||sprite.solidfloor)return original;TileMesh fixed=profile(sprite);return fixed==null?original:fixed;
    }
    public static IsoGridSquare enter(IsoGridSquare square){IsoGridSquare previous=CONTEXT.get();if(square==null)CONTEXT.remove();else CONTEXT.set(square);return previous;}
    public static void restore(IsoGridSquare square){if(square==null)CONTEXT.remove();else CONTEXT.set(square);}
    private static boolean outsideAcross(IsoGridSquare square,IsoDirections direction){IsoGridSquare adjacent=square.getAdjacentSquare(direction);return adjacent!=null&&adjacent.isOutside();}
    /** Missing streamed neighbors are uncertain. Only proved outside edges unmask. */
    public static boolean exterior(TileMesh mesh){
        Integer planes; synchronized(RoofTrimGeometry.class){planes=PLANES.get(mesh);}if(planes==null)return false;
        IsoGridSquare q=CONTEXT.get();if(q==null)return false;if(q.isOutside())return true;
        return (planes&1)!=0&&outsideAcross(q,IsoDirections.N)||(planes&2)!=0&&outsideAcross(q,IsoDirections.W);
    }
}
