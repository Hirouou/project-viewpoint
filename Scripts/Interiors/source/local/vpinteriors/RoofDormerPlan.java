package local.vpinteriors;

import java.util.*;
import static local.vpinteriors.RoofCompletionPlan.*;

/** Only the documented five-tile projecting17 gable on a native Y slope. */
public final class RoofDormerPlan {
    public record Dormer(int level,int lowX,int front,int mainLow,Tile source,float ridge) {}
    public record Result(List<Dormer> dormers,boolean pending) {}
    private static Cell at(Grid grid,int x,int y,int z) {
        Cell cell=grid.at(x,y,z);if(cell==null)cell=Cell.UNKNOWN;
        if(!cell.loaded())grid.missing(x,y,z);
        return cell;
    }
    private static String family(String name) {return name.substring(0,name.lastIndexOf('_'));}
    private static Tile gable(Cell cell,int index,String family) {
        for(Tile tile:cell.tiles())if(tile.gable()==index && (family==null||family.equals(family(tile.name()))))return tile;
        return null;
    }
    public static Result find(Grid grid,List<Plan> mains) {
        List<Dormer> result=new ArrayList<>();boolean pending=false;
        for(Plan main:mains) {
            if(main.axis!=Y_SLOPE)continue;
            int front=main.low+1;
            for(int center=main.start+2;center+2<main.end;center++) {
                Cell peak=at(grid,center,front,main.level);
                if(!peak.loaded()) {pending=true;continue;}
                Tile middle=gable(peak,17,null);if(middle==null)continue;
                String f=family(middle.name());boolean valid=true;
                int[] profile={13,12,17,4,5};
                for(int i=0;i<5;i++) {
                    int x=center-2+i;Cell face=at(grid,x,front,main.level),wall=at(grid,x,front,main.level-1);
                    if(!face.loaded()||!wall.loaded()) {pending=true;valid=false;break;}
                    if(gable(face,profile[i],f)==null || !wall.wallN()) {valid=false;break;}
                    for(int y=main.low-2;y<=main.low;y++) {
                        Cell here=at(grid,x,y,main.level),below=at(grid,x,y,main.level-1);
                        if(!here.loaded()||!below.loaded()) {pending=true;valid=false;break;}
                        if(!below.floor()) {valid=false;break;}
                        // Existing Y water is expected underneath this projecting
                        // roof. An existing opposite X/corner surface wins.
                        if(i<2)for(Tile tile:here.tiles())if(tile.roof()!=-1 && tile.roof()!=0 && tile.roof()!=1 && tile.roof()!=2) {valid=false;break;}
                    }
                }
                Tile source=roof(at(grid,center+1,main.low,main.level),4);
                boolean lowCorner=roof(at(grid,center+2,main.low,main.level),11)!=null;
                boolean midCorner=roof(at(grid,center+1,main.low-1,main.level),12)!=null;
                if(valid && source!=null && lowCorner && midCorner)
                    result.add(new Dormer(main.level,center+2,front,main.low,source,center+.5f));
            }
        }
        return new Result(List.copyOf(result),pending);
    }
    private RoofDormerPlan() {}
}
