package local.vpinteriors;

import java.util.*;

/** Evidence-driven completion of the hidden side of the native seven-tile gable. */
public final class RoofCompletionPlan {
    public static final int Y_SLOPE=0, X_SLOPE=1, MAX_SPAN=32;
    public interface Grid {
        Cell at(int x,int y,int z);
        default void missing(int x,int y,int z) {}
    }
    public record Tile(String name,int roof,int gable,Object tag) {}
    public record Cell(boolean loaded,boolean floor,boolean wallN,boolean wallW,List<Tile> tiles) {
        public static final Cell UNKNOWN=new Cell(false,false,false,false,List.of());
        public static final Cell AIR=new Cell(true,false,false,false,List.of());
    }
    public static final class Plan {
        public final int axis,level,start,end,low,far;
        public final float ridge;
        private final Grid grid;
        Plan(Grid grid,int axis,int level,int start,int end,int low,int far) {
            this.grid=grid;this.axis=axis;this.level=level;this.start=start;this.end=end;
            this.low=low;this.far=far;
            // Coordinates denote squares; the west/north wall lies on its
            // square's near edge, while the south/east wall is one square later.
            ridge=axis==Y_SLOPE?(far+low+1)*.5f:(low+far+1)*.5f;
        }
        public int index(int rank) { return axis==Y_SLOPE?rank:5-rank; }
        public int along(int rank) { return low-rank; }
        public int x(int cross,int along) { return axis==Y_SLOPE?cross:along; }
        public int y(int cross,int along) { return axis==Y_SLOPE?along:cross; }
        public Tile source(int cross,int rank) {
            int wanted=index(rank),a=along(rank);
            Tile result=roof(grid.at(x(cross,a),y(cross,a),level),wanted);
            if(result!=null)return result;
            // A dormer can replace a source tile. Only the absent opposite
            // slope is inferred here; borrow the same run's native roof texture.
            for(int d=1;d<end-start;d++)for(int c:new int[]{cross-d,cross+d}) {
                if(c<start||c>=end)continue;
                result=roof(grid.at(x(c,a),y(c,a),level),wanted);
                if(result!=null)return result;
            }
            return null;
        }
        public boolean containsCap(int x,int y,int z,int roofIndex) {
            if(z!=level+1)return false;
            int c=axis==Y_SLOPE?x:y,a=axis==Y_SLOPE?y:x;
            // Native cap sprites are shifted one square along the ridge.
            // Remove only these documented positions after a plan is validated.
            return c>=start && c<=end && a==along(2)
                && roofIndex==(axis==Y_SLOPE?15:14);
        }
        public String key() { return axis+":"+level+":"+start+":"+end+":"+low+":"+far; }
    }
    public record Result(List<Plan> plans,boolean pending) {}
    private static final class Probe {
        final Grid grid;boolean pending;
        Probe(Grid grid) { this.grid=grid; }
        Cell at(int axis,int c,int a,int z) {
            Cell s=grid.at(axis==Y_SLOPE?c:a,axis==Y_SLOPE?a:c,z);
            if(s==null)s=Cell.UNKNOWN;
            if(!s.loaded) {
                pending=true;
                grid.missing(axis==Y_SLOPE?c:a,axis==Y_SLOPE?a:c,z);
            }
            return s;
        }
    }
    public static Tile roof(Cell s,int index) {
        if(s==null)return null;
        for(Tile t:s.tiles)if(t.roof==index)return t;
        return null;
    }
    private static boolean gable(Cell s,int index,String family) {
        for(Tile t:s.tiles)if(t.gable==index && (family==null||family.equals(family(t.name))))return true;
        return false;
    }
    private static String family(String name) {
        int i=name.lastIndexOf('_');return i<0?name:name.substring(0,i);
    }
    private static String gableFamily(Cell s,int index) {
        for(Tile t:s.tiles)if(t.gable==index)return family(t.name);
        return null;
    }
    private static boolean profile(Probe p,int axis,int cross,int low,int far,int level) {
        int[] source=axis==Y_SLOPE?new int[]{0,1,2}:new int[]{5,4,3};
        int[] opposite=axis==Y_SLOPE?new int[]{8,9,10}:new int[]{13,12,11};
        int toward=-1;
        String f=gableFamily(p.at(axis,cross,low,level),source[0]);
        if(f==null)return false;
        for(int i=0;i<3;i++) {
            if(!gable(p.at(axis,cross,low+toward*i,level),source[i],f))return false;
            if(!gable(p.at(axis,cross,far-toward*i,level),opposite[i],f))return false;
        }
        return true;
    }
    private static Plan inspect(Grid grid,int axis,int level,int anchorCross,int high,Probe p) {
        int highIndex=axis==Y_SLOPE?2:3,toward=-1;
        int low=high-toward*2,start=anchorCross,end=anchorCross+1;
        while(start>anchorCross-MAX_SPAN && roof(p.at(axis,start-1,high,level),highIndex)!=null)start--;
        while(end<anchorCross+MAX_SPAN && roof(p.at(axis,end,high,level),highIndex)!=null)end++;
        if(end-start<2 || end-start>=MAX_SPAN)return null;
        // These profiles correspond to complete six- or seven-tile gables.
        // Other roof widths/pitches/intersections deliberately remain native.
        for(int distance:new int[]{6,5}) {
            int far=low+toward*distance;
            if(!profile(p,axis,start,low,far,level) && !profile(p,axis,end,low,far,level))continue;
            Plan candidate=new Plan(grid,axis,level,start,end,low,far);
            boolean valid=true,hasFullSource=false;
            int min=Math.min(low,far),max=Math.max(low,far);
            for(int c=start;c<end && valid;c++) {
                boolean complete=true;
                for(int rank=0;rank<3;rank++) {
                    if(roof(p.at(axis,c,candidate.along(rank),level),candidate.index(rank))==null)complete=false;
                    if(candidate.source(c,rank)==null)valid=false;
                }
                hasFullSource|=complete;
                // A wall at both eaves bounds the footprint independently of
                // the terrain's solidfloor flag (grass itself is solidfloor).
                int nearWall=far;
                int oppositeWall=low+1;
                Cell wallA=p.at(axis,c,nearWall,level-1),wallB=p.at(axis,c,oppositeWall,level-1);
                if(!(axis==Y_SLOPE?wallA.wallN&&wallB.wallN:wallA.wallW&&wallB.wallW))valid=false;
                for(int a=min;a<=max && valid;a++) {
                    Cell here=p.at(axis,c,a,level),below=p.at(axis,c,a,level-1);
                    if(!here.loaded||!below.loaded) { valid=false;break; }
                    // Permit a stairwell cutout when the floor one storey
                    // lower still proves the same bounded house footprint.
                    if(!below.floor && (level<2 || !p.at(axis,c,a,level-2).floor))valid=false;
                    float center=a+.5f;
                    boolean hiddenSide=center<candidate.ridge;
                    if(hiddenSide)for(Tile tile:here.tiles)
                        if(tile.roof!=-1) { valid=false;break; }
                }
            }
            if(valid&&hasFullSource)return candidate;
        }
        return null;
    }
    /** Bounded query used once while a chunk's Recipe is gathered, never per frame. */
    public static Result find(Grid grid,int level,int minX,int minY,int maxX,int maxY) {
        if(level<1)return new Result(List.of(),false);
        LinkedHashMap<String,Plan> plans=new LinkedHashMap<>();boolean pending=false;
        Set<String> inspected=new HashSet<>();
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++) {
            Cell sq=grid.at(x,y,level);if(sq==null||!sq.loaded)continue;
            for(Tile tile:sq.tiles) {
                int axis=tile.roof==2?Y_SLOPE:tile.roof==3?X_SLOPE:-1;
                if(axis<0)continue;
                int cross=axis==Y_SLOPE?x:y,high=axis==Y_SLOPE?y:x;
                String input=axis+":"+cross+":"+high;
                if(!inspected.add(input))continue;
                Probe p=new Probe(grid);Plan plan=inspect(grid,axis,level,cross,high,p);
                pending|=p.pending;
                if(plan!=null) {
                    plans.put(plan.key(),plan);
                    for(int c=plan.start;c<plan.end;c++)inspected.add(axis+":"+c+":"+high);
                }
            }
        }
        return new Result(List.copyOf(plans.values()),pending);
    }
    private RoofCompletionPlan() {}
}
