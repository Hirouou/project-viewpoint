package local.vpads;

import java.util.*;
import org.joml.Vector3f;
import zombie.characters.*;
import zombie.inventory.types.HandWeapon;

/** Continuous, camera-relative motion for the real skeleton, sampled once per animation update. */
public final class NativeAimMotion {
    record Sample(float sideways,float vertical,float back,float yaw,float pitch){}
    record Target(Vector3f eye,Vector3f direction,Vector3f up){}
    private record Shot(double time,float strength){}
    static final class Hold {
        double time,walkPhase;
        float walking;
        final ArrayList<Shot> shots=new ArrayList<>();
        void shot(float strength){shots.add(new Shot(time,strength));if(shots.size()>12)shots.remove(0);}
        Sample advance(double dt,boolean moving){
            dt=Math.max(0,Math.min(.25,dt));time+=dt;
            walking+=(float)((moving?1:0)-walking)*(float)(1-Math.exp(-dt/.20));
            walkPhase+=dt*2*Math.PI*1.65;
            double breathing=time*2*Math.PI/4.2;
            float recoil=0;
            for(Iterator<Shot> it=shots.iterator();it.hasNext();){
                Shot s=it.next();double age=time-s.time;
                if(age>.65){it.remove();continue;}
                // A fast continuous rise followed by a damped return, not a frame jump.
                double x=age/.040;recoil+=(float)(s.strength*x*Math.exp(1-x));
            }
            recoil=Math.min(2,recoil);
            return new Sample((float)Math.sin(walkPhase)*.004f*walking,
                (float)Math.sin(breathing)*.0008f+(float)Math.sin(walkPhase*2)*.0025f*walking,
                .014f*recoil,
                (float)Math.sin(breathing*.5)*.00025f+(float)Math.sin(walkPhase)*.0015f*walking,
                (float)Math.sin(breathing)*.0007f+(float)Math.cos(walkPhase*2)*.0025f*walking+.055f*recoil);
        }
    }
    private static Object owner,weapon;
    private static long lastTime;
    private static Hold hold=new Hold();
    private static Sample sample=new Sample(0,0,0,0,0);
    private NativeAimMotion(){}
    static synchronized void reset(){owner=null;weapon=null;lastTime=0;hold=new Hold();sample=new Sample(0,0,0,0,0);}
    private static void session(IsoPlayer player,Object item){
        if(owner!=player||weapon!=item){reset();owner=player;weapon=item;}
    }
    static synchronized void update(IsoPlayer player){
        session(player,player.getPrimaryHandItem());long now=System.nanoTime();
        double dt=lastTime==0?0:(now-lastTime)*1e-9;lastTime=now;
        sample=hold.advance(dt,player.isPlayerMoving());
    }
    /** Called after the engine's real firearm firing method, including unlimited-ammo shots. */
    public static synchronized void fired(HandWeapon gun,IsoGameCharacter character){
        if(!(character instanceof IsoPlayer player)||!player.isLocalPlayer()||gun==null
            ||!gun.isAimedFirearm()||!player.isAiming()||!NativeBodyAim.wants(player)
            ||!ViewpointADSTest.isFirstPerson()||!ViewpointADSTest.isViewpointActive())return;
        session(player,gun);hold.shot(gun.isTwoHandWeapon()?(gun.getProjectileCount()>1?1.3f:.75f):1);
    }
    static synchronized Target target(Vector3f eye,Vector3f direction,Vector3f up){return target(eye,direction,up,sample);}
    static Target target(Vector3f eye,Vector3f direction,Vector3f up,Sample s){
        Vector3f right=NativeSightAlignment.cameraRight(direction,up);
        Vector3f origin=new Vector3f(eye).fma(s.sideways,right).fma(s.vertical,up).fma(-s.back,direction);
        Vector3f forward=new Vector3f(direction).fma(s.yaw,right).fma(s.pitch,up).normalize();
        Vector3f vertical=new Vector3f(up).fma(-up.dot(forward),forward).normalize();
        return new Target(origin,forward,vertical);
    }
}
