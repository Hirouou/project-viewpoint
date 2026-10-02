package local.vpads;
import java.nio.file.*;
public final class Main {
    public static void main(String[] args)throws Exception{
        Class.forName("me.zed_0xff.zombie_buddy.Exposer").getMethod("exposeClass",Class.class).invoke(null,ViewpointADSTest.class);
        try{
            Class<?> fs=Class.forName("zombie.ZomboidFileSystem");Object instance=fs.getField("instance").get(null);
            Path root=Path.of((String)fs.getMethod("getModDir",String.class).invoke(instance,"ViewpointADSTest"));
            NativeBodyAim.load(root.resolve("42/media/aim-calibration"));
        }catch(Exception e){System.out.println("[ViewpointADS] Native calibration unavailable; original body retained: "+e);}
    }
}
