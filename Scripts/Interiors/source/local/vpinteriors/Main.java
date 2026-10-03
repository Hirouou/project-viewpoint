package local.vpinteriors;
import java.nio.file.*;
public final class Main {
    public static void main(String[] args) {
        try {
            Class<?> fs=Class.forName("zombie.ZomboidFileSystem");Object instance=fs.getField("instance").get(null);
            Path root=Path.of((String)fs.getMethod("getModDir",String.class).invoke(instance,"ViewpointFurnitureFix"));
            Path pack=findPack(root);
            SinkCounterSelector.load(pack);
            CurrentAssetSelector.load(pack);
        }catch(Throwable e){System.out.println("[ViewpointInteriors] Companion asset selection unavailable: "+e);}
    }
    static Path findPack(Path root) throws java.io.FileNotFoundException {
        for(Path p:new Path[]{root.resolve("42/media/modelpacks/furniturefix"),root.resolve("media/modelpacks/furniturefix"),root.resolve("common/media/modelpacks/furniturefix")})
            if(Files.isRegularFile(p.resolve("pz_counter_modern_floor__sink_inset_oval.obj")))return p;
        throw new java.io.FileNotFoundException("Sink countertop models under "+root);
    }
}
