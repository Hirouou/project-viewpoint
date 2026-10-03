package local.vpinteriors;

import java.util.*;

/** CPU-only native UV preserving surface transformations, also used by fixtures. */
public final class RoofCompletionMesh {
    public record Piece(int index,String kind,float x,float y,float lift,float[] vertices,RoofCompletionPlan.Tile source) {}
    public interface Vertices { float[] get(int index); }
    public static float[] reflect(float[] input,int axis) {
        int component=axis==RoofCompletionPlan.Y_SLOPE?2:0;
        float[] out=input.clone();
        for(int p=0;p<out.length;p+=8) { out[p+component]=-out[p+component];out[p+5+component]=-out[p+5+component]; }
        // Reflection changes handedness. Reverse every triangle, keeping UVs
        // attached to their vertices; flipping normals alone is insufficient.
        for(int t=0;t<out.length;t+=24)for(int c=0;c<8;c++) {
            float tmp=out[t+8+c];out[t+8+c]=out[t+16+c];out[t+16+c]=tmp;
        }
        return out;
    }
    private static List<float[]> clip(List<float[]> polygon,int component,float bound,boolean above) {
        List<float[]> out=new ArrayList<>();
        if(polygon.isEmpty())return out;
        float[] a=polygon.get(polygon.size()-1);boolean ain=above?a[component]>=bound:a[component]<=bound;
        for(float[] b:polygon) {
            boolean bin=above?b[component]>=bound:b[component]<=bound;
            if(ain!=bin) {
                float ratio=(bound-a[component])/(b[component]-a[component]);float[] v=new float[8];
                for(int k=0;k<8;k++)v[k]=a[k]+ratio*(b[k]-a[k]);
                v[component]=bound;out.add(v);
            }
            if(bin)out.add(b.clone());a=b;ain=bin;
        }
        return out;
    }
    public static float[] strip(float[] source,int axis,float lower,float upper) {
        int component=axis==RoofCompletionPlan.Y_SLOPE?2:0;List<float[]> result=new ArrayList<>();
        for(int t=0;t<source.length;t+=24) {
            List<float[]> polygon=new ArrayList<>();
            for(int k=0;k<3;k++)polygon.add(Arrays.copyOfRange(source,t+k*8,t+(k+1)*8));
            polygon=clip(clip(polygon,component,lower,true),component,upper,false);
            for(int k=1;k+1<polygon.size();k++) {
                result.add(polygon.get(0));result.add(polygon.get(k));result.add(polygon.get(k+1));
            }
        }
        float[] out=new float[result.size()*8];
        for(int i=0;i<result.size();i++)System.arraycopy(result.get(i),0,out,i*8,8);
        return out;
    }
    public static List<Piece> pieces(RoofCompletionPlan.Plan plan,Vertices data) {
        List<Piece> out=new ArrayList<>();int component=plan.axis==RoofCompletionPlan.Y_SLOPE?2:0;
        float[] high=data.get(plan.index(2));if(high==null)return List.of();
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(int p=0;p<high.length;p+=8) { min=Math.min(min,high[p+component]);max=Math.max(max,high[p+component]); }
        float highCenter=plan.along(2)+.5f,join=highCenter+min,gap=join-plan.ridge;
        if(gap<-.075f||gap>.70f)return List.of();
        float[] cap=null;float shift=0,raise=0;
        if(gap>.01f) {
            // Reuse an opaque interior band of the highest native tile. Its
            // affine plane is translated to the missing half-tile at the ridge;
            // UVs are clipped/interpolated and kept unchanged, never stretched.
            float lower=min+.20f;
            if(lower+gap>max-.10f)lower=max-.10f-gap;
            cap=strip(high,plan.axis,lower,lower+gap);
            shift=plan.ridge-(highCenter+lower);
            raise=-high[5+component]/high[6]*shift;
        }
        for(int cross=plan.start;cross<plan.end;cross++) {
            for(int rank=0;rank<3;rank++) {
                float center=plan.along(rank)+.5f,mirror=2*plan.ridge-center;
                float[] v=data.get(plan.index(rank));if(v==null)return List.of();
                out.add(new Piece(plan.index(rank),"opposite",plan.axis==0?cross+.5f:mirror,
                    plan.axis==0?mirror:cross+.5f,0,reflect(v,plan.axis),plan.source(cross,rank)));
            }
            if(cap!=null && cap.length>0) {
                float center=highCenter+shift,mirror=2*plan.ridge-center;
                out.add(new Piece(plan.index(2),"ridge-source",plan.axis==0?cross+.5f:center,
                    plan.axis==0?center:cross+.5f,raise,cap,plan.source(cross,2)));
                out.add(new Piece(plan.index(2),"ridge-opposite",plan.axis==0?cross+.5f:mirror,
                    plan.axis==0?mirror:cross+.5f,raise,reflect(cap,plan.axis),plan.source(cross,2)));
            }
        }
        return out;
    }
    private RoofCompletionMesh() {}
}
