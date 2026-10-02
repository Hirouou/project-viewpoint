package local.vpads;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.joml.*;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.model.*;

/** Adjusts the existing local body draw. Never creates a mesh, texture, draw or animation track. */
public final class NativeBodyAim {
    private static final Matrix4f BASIS=new Matrix4f().scaling(1,1,-1).rotateZ(-(float)java.lang.Math.PI/2);
    private static final Matrix4f INVERSE_BASIS=new Matrix4f(BASIS).invert();
    static final Map<String,AimCalibration> calibrations=new LinkedHashMap<>();
    private record State(Object owner,String item,boolean enabled,boolean aiming,long time){}
    private static volatile State state=new State(null,"",false,false,0);
    private static volatile float blend;
    private static volatile boolean ready;
    private static volatile int nativeParts;
    private static volatile String reason="inactive",sample="{\"error\":\"No native frame yet\"}";
    private static Object baselineOwner;private static String baselineItem="";
    private static Map<String,Matrix4f> baseline;
    private static long renderTime;
    private static Field frameField,instancesField,meshesField,valuesField,paletteField,shadowField,rowsField,bonesField;
    private static Method paletteMethod;
    private static int stride;
    private static final ThreadLocal<Object> currentFrame=new ThreadLocal<>();
    private static final ThreadLocal<Transaction> transaction=new ThreadLocal<>();
    private static boolean reported,failed;

    private NativeBodyAim(){}
    public static void load(Path dir)throws Exception {
        calibrations.clear();NativeSightAlignment.load(dir.resolve("iron-sights.txt"));
        for(String line:Files.readAllLines(dir.resolve("calibrations.txt"))){
            if(line.isBlank())continue;String[] a=line.split("\\|");
            if(a.length!=2||calibrations.put(a[0],AimCalibration.read(dir.resolve(a[1])))!=null)throw new IllegalArgumentException("Calibration manifest");
        }
        System.out.println("[ViewpointADS] Native body aim 0.4.8: "+calibrations.size()+" joint calibrations; no replacement arms or weapons.");
    }
    public static void selectState(Object owner,String item,boolean enabled,boolean aiming){
        state=new State(owner,item,enabled,aiming,System.nanoTime());
        if(!enabled){NativeSightAlignment.reset();NativeSkeletonAim.active=false;blend=0;ready=false;nativeParts=0;reason="inactive";}
        else if(!aiming){NativeSightAlignment.reset();NativeSkeletonAim.active=false;ready=false;reason="lowered";}
        else if(owner instanceof IsoGameCharacter player){
            String blocked=NativeActionGate.blocked(player);
            if(blocked!=null){NativeSightAlignment.reset();blend=0;ready=false;reason=blocked;}
        }
    }
    public static boolean hasCalibration(String item){return calibrations.containsKey(item);}
    public static Object owner(){return state.owner;}
    public static boolean wants(Object owner){return wants()&&state.owner==owner;}
    public static boolean wants(){return !failed&&state.enabled&&System.nanoTime()-state.time<750_000_000L;}
    public static boolean isAiming(){return state.aiming&&NativeSkeletonAim.active;}
    public static boolean active(){return wants()&&isAiming();}
    public static float aimBlend(){return NativeSkeletonAim.blend();}
    public static int nativeParts(){return nativeParts;}
    public static String reason(){return NativeSkeletonAim.reason;}
    public static String cameraSample(){return "version=0.4.8; weapon="+state.item+"; handgunGrip="+NativeSightAlignment.handgunGrip+"; gripSource=native weapon-relative calibration; captureState="+NativeCaptureSync.sample+"; captureAttachments="+NativeCaptureSync.refreshed+"; closeNear="+(NativeCloseRendering.enabled()?NativeCloseRendering.NEAR:.05f)+"; shared native skeleton; head camera; spineDegrees="+java.lang.Math.toDegrees(NativeSkeletonAim.spinePitch)+"; sights="+NativeSightAlignment.status+"; sightDistance="+NativeSightAlignment.sightDistance+"; aimDegrees="+java.lang.Math.toDegrees(NativeSkeletonAim.totalPitch);}

    public static void beginFrame(Object drawer){
        try {
            if(frameField==null){frameField=drawer.getClass().getDeclaredField("frame");frameField.setAccessible(true);}
            Object frame=frameField.get(drawer);currentFrame.set(frame);
        }catch(ReflectiveOperationException e){fail(e);}
    }
    public static void endFrame(){
        Transaction tx=transaction.get();
        try{if(tx!=null)tx.restore();}catch(ReflectiveOperationException e){fail(e);}
        finally{transaction.remove();currentFrame.remove();}
    }

