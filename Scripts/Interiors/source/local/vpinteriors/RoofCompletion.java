package local.vpinteriors;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.iso.*;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.tileDepth.TileDepthTextureAssignmentManager;
import viewpoint.visibility.Owner;
import viewpoint.world.*;

/** Appends only roofs justified by a bounded native gable/footprint component. */
public final class RoofCompletion {
    private static final float STOREY=2.4494896f;
    private static final int HALO=8;
    private static Constructor<TileMesh> constructor;
    private static Field pending,batches,ops,page,owner,opMap,dx,dy,height;
    private static final Map<Integer,List<CachedMesh>> meshes=new ConcurrentHashMap<>();
    private static final Map<ChunkLevel,Retry> retries=new ConcurrentHashMap<>();
    private static IsoCell retryCell;
    private static boolean warned;
    private record CachedMesh(float[] data,TileMesh mesh) {}
    private record Point(int x,int y,int z) {}
    private record ChunkLevel(int x,int y,int z) {}
    private record Retry(int generation,int count) {}
    static Field field(Class<?> c,String name)throws Exception { Field f=c.getDeclaredField(name);f.setAccessible(true);return f; }
    private static synchronized void access()throws Exception {
        if(constructor!=null)return;
        constructor=TileMesh.class.getDeclaredConstructor(float[].class);constructor.setAccessible(true);
        pending=field(Recipe.class,"pending");batches=field(Recipe.class,"batches");
        Class<?> b=Class.forName("viewpoint.world.Recipe$Batch"),o=Class.forName("viewpoint.world.Recipe$Op");
        ops=field(b,"ops");page=field(b,"page");owner=field(o,"owner");opMap=field(o,"map");
        dx=field(o,"dx");dy=field(o,"dy");height=field(o,"height");
    }
    private static TileMesh mesh(float[] data)throws Exception {
        int hash=Arrays.hashCode(data);List<CachedMesh> bucket=meshes.computeIfAbsent(hash,k->Collections.synchronizedList(new ArrayList<>()));
        synchronized(bucket) {
            for(CachedMesh held:bucket)if(Arrays.equals(held.data,data))return held.mesh;
            TileMesh m=constructor.newInstance((Object)data);bucket.add(new CachedMesh(data.clone(),m));return m;
        }
    }
    private static boolean waterName(String name) {
        return name!=null && ((name.startsWith("roofs_") && !name.startsWith("roofs_accents_")) || name.startsWith("e_roof_snow_"));
    }
    private static int gableIndex(String name) {
        if(name==null || !(name.startsWith("walls_exterior_roofs_") || name.startsWith("walls_burnt_roofs_")))return -1;
        try {
            int index=Integer.parseInt(name.substring(name.lastIndexOf('_')+1));
            return index>=24 && index<48?index-24:index;
        }catch(NumberFormatException e) { return -1; }
    }
    static int nativeRoofIndex(IsoSprite sprite) {
        if(sprite==null)return -1;
        String name=sprite.name;
        if(!waterName(name))return -1;
        Set<String> seen=new HashSet<>();
        for(int hop=0;hop<16 && waterName(name) && seen.add(name);hop++) {
            // A cap's depth assignment can point to ordinary slope1 or3.
            // Capture its native structural role before following that alias.
            if(name.equals("roofs_01_14"))return 14;
            if(name.equals("roofs_01_15"))return 15;
            name=TileDepthTextureAssignmentManager.getInstance().getAssignedTileName("game",name);
        }
        return RoofGeometry.canonicalIndex(sprite);
    }
    private static final class WorldGrid implements RoofCompletionPlan.Grid {
        final IsoChunk current;final IsoCell cell;final Map<Point,RoofCompletionPlan.Cell> cache=new HashMap<>();
        final Set<RoofDependencies.Chunk> missingChunks=new LinkedHashSet<>();
        WorldGrid(IsoChunk current) { this.current=current;cell=IsoWorld.instance.currentCell; }
        public RoofCompletionPlan.Cell at(int x,int y,int z) {
            return cache.computeIfAbsent(new Point(x,y,z),p->read(p.x,p.y,p.z));
        }
        public void missing(int x,int y,int z) {
            missingChunks.add(new RoofDependencies.Chunk(Math.floorDiv(x,8),Math.floorDiv(y,8)));
        }
        private RoofCompletionPlan.Cell read(int x,int y,int z) {
            int cx=Math.floorDiv(x,8),cy=Math.floorDiv(y,8);
            IsoChunk chunk=cx==current.wx && cy==current.wy?current:cell.getChunk(cx,cy);
            if(chunk==null || (chunk!=current && !chunk.loaded))return RoofCompletionPlan.Cell.UNKNOWN;
            IsoGridSquare square=chunk.getGridSquare(Math.floorMod(x,8),Math.floorMod(y,8),z);
            if(square==null)return RoofCompletionPlan.Cell.AIR;
            boolean floor=false,north=false,west=false;List<RoofCompletionPlan.Tile> tiles=new ArrayList<>();
            for(int i=0;i<square.getObjects().size();i++) {
                IsoObject object=square.getObjects().get(i);IsoSprite sprite=object.getSprite();
                if(sprite==null)continue;
                floor|=sprite.solidfloor;
                north|=sprite.cutN||sprite.getProperties().has(IsoFlagType.doorN);
                west|=sprite.cutW||sprite.getProperties().has(IsoFlagType.doorW);
                int canonical=nativeRoofIndex(sprite),gable=gableIndex(sprite.name);
                if(canonical<0 && waterName(sprite.name))canonical=-2;
                if(canonical>=0 || canonical==-2 || gable>=0)
                    tiles.add(new RoofCompletionPlan.Tile(sprite.name,canonical,gable,object));
            }
            return new RoofCompletionPlan.Cell(true,floor,north,west,List.copyOf(tiles));
        }
    }
    private static Texture texture(RoofCompletionPlan.Tile source) {
        if(source==null || !(source.tag() instanceof IsoObject object))return null;
        IsoSprite sprite=object.getSprite();if(sprite==null)return null;
        if(object.isUseSnowSprite())sprite=sprite.getSnowSprite();
        return sprite==null?null:sprite.getTextureForCurrentFrame(object.getDir(),object);
    }
    private static boolean ready(Texture t) { return t!=null && t.isReady() && t.getTextureId()!=null; }
    private static void pending(Recipe recipe)throws Exception { pending.setBoolean(recipe,true); }
    static void resetRetry(int x,int y,int level) { retries.remove(new ChunkLevel(x,y,level)); }
    private static void retry(Recipe recipe,IsoChunk chunk,int level,boolean missing)throws Exception {
        IsoCell cell=IsoWorld.instance.currentCell;
        if(retryCell!=cell) { retries.clear();retryCell=cell; }
        ChunkLevel key=new ChunkLevel(chunk.wx,chunk.wy,level);
        if(!missing) { retries.remove(key);return; }
        if(retries.size()>4096)retries.clear();
        Retry r=retries.get(key);int gen=chunk.adjacentChunkLoadedCounter;
        if(r==null || r.generation!=gen)r=new Retry(gen,0);
        // Initial retries are bounded. Missing component dependencies separately
        // invalidate this target when they stream in, including distant chunks.
        if(r.count<3) { pending(recipe);retries.put(key,new Retry(gen,r.count+1)); }
    }
    private static boolean allReady(List<RoofCompletionMesh.Piece> pieces) {
        for(var piece:pieces)if(!ready(texture(piece.source())))return false;
        return !pieces.isEmpty();
    }
    private static boolean inChunk(IsoChunk chunk,RoofCompletionMesh.Piece piece) {
        return Math.floorDiv((int)Math.floor(piece.x()),8)==chunk.wx
            && Math.floorDiv((int)Math.floor(piece.y()),8)==chunk.wy;
    }
    private static List<RoofCompletionMesh.Piece> localPieces(IsoChunk chunk,List<RoofCompletionMesh.Piece> pieces) {
        List<RoofCompletionMesh.Piece> result=new ArrayList<>();
        for(var piece:pieces)if(inChunk(chunk,piece))result.add(piece);
        return result;
    }
    private static boolean localCap(WorldGrid grid,IsoChunk chunk,int level) {
        for(int y=chunk.wy*8;y<chunk.wy*8+8;y++)for(int x=chunk.wx*8;x<chunk.wx*8+8;x++)
            for(var tile:grid.at(x,y,level).tiles())if(tile.roof()==14 || tile.roof()==15)return true;
        return false;
    }
    private static void emit(Recipe recipe,IsoChunk chunk,int level,RoofCompletionMesh.Piece piece)throws Exception {
        // Ownership follows the new roof surface, rather than the original
        // isometric source's chunk. Each target Recipe emits this piece once.
        if(!inChunk(chunk,piece))return;
        Texture t=texture(piece.source());if(!ready(t)) { pending(recipe);return; }
        TileMesh m=mesh(piece.vertices());
        Recipe.place(recipe,t.getTextureId(),m,WorldMesher.textureMapping(t),piece.x()-chunk.wx*8,
            piece.y()-chunk.wy*8,level*STOREY+piece.lift(),0);
        for(Object batch:(List<?>)batches.get(recipe))if(page.get(batch)==t.getTextureId()) {
            List<?> entries=(List<?>)ops.get(batch);
            if(!entries.isEmpty()) {
                Object last=entries.get(entries.size()-1);
                if(Math.abs(dx.getFloat(last)-(piece.x()-chunk.wx*8))<1e-6f && Math.abs(dy.getFloat(last)-(piece.y()-chunk.wy*8))<1e-6f
                    && Math.abs(height.getFloat(last)-(level*STOREY+piece.lift()))<1e-6f)owner.setInt(last,Owner.NONE);
            }
        }
    }
    private static void removeCaps(Recipe recipe,IsoChunk chunk,int level,RoofCompletionPlan.Plan plan,WorldGrid grid)throws Exception {
        for(int y=chunk.wy*8;y<chunk.wy*8+8;y++)for(int x=chunk.wx*8;x<chunk.wx*8+8;x++) {
            for(var tile:grid.at(x,y,level).tiles()) {
                if(!plan.containsCap(x,y,level,tile.roof()))continue;
                Texture t=texture(tile);if(!ready(t)) { pending(recipe);continue; }
                float[] map=WorldMesher.textureMapping(t);float px=x-chunk.wx*8+.5f,py=y-chunk.wy*8+.5f;
                for(Object batch:(List<?>)batches.get(recipe))if(page.get(batch)==t.getTextureId()) {
                    @SuppressWarnings("unchecked") List<Object> entries=(List<Object>)ops.get(batch);
                    Iterator<Object> it=entries.iterator();
                    while(it.hasNext()) {
                        Object op=it.next();
                        if(Math.abs(dx.getFloat(op)-px)<1e-6f && Math.abs(dy.getFloat(op)-py)<1e-6f
                            && Math.abs(height.getFloat(op)-level*STOREY)<1e-6f && Arrays.equals((float[])opMap.get(op),map))it.remove();
                    }
                }
            }
        }
    }
    public static void append(Recipe recipe,IsoChunk chunk,int level) {
        if(recipe==null || chunk==null || level<1 || IsoWorld.instance==null || IsoWorld.instance.currentCell==null)return;
        try {
            access();WorldGrid grid=new WorldGrid(chunk);
            int x=chunk.wx*8,y=chunk.wy*8;
            var found=RoofCompletionPlan.find(grid,level,x-HALO,y-HALO,x+7+HALO,y+7+HALO);
            boolean missing=found.pending();
            for(var plan:found.plans()) {
                var pieces=localPieces(chunk,RoofCompletionMesh.pieces(plan,RoofGeometry::canonicalVertices));
                if(!allReady(pieces)) { if(!pieces.isEmpty())pending(recipe);continue; }
                for(var piece:pieces)emit(recipe,chunk,level,piece);
            }
            var components=RoofComponentPlan.find(grid,RoofNativeVertices::vertices,level,x-HALO,y-HALO,x+7+HALO,y+7+HALO);
            missing|=components.pending();Set<String> nativePlans=new HashSet<>();
            for(var plan:found.plans())nativePlans.add(plan.key());
            for(var component:components.components())if(!nativePlans.contains(component.key())) {
                var pieces=localPieces(chunk,RoofComponentMesh.pieces(component,components.components(),RoofGeometry::canonicalVertices));
                if(!allReady(pieces)) {if(!pieces.isEmpty())pending(recipe);continue;}
                for(var piece:pieces)emit(recipe,chunk,level,piece);
            }
            var dormers=RoofDormerPlan.find(grid,found.plans());
            missing|=dormers.pending();
            for(var dormer:dormers.dormers()) {
                var pieces=localPieces(chunk,RoofDormerMesh.pieces(dormer,RoofGeometry::canonicalVertices));
                if(!allReady(pieces)) { if(!pieces.isEmpty())pending(recipe);continue; }
                for(var piece:pieces)emit(recipe,chunk,level,piece);
            }
            if(level>1 && localCap(grid,chunk,level)) {
                var below=RoofCompletionPlan.find(grid,level-1,x-HALO,y-HALO,x+7+HALO,y+7+HALO);
                missing|=below.pending();
                for(var plan:below.plans()) {
                    var pieces=RoofCompletionMesh.pieces(plan,RoofGeometry::canonicalVertices);
                    if(allReady(pieces))removeCaps(recipe,chunk,level,plan,grid);
                    else if(!pieces.isEmpty())pending(recipe);
                }
                var more=RoofComponentPlan.find(grid,RoofNativeVertices::vertices,level-1,x-HALO,y-HALO,x+7+HALO,y+7+HALO);
                missing|=more.pending();Set<String> oldBelow=new HashSet<>();for(var plan:below.plans())oldBelow.add(plan.key());
                for(var component:more.components())if(component.tiers==3&&!oldBelow.contains(component.key())) {
                    var pieces=RoofComponentMesh.pieces(component,more.components(),RoofGeometry::canonicalVertices);
                    if(allReady(pieces))removeCaps(recipe,chunk,level,component.legacy(),grid);
                    else if(!pieces.isEmpty())pending(recipe);
                }
            }
            retry(recipe,chunk,level,missing);
            RoofDependencies.observe(grid.cell,chunk,level,grid.missingChunks);
        }catch(Throwable failure) {
            if(!warned) { warned=true;System.out.println("[ViewpointInteriors] Roof completion unavailable: "+failure); }
        }
    }
    private RoofCompletion() {}
}
