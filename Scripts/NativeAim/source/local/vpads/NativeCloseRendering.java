package local.vpads;
import org.joml.Matrix4f;
import viewpoint.render.SceneData;
import viewpoint.render.FrameContext;
import viewpoint.platform.GlProgram;
/** One consistent depth projection for close native arms/weapons and scene effects. */
public final class NativeCloseRendering {
    static final float NEAR=.008f,FAR=400f;
    private NativeCloseRendering(){}
    static boolean enabled(){return NativeBodyAim.wants()&&ViewpointADSTest.isFirstPerson();}
    static void projection(Matrix4f projection,boolean close){
        if(!close)return;
        // Preserve field of view/aspect and jitter; only change the depth terms.
        projection.m22(-(FAR+NEAR)/(FAR-NEAR));
        projection.m32(-2*FAR*NEAR/(FAR-NEAR));
    }
    public static void prepare(Object scene){if(scene instanceof SceneData data)projection(data.projection,enabled());}
    public static void camera(FrameContext frame,GlProgram program){
        Matrix4f p=frame.scene.projection;
        // Match the frame already captured even if the user toggles modes mid-draw.
        if(Math.abs(p.m32()-(-2*FAR*NEAR/(FAR-NEAR)))>1e-6f)return;
        program.set("uCamera",NEAR,FAR,1/p.m00(),1/p.m11());
    }
}