    /** Only called before the original ModelPass uploads its scene; original CPU snapshots are restored after drawing. */
    public static void prepare(Object scene,Matrix4f view){
        // Native skeleton and attachments are already posed together. No separate draw transform.
        NativeCloseRendering.prepare(scene);
        try {Object frame=currentFrame.get();if(frame!=null)AimRay.capture(frame);}
        catch(ReflectiveOperationException e){fail(e);}
    }
    private static void fail(Throwable e){if(!failed){failed=true;System.out.println("[ViewpointADS] Native adjustment disabled; original rendering retained: "+e);}}
    static boolean armBone(String name){return name.contains("UpperArm")||name.contains("Forearm")||name.contains("Hand")||name.contains("Finger")||name.equals("Bip01_Prop1")||name.equals("Bip01_Prop2");}
    static boolean owned(ModelInstance instance,Object owner){for(int i=0;i<16&&instance!=null;i++,instance=instance.parent)if(instance.character==owner)return true;return false;}
    static boolean held(ModelInstance instance,ModelInstance primary){if(primary==null)return false;for(int i=0;i<16&&instance!=null;i++,instance=instance.parent)if(instance==primary)return true;return false;}
    static Map<String,Matrix4f> copy(Map<String,Matrix4f> values){Map<String,Matrix4f> out=new LinkedHashMap<>();for(var e:values.entrySet())out.put(e.getKey(),new Matrix4f(e.getValue()));return out;}
    private static Field field(Class<?> type,String name)throws ReflectiveOperationException{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void init(Class<?> type)throws ReflectiveOperationException{
        if(instancesField!=null)return;
        instancesField=field(type,"instances");meshesField=field(type,"meshes");valuesField=field(type,"values");
        paletteField=field(type,"palette");shadowField=field(type,"shadowPalette");rowsField=field(type,"palettes");bonesField=field(type,"bones");
        stride=field(type,"VALUES").getInt(null);paletteMethod=type.getMethod("palette",float[].class,int.class);
    }
    static Matrix4f offset(SkinningData skin,int index){
        if(skin.boneOffset==null||index<0||index>=skin.boneOffset.size()||skin.boneOffset.get(index)==null)return null;
        org.lwjgl.util.vector.Matrix4f a=skin.boneOffset.get(index);
        return new Matrix4f(a.m00,a.m10,a.m20,a.m30,a.m01,a.m11,a.m21,a.m31,a.m02,a.m12,a.m22,a.m32,a.m03,a.m13,a.m23,a.m33);
    }
    static Matrix4f rowMatrix(float[] a,int at){return new Matrix4f(a[at],a[at+4],a[at+8],0,a[at+1],a[at+5],a[at+9],0,a[at+2],a[at+6],a[at+10],0,a[at+3],a[at+7],a[at+11],1);}
    static void putRows(float[] out,int at,Matrix4f m){if(!m.isFinite())throw new IllegalArgumentException("Skin transform");float[] a=m.get(new float[16]);for(int row=0;row<3;row++)for(int col=0;col<4;col++)out[at+row*4+col]=a[col*4+row];}
    private record PaletteEdit(int record,int bones,float[] rows){}
    private record ValueEdit(int at,float[] values){}
    private static final class Source {
        final int record;final ModelInstance instance;final SkinningData skin;final float[] colour,shadow;
        final Matrix4f cameraModel;final Map<String,Matrix4f> modelJoints=new LinkedHashMap<>(),cameraJoints=new LinkedHashMap<>();
        Source(int record,ModelInstance instance,SkinningData skin,float[] colour,float[] shadow,Matrix4f world,Matrix4f view){
            this.record=record;this.instance=instance;this.skin=skin;this.colour=colour;this.shadow=shadow;cameraModel=new Matrix4f(view).mul(world);
            for(var e:skin.boneIndices.entrySet()){
                Matrix4f offset=offset(skin,e.getValue());if(offset==null)continue;
                Matrix4f joint=rowMatrix(shadow,e.getValue()*12).mul(new Matrix4f(offset).invert()).mul(BASIS);
                modelJoints.put(e.getKey(),joint);cameraJoints.put(e.getKey(),new Matrix4f(cameraModel).mul(joint));
            }
        }
    }
    static final class Transaction {
        final Object draws;final float[] values,originalRows;final int[] palettes;final int originalBones;
        final Map<Integer,Integer> oldPalettes=new LinkedHashMap<>();final Map<Integer,float[]> oldValues=new LinkedHashMap<>();
        Transaction(Object draws,float[] values,float[] rows,int[] palettes,int bones){this.draws=draws;this.values=values;originalRows=rows;this.palettes=palettes;originalBones=bones;}
        void palette(int record,int value){oldPalettes.putIfAbsent(record,palettes[record]);palettes[record]=value;}
        void value(int at,float[] value){oldValues.putIfAbsent(at,Arrays.copyOfRange(values,at,at+16));System.arraycopy(value,0,values,at,16);}
        void restore()throws ReflectiveOperationException{
            for(var e:oldPalettes.entrySet())palettes[e.getKey()]=e.getValue();
            for(var e:oldValues.entrySet())System.arraycopy(e.getValue(),0,values,e.getKey(),16);
            rowsField.set(draws,originalRows);bonesField.setInt(draws,originalBones);
        }
    }
}
