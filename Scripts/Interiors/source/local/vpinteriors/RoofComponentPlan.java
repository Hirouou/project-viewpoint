package local.vpinteriors;

import java.util.*;
import static local.vpinteriors.RoofCompletionPlan.*;

/** Native two/three-tier gable components, including proved perpendicular joins. */
public final class RoofComponentPlan {
    public interface Geometry { float[] vertices(Tile tile); }
    public static final class Component {
        public final int axis,level,start,end,low,far,tiers;public final float ridge;
        final Grid grid;final Geometry geometry;
        Component(Grid grid,Geometry geometry,int axis,int level,int start,int end,int low,int far,int tiers) {
            this.grid=grid;this.geometry=geometry;this.axis=axis;this.level=level;this.start=start;this.end=end;this.low=low;this.far=far;this.tiers=tiers;
            ridge=(low+far+1)*.5f;
        }
        public int index(int rank) {return axis==Y_SLOPE?rank:5-rank;}
        public int along(int rank) {return low-rank;}
        public int x(int c,int a) {return axis==Y_SLOPE?c:a;}
        public int y(int c,int a) {return axis==Y_SLOPE?a:c;}
        public Tile source(int c,int rank) {
            int a=along(rank),wanted=index(rank);
            for(int d=0;d<end-start;d++)for(int cross:new int[]{c-d,c+d})if(cross>=start&&cross<end) {
                Tile t=roof(grid.at(x(cross,a),y(cross,a),level),wanted);if(t!=null)return t;
            }
            return null;
        }
        public String key() {return axis+":"+level+":"+start+":"+end+":"+low+":"+far;}
        public Plan legacy() {return new Plan(grid,axis,level,start,end,low,far);}
    }
    public record Result(List<Component> components,boolean pending) {}
    private static final class Query {
        final Grid grid;boolean pending;
        Query(Grid grid) {this.grid=grid;}
        Cell at(int axis,int c,int a,int level) {
            int x=axis==Y_SLOPE?c:a,y=axis==Y_SLOPE?a:c;Cell cell=grid.at(x,y,level);
            if(cell==null)cell=Cell.UNKNOWN;
            if(!cell.loaded()) {pending=true;grid.missing(x,y,level);}return cell;
        }
    }
    private static String family(String name) {return name.substring(0,name.lastIndexOf('_'));}
    private static boolean gable(Cell cell,int index,String family) {
        for(Tile t:cell.tiles())if(t.gable()==index&&(family==null||family.equals(family(t.name()))))return true;
        return false;
    }
    private static boolean profile(Query q,int axis,int c,int low,int far,int level,int tiers) {
        int first=axis==Y_SLOPE?0:5;String sheet=null;
        for(Tile t:q.at(axis,c,low,level).tiles())if(t.gable()==first) {sheet=family(t.name());break;}
        if(sheet==null)return false;
        for(int rank=0;rank<tiers;rank++) {
            if(!gable(q.at(axis,c,low-rank,level),axis==Y_SLOPE?rank:5-rank,sheet))return false;
            if(!gable(q.at(axis,c,far+rank,level),axis==Y_SLOPE?8+rank:13-rank,sheet))return false;
        }
        if(tiers==2 && low-far==4 && !gable(q.at(axis,c,far+2,level),axis==Y_SLOPE?16:17,sheet))return false;
        return true;
    }
    public static boolean perpendicular(float[] data,int axis) {
        if(data==null)return false;boolean found=false;
        for(int i=0;i<data.length;i+=8)if(data[i+6]>.5f) {
            if(Math.abs(data[i+(axis==Y_SLOPE?7:5)])>.05f)return false;
            if(Math.abs(data[i+(axis==Y_SLOPE?5:7)])>.05f)found=true;
        }return found;
    }
    private static boolean bounded(Query q,Geometry geometry,int axis,int cross,int at,int level,int start,int end,int low,int far,int tiers) {
        Cell wall=q.at(axis,cross,at,level-1);
        if(axis==Y_SLOPE?wall.wallN():wall.wallW())return true;
        // A native corner gable square can carry its cross-wall only. The
        // adjacent interior square must still prove the physical eave wall.
        if(cross==start || cross==end-1) {
            int edge=cross==start?start:end;
            int inside=cross==start?cross+1:cross-1;
            Cell next=q.at(axis,inside,at,level-1);
            if(profile(q,axis,edge,low,far,level,tiers) && (axis==Y_SLOPE?next.wallN():next.wallW()))return true;
        }
        // Internal L joins have an upper storey continuing past this component's
        // eave. A nearby *native perpendicular water* proves the intersection.
        // Ground grass alone cannot authorize an internal join.
        if(level<2 || !wall.floor())return false;
        for(int dc=-3;dc<=3;dc++)for(int da=-3;da<=3;da++)
            for(Tile t:q.at(axis,cross+dc,at+da,level).tiles())if(t.roof()!=-1&&perpendicular(geometry.vertices(t),axis))return true;
        return false;
    }
    private static Component inspect(Grid grid,Geometry geometry,Query q,int axis,int level,int c,int high,int tiers) {
        int highIndex=axis==Y_SLOPE?tiers-1:6-tiers,low=high+tiers-1,start=c,end=c+1;
        while(start>c-MAX_SPAN&&roof(q.at(axis,start-1,high,level),highIndex)!=null)start--;
        while(end<c+MAX_SPAN&&roof(q.at(axis,end,high,level),highIndex)!=null)end++;
        if(end-start<2||end-start>=MAX_SPAN)return null;
        for(int distance:new int[]{tiers*2,tiers*2-1}) {
            int far=low-distance;
            if(!profile(q,axis,start,low,far,level,tiers)&&!profile(q,axis,end,low,far,level,tiers))continue;
            Component p=new Component(grid,geometry,axis,level,start,end,low,far,tiers);boolean valid=true,full=false;
            for(int cross=start;cross<end&&valid;cross++) {
                boolean column=true;
                for(int rank=0;rank<tiers;rank++) {
                    column&=roof(q.at(axis,cross,p.along(rank),level),p.index(rank))!=null;
                    if(p.source(cross,rank)==null)valid=false;
                }full|=column;
                if(!bounded(q,geometry,axis,cross,far,level,start,end,low,far,tiers)
                    ||!bounded(q,geometry,axis,cross,low+1,level,start,end,low,far,tiers))valid=false;
                for(int along=far;along<=low&&valid;along++) {
                    Cell here=q.at(axis,cross,along,level),below=q.at(axis,cross,along,level-1);
                    if(!here.loaded()||!below.loaded()) {valid=false;break;}
                    if(!below.floor()&&(level<2||!q.at(axis,cross,along,level-2).floor())) {valid=false;break;}
                    if(along+.5f<p.ridge)for(Tile tile:here.tiles())if(tile.roof()!=-1) {
                        // An existing same-axis roof wins; only a proved
                        // perpendicular face can intersect the inferred water.
                        if(!perpendicular(geometry.vertices(tile),axis)) {valid=false;break;}
                    }
                }
            }
            if(valid&&full)return p;
        }
        return null;
    }
    public static Result find(Grid grid,Geometry geometry,int level,int minX,int minY,int maxX,int maxY) {
        Map<String,Component> result=new LinkedHashMap<>();Set<String> tried=new HashSet<>();boolean pending=false;
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++) {
            Cell cell=grid.at(x,y,level);if(cell==null||!cell.loaded())continue;
            for(Tile tile:cell.tiles()) {
                int axis=tile.roof()==2||tile.roof()==1?Y_SLOPE:tile.roof()==3||tile.roof()==4?X_SLOPE:-1;
                if(axis<0)continue;int tiers=tile.roof()==1||tile.roof()==4?2:3;
                int cross=axis==Y_SLOPE?x:y,high=axis==Y_SLOPE?y:x;String k=axis+":"+tiers+":"+cross+":"+high;
                if(!tried.add(k))continue;Query q=new Query(grid);Component component=inspect(grid,geometry,q,axis,level,cross,high,tiers);pending|=q.pending;
                if(component!=null) {
                    result.put(component.key(),component);
                    for(int c=component.start;c<component.end;c++)tried.add(axis+":"+tiers+":"+c+":"+high);
                }
            }
        }return new Result(List.copyOf(result.values()),pending);
    }
    private RoofComponentPlan() {}
}
