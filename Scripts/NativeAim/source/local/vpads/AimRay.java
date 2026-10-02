package local.vpads;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import org.joml.Quaternionf;
import zombie.characters.IsoGameCharacter;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.Bullet;
import zombie.iso.Vector3;
import zombie.network.GameServer;

/** Bridges the visible first-person camera to PZ's existing ballistic controller.
 * No hit, spread, ammunition, damage or collision calculations are replaced. */
public final class AimRay {
    public static final float FLOOR_HEIGHT = 2.4494896f;
    private static final long MAX_AGE_NS = 750_000_000L;
    private static volatile Camera camera;
    private static Field yawField, pitchField;
    private static Field camXField, camYField, camZField, eyeXField, eyeYField, eyeZField;
    private static final IdentityHashMap<BallisticsController,Integer> changed = new IdentityHashMap<>();
    private static boolean reported, logged;

    private record Camera(Object owner, float headX, float headY, float headZ, long time) {}
    public record Ray(float x, float y, float z, float dx, float dy, float dz,
                      float yaw, float pitch) {
        public void position(Vector3 out) { out.set(x,y,z); }
        /** PZ floor coordinates; normalized only after converting to physical space. */
        public void direction(Vector3 out) { out.set(dx,dy,dz); }
        public void at(float distance, Vector3 out) { out.set(x+dx*distance,y+dy*distance,z+dz*distance); }
        public Quaternionf cameraRotation() {
            // The native reticle plane uses +Z toward the camera, +Y up.
            return new Quaternionf().rotationYXZ(-yaw-(float)Math.PI/2f,pitch,0f);
        }
    }
    private AimRay() {}

    /** Pure coordinate conversion, also used by the calibration verification. */
    public static Ray ray(float headX,float headY,float headZ,float yaw,float pitch) {
        float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw);
        float cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);
        // Match SceneDrawer's 0.12 horizontal camera shift exactly.
        return new Ray(headX+cy*.12f,headY+sy*.12f,headZ,cy*cp,sy*cp,sp/FLOOR_HEIGHT,yaw,pitch);
    }

    /** Render-thread snapshot. Never calls Controls.eye, which mutates its smoothing. */
    public static void capture(Object frame) throws ReflectiveOperationException {
        Object owner=NativeBodyAim.owner();
        if(!(owner instanceof IsoGameCharacter player)||!NativeBodyAim.wants(player))return;
        if(camXField==null) {
            Class<?> type=frame.getClass();
            camXField=type.getField("camX");camYField=type.getField("camY");camZField=type.getField("camZ");
            eyeXField=type.getField("eyeX");eyeYField=type.getField("eyeY");eyeZField=type.getField("eyeZ");
            Class<?> look=Class.forName("viewpoint.input.Look");
            yawField=look.getField("yaw");pitchField=look.getField("pitch");
        }
        float x=camXField.getFloat(frame)-eyeXField.getFloat(frame)-player.getX();
        float y=camYField.getFloat(frame)-eyeZField.getFloat(frame)-player.getY();
        float z=camZField.getFloat(frame)+eyeYField.getFloat(frame)/FLOOR_HEIGHT-player.getZ();
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z)||Math.abs(x)>2f||Math.abs(y)>2f||Math.abs(z)>2f)return;
        camera=new Camera(player,x,y,z,System.nanoTime());
    }

    public static Ray current(IsoGameCharacter player) {
        Camera c=camera;
        if(player==null||c==null||c.owner!=player||System.nanoTime()-c.time>MAX_AGE_NS
                ||NativeActionGate.blocked(player)!=null||!NativeBodyAim.wants(player)||!NativeBodyAim.isAiming()||!NativeBodyAim.active()||!ViewpointADSTest.isFirstPerson())return null;
        try {
            float yaw=yawField.getFloat(null),pitch=pitchField.getFloat(null);
            if(!Float.isFinite(yaw)||!Float.isFinite(pitch))return null;
            return ray(player.getX()+c.headX,player.getY()+c.headY,player.getZ()+c.headZ,yaw,pitch);
        } catch(ReflectiveOperationException e) { fail(e);return null; }
    }

    public static boolean muzzle(IsoGameCharacter player,Vector3 position,Vector3 direction) {
        Ray r=current(player);if(r==null)return false;
        r.position(position);r.direction(direction);return true;
    }

    public static boolean aim(IsoGameCharacter player,BallisticsController.AimingVectorParameters out) {
        Ray r=current(player);if(r==null)return false;
        r.position(out.muzzlePosition);r.direction(out.muzzleDirection);r.at(8f,out.targetPosition);
        r.direction(out.desiredForward);out.desiredForward.normalize();
        out.desiredForward2f.set((float)Math.cos(r.yaw),(float)Math.sin(r.yaw));
        // PZ's vertical animation input is a physical angle, not floor-coordinate slope.
        out.desiredForwardPitchRads=r.pitch;
        return true;
    }

    /** Runs after the vanilla update so its ground-plane reticle cannot overwrite FPS aim. */
    public static void update(BallisticsController controller,IsoGameCharacter player,boolean initialized,
                              Vector3 converted,Vector3 target) {
        if(GameServer.server)return;
        try {
            Ray r=current(player);
            if(r==null||!initialized) { restore(controller);return; }
            r.position(controller.getMuzzlePosition());r.direction(controller.getMuzzleDirection());
            converted.set(r.dx,r.dz*FLOOR_HEIGHT,r.dy);converted.normalize();
            r.at(8f,controller.getIsoAimingPosition());
            // Keep target position on the ray. Native target queries still choose actual hit parts.
            float range=8f;
            if(player.getAttackingWeapon()!=null)
                range=player.getAttackingWeapon().getMaxRange()*player.getAttackingWeapon().getRangeMod(player);
            if(!Float.isFinite(range)||range<=0)range=8f;
            r.at(range,target);
            int id=controller.getID();
            Bullet.updateBallistics(id,r.x,r.z*FLOOR_HEIGHT,r.y);
            Bullet.updateBallisticsMuzzleAimDirection(id,converted.x,converted.y,converted.z);
            Vector3 aim=controller.getIsoAimingPosition();
            Bullet.updateBallisticsAimReticlePosition(id,aim.x,aim.z*FLOOR_HEIGHT,aim.y);
            Quaternionf q=r.cameraRotation();
            Bullet.updateBallisticsAimReticleQuaternion(id,q.x,q.y,q.z,q.w);
            changed.put(controller,id);
            if(!logged) { System.out.println("[ViewpointADS] Camera ray linked to native ballistics (origin, direction, reticle and pitch)");logged=true; }
        } catch(Throwable e) { fail(e); }
    }

    private static void restore(BallisticsController controller) {
        Integer id=changed.remove(controller);if(id==null)return;
        Quaternionf q=new Quaternionf().rotationYXZ((float)Math.PI/4f,-(float)Math.PI/6f,0f);
        Bullet.updateBallisticsAimReticleQuaternion(id,q.x,q.y,q.z,q.w);
    }
    private static void fail(Throwable e) {
        if(!reported) { reported=true;System.out.println("[ViewpointADS] Camera ballistic bridge unavailable: "+e); }
    }
}
