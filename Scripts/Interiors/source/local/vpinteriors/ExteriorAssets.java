package local.vpinteriors;

import java.lang.reflect.Field;
import java.util.List;
import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import viewpoint.world.Recipe;

/** Exterior wall models must not inherit the visibility mask of the room behind them. */
public final class ExteriorAssets {
    public static final int NO_SQUARE=64;
    private static Field models,square;
    private static synchronized void access() throws ReflectiveOperationException {
        if(models!=null)return;
        Field m=Recipe.class.getDeclaredField("models");m.setAccessible(true);
        Field s=Class.forName("viewpoint.world.Recipe$Op").getDeclaredField("square");s.setAccessible(true);
        square=s;models=m;
    }
    public static int count(Recipe recipe) {
        if(recipe==null)return 0;
        try{access();List<?> entries=(List<?>)models.get(recipe);return entries==null?0:entries.size();}
        catch(ReflectiveOperationException e){return 0;}
    }
    private static boolean any(PropertyContainer p,IsoFlagType... flags) {
        for(IsoFlagType flag:flags)if(p.has(flag))return true;
        return false;
    }
    private static boolean outsideAcross(IsoGridSquare q,IsoDirections direction) {
        if(q.isOutside())return true;
        IsoGridSquare neighbor=q.getAdjacentSquare(direction);
        // A missing streamed neighbor is uncertain, not proof of an exterior edge.
        return neighbor!=null&&neighbor.isOutside();
    }
    public static boolean exterior(IsoObject object,IsoSprite sprite) {
        if(object==null||sprite==null||object.getSquare()==null)return false;
        PropertyContainer p=sprite.getProperties();if(p==null)return false;
        IsoGridSquare q=object.getSquare();
        boolean north=any(p,IsoFlagType.WindowN,IsoFlagType.windowN,IsoFlagType.doorN,
            IsoFlagType.DoorWallN,IsoFlagType.WallN,IsoFlagType.attachedN,IsoFlagType.WallNW,IsoFlagType.attachedNW);
        boolean west=any(p,IsoFlagType.WindowW,IsoFlagType.windowW,IsoFlagType.doorW,
            IsoFlagType.DoorWallW,IsoFlagType.WallW,IsoFlagType.attachedW,IsoFlagType.WallNW,IsoFlagType.attachedNW);
        boolean south=any(p,IsoFlagType.attachedS,IsoFlagType.attachedSE,IsoFlagType.WallSE);
        boolean east=any(p,IsoFlagType.attachedE,IsoFlagType.attachedSE,IsoFlagType.WallSE);
        return north&&outsideAcross(q,IsoDirections.N)||west&&outsideAcross(q,IsoDirections.W)
            ||south&&outsideAcross(q,IsoDirections.S)||east&&outsideAcross(q,IsoDirections.E);
    }
    public static void preserve(Recipe recipe,int first,IsoObject object,IsoSprite sprite,int localSquare) {
        if(recipe==null||!exterior(object,sprite))return;
        try{
            access();List<?> entries=(List<?>)models.get(recipe);if(entries==null)return;
            for(int i=Math.max(0,first);i<entries.size();i++) {
                Object op=entries.get(i);
                if(square.getInt(op)==localSquare)square.setInt(op,NO_SQUARE);
            }
        }catch(ReflectiveOperationException e){System.out.println("[ViewpointInteriors] Exterior wall model visibility unavailable: "+e);}
    }
    private ExteriorAssets(){}
}
