package local.vpinteriors;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoSprite;
import viewpoint.packs.*;
import viewpoint.render.PackModel;
import viewpoint.render.PackModels;
import viewpoint.visibility.Edges;
import viewpoint.visibility.Owner;
import viewpoint.world.Recipe;
import viewpoint.world.TileMesh;

/** Caixilhos no pacote de modelos; vidro no mesmo passe translúcido das janelas nativas. */
public final class WindowAssets {
    static final int GLASS=1024;
    static final Map<String,Asset> assets=new ConcurrentHashMap<>();
    static final Map<String,String[]> states=new ConcurrentHashMap<>();
    private static final Map<String,PackBind> selected=new ConcurrentHashMap<>();
    private static Constructor<?> meshConstructor;
    private static Method parse,vertices,indices;
    private static Field pages,ops,owner,pending,gatherPending;
    static final class Asset {
        final String sprite,frameId;
        final PackModel frame;
        final TileMesh glass;
        final Path png;
        final float extraHeight;
        volatile Texture texture;
        Asset(String sprite,String frameId,PackModel frame,TileMesh glass,Path png,float extraHeight){
            this.sprite=sprite;this.frameId=frameId;this.frame=frame;this.glass=glass;this.png=png;
            this.extraHeight=extraHeight;
        }
    }
    static Field field(Class<?> type,String name)throws Exception{
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }
    static void access()throws Exception{
        if(meshConstructor!=null)return;
        Class<?> reader=Class.forName("viewpoint.packs.ObjReader"),mesh=Class.forName("viewpoint.packs.ObjReader$Mesh");
        parse=reader.getDeclaredMethod("parse",List.class);parse.setAccessible(true);
        vertices=mesh.getDeclaredMethod("vertices");vertices.setAccessible(true);
        indices=mesh.getDeclaredMethod("indices");indices.setAccessible(true);
        meshConstructor=TileMesh.class.getDeclaredConstructor(float[].class);meshConstructor.setAccessible(true);
        pages=field(Recipe.class,"pages");pending=field(Recipe.class,"pending");
        gatherPending=field(Class.forName("viewpoint.world.WorldMesher"),"pending");
        ops=field(Class.forName("viewpoint.world.Recipe$Batch"),"ops");
        owner=field(Class.forName("viewpoint.world.Recipe$Op"),"owner");
    }
    static Path within(Path root,String value)throws java.io.IOException{
        Path result=root.resolve(value).normalize();
        if(!result.startsWith(root)||!Files.isRegularFile(result))throw new java.io.IOException("Missing window asset "+value);
        return result;
    }
    public static void load(Path pack)throws Exception{
        assets.clear();states.clear();selected.clear();
        Path contract=pack.resolve("window-states.properties");
        if(!Files.isRegularFile(contract))return;
        access();Properties data=new Properties();
        try(var in=Files.newBufferedReader(contract)){data.load(in);}
        pack=pack.toAbsolutePath().normalize();
        for(String key:data.stringPropertyNames())if(key.startsWith("asset.")){
            String sprite=key.substring(6);String[] values=data.getProperty(key).split(",",-1);
            if(values.length!=4)throw new IllegalArgumentException("Window asset needs frame, glass, PNG, height: "+sprite);
            Path frame=within(pack,values[0]+".obj");
            TileMesh glass=values[1].isEmpty()?null:readGlass(within(pack,values[1]));
            Path png=glass==null?null:within(pack,values[2]);
            assets.put(sprite,new Asset(sprite,values[0],PackModels.of(frame.toFile()),glass,png,Float.parseFloat(values[3])));
        }
        for(String key:data.stringPropertyNames())if(key.startsWith("window.")){
            String[] variants=data.getProperty(key).split(",",-1);
            if(variants.length!=4)throw new IllegalArgumentException("Four native window states required: "+key);
            for(String v:variants)if(!v.isEmpty()&&!assets.containsKey(v))throw new IllegalArgumentException("Undelivered window state "+v);
            states.put(key.substring(7),variants);
        }
        PackModels.publish();
        System.out.println("[ViewpointInteriors] Window assets loaded: "+assets.size()+" sprite variants; native transparent-glass pass.");
    }
    static TileMesh readGlass(Path obj)throws Exception{
        access();Object mesh=parse.invoke(null,Files.readAllLines(obj));
        float[] v=(float[])vertices.invoke(mesh);int[] ix=(int[])indices.invoke(mesh);
        if(ix.length==0||ix.length%3!=0)throw new IllegalArgumentException("No glass triangles: "+obj);
        // SurfacePass.translucent explicitly disables face culling. The editable
        // glass contains front/back copies for Blender; drawing both copies in
        // this pass would apply the same alpha twice. Retain one side of a flat
        // pane and let the native pass make it visible from either direction.
        ArrayList<Integer> corners=new ArrayList<>();
        float nx=v[ix[0]*8+3],ny=v[ix[0]*8+4],nz=v[ix[0]*8+5];
        boolean flat=true;
        for(int corner:ix){int at=corner*8;
            if(Math.abs(nx*v[at+3]+ny*v[at+4]+nz*v[at+5])<.999f)flat=false;
        }
        for(int i=0;i<ix.length;i+=3){int at=ix[i]*8;
            if(!flat||nx*v[at+3]+ny*v[at+4]+nz*v[at+5]>0)
                for(int k=0;k<3;k++)corners.add(ix[i+k]);
        }
        float[] out=new float[corners.size()*8];
        for(int i=0;i<corners.size();i++){
            int source=corners.get(i)*8,at=i*8;
            // ObjReader produces Viewpoint render axes (-X,Z,-Y). TileMesh is
            // converted into those axes by Cook, so undo that conversion here.
            out[at]=-v[source];out[at+1]=v[source+1];out[at+2]=-v[source+2];
            out[at+3]=v[source+6];out[at+4]=v[source+7];
            out[at+5]=-v[source+3];out[at+6]=v[source+4];out[at+7]=-v[source+5];
        }
        return (TileMesh)meshConstructor.newInstance((Object)out);
    }
    static Asset asset(IsoObject object,IsoSprite sprite){
        if(sprite==null||sprite.name==null)return null;
        Asset current=assets.get(sprite.name);if(current==null)return null;
        if(!(object instanceof IsoWindow))return current;
        String[] variants=states.get(sprite.name);if(variants==null)return current;
        String state=CurrentAssetSelector.state(object,sprite);
        int index=state.equals("open")?1:state.equals("broken")?2:state.equals("glass_removed")?3:0;
        Asset chosen=assets.get(variants[index]);return chosen==null?current:chosen;
    }
    public static PackBind select(IsoObject object,IsoSprite sprite,PackBind original){
        Asset current=sprite==null?null:assets.get(sprite.name);
        if(current==null||original==null||!SinkCounterSelector.id(original).equals(current.frameId))return original;
        Asset a=asset(object,sprite);if(a==null)return original;
        // All native directions have their own coordinates, with rotation zero.
        float z=original.z+a.extraHeight-SinkCounterSelector.rise(object);
        String key=a.frame.index+":"+original.x+":"+original.y+":"+z+":"+original.scale;
        if(selected.size()>2048)selected.clear();
        return selected.computeIfAbsent(key,k->new PackBind(a.frame,0,original.x,original.y,z,original.scale));
    }
    static float[] mapping(Texture texture){
        float x=texture.getXStart(),y=texture.getYStart();
        return new float[]{x,y,texture.getXEnd(),texture.getYEnd(),x,y,0,0,1,
            texture.getXEnd()-x,texture.getYEnd()-y};
    }
    static void place(Recipe recipe,TextureID texture,TileMesh mesh,float[] map,float x,float y,float z,int square,IsoSprite sprite)throws Exception{
        Recipe.place(recipe,texture,mesh,map,x,y,z,GLASS);
        Object batch=((Map<?,?>)pages.get(recipe)).get(texture);
        List<?> entries=(List<?>)ops.get(batch);
        owner.setInt(entries.get(entries.size()-1),Owner.of(square,Edges.ownerKind(sprite)));
    }
    public static void append(Recipe recipe,IsoObject object,IsoSprite sprite,float x,float y,float z,int square){
        Asset a=asset(object,sprite);if(recipe==null||a==null||a.glass==null)return;
        PackBind binding=ModelPacks.bind(sprite);
        if(binding==null||!SinkCounterSelector.id(binding).equals(a.frameId))return;
        try{
            Texture texture=a.texture;
            if(texture==null){texture=Texture.getSharedTexture(a.png.toString().replace('\\','/'));a.texture=texture;}
            if(texture==null||!texture.isReady()||texture.getTextureId()==null){
                pending.setBoolean(recipe,true);
                // WorldMesher copies its pending array over Recipe.pending at
                // the end of gathering; mark both so async texture load retries.
                ((boolean[])gatherPending.get(null))[0]=true;
                return;
            }
            place(recipe,texture.getTextureId(),a.glass,mapping(texture),x+binding.x,y+binding.y,z+binding.z,square,sprite);
        }catch(Exception e){System.out.println("[ViewpointInteriors] Window glass unavailable for "+a.sprite+": "+e);}
    }
    private WindowAssets(){}
}
