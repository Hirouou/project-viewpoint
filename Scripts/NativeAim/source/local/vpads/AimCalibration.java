package local.vpads;

import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;

/** Approved camera-space joint locations only. No arms, gun mesh or texture is loaded. */
public final class AimCalibration {
    public final Map<String,Matrix4f> joints;
    private AimCalibration(Map<String,Matrix4f> joints){this.joints=Collections.unmodifiableMap(joints);}
    /** Authoring bones use Rz(-90) on the right; the game uses the original X bone axes. */
    Map<String,Matrix4f> nativeJoints(){
        Map<String,Matrix4f> out=new LinkedHashMap<>();
        for(var e:joints.entrySet())out.put(e.getKey(),new Matrix4f(e.getValue()).rotateZ((float)java.lang.Math.PI/2));
        return out;
    }
    public static AimCalibration read(Path path)throws Exception {
        Map<String,Matrix4f> out=new LinkedHashMap<>();
        for(String line:Files.readAllLines(path)){
            if(line.isBlank())continue;
            String[] a=line.split("\\|");if(a.length!=4)throw new IllegalArgumentException("Joint calibration: "+path);
            String[] fields=a[3].split(",");if(fields.length!=16)throw new IllegalArgumentException("Matrix size");
            float[] f=new float[16];for(int i=0;i<16;i++){f[i]=Float.parseFloat(fields[i]);if(!Float.isFinite(f[i]))throw new IllegalArgumentException("Matrix value");}
            Matrix4f m=new Matrix4f().set(f).invert();if(!m.isFinite())throw new IllegalArgumentException("Singular joint");
            if(out.put(a[0],m)!=null)throw new IllegalArgumentException("Duplicate joint");
        }
        for(String side:new String[]{"R","L"})for(String bone:new String[]{"UpperArm","Forearm","Hand"})
            if(!out.containsKey("Bip01_"+side+"_"+bone))throw new IllegalArgumentException("Missing arm joint");
        return new AimCalibration(out);
    }
}
