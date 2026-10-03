package local.vpinteriors;

import java.util.*;

/** Projecting water is clipped against the main plane, rather than stacked boxes. */
public final class RoofDormerMesh {
    private static float[] aboveMain(float[] source,float centerY,float lift,float[] main,float mainCenter) {
        float slope=main[7]/main[6];
        float intercept=(main[5]*main[0]+main[6]*main[1]+main[7]*main[2])/main[6];
        float offset=lift-intercept+slope*(centerY-mainCenter);
        List<float[]> vertices=new ArrayList<>();
        for(int p=0;p<source.length;p+=24) {
            List<float[]> polygon=new ArrayList<>();
            for(int i=0;i<3;i++)polygon.add(Arrays.copyOfRange(source,p+i*8,p+(i+1)*8));
            List<float[]> clipped=new ArrayList<>();float[] a=polygon.get(2);
            float fa=a[1]+slope*a[2]+offset;
            for(float[] b:polygon) {
                float fb=b[1]+slope*b[2]+offset;
                if((fa>=0)!=(fb>=0)) {
                    float t=fa/(fa-fb);float[] v=new float[8];
                    for(int i=0;i<8;i++)v[i]=a[i]+t*(b[i]-a[i]);clipped.add(v);
                }
                if(fb>=0)clipped.add(b.clone());a=b;fa=fb;
            }
            for(int i=1;i+1<clipped.size();i++) {
                float[] v0=clipped.get(0),v1=clipped.get(i),v2=clipped.get(i+1);
                float ax=v1[0]-v0[0],ay=v1[1]-v0[1],az=v1[2]-v0[2];
                float bx=v2[0]-v0[0],by=v2[1]-v0[1],bz=v2[2]-v0[2];
                float dot=(ay*bz-az*by)*v0[5]+(az*bx-ax*bz)*v0[6]+(ax*by-ay*bx)*v0[7];
                if(dot>1e-9f) {vertices.add(v0);vertices.add(v1);vertices.add(v2);}
            }
        }
        float[] result=new float[vertices.size()*8];
        for(int i=0;i<vertices.size();i++)System.arraycopy(vertices.get(i),0,result,i*8,8);
        return result;
    }
    public static List<RoofCompletionMesh.Piece> pieces(RoofDormerPlan.Dormer plan,RoofCompletionMesh.Vertices data) {
        float[] mid=data.get(4),main=data.get(0);if(mid==null||main==null)return List.of();
        List<RoofCompletionMesh.Piece> result=new ArrayList<>();
        // The native full middle tile is opaque across its fitted hull. Reuse
        // its pixels at the lower tier by translating the height .8165; this
        // preserves the real native UV without sampling missing corner alpha.
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(int i=0;i<mid.length;i+=8) {min=Math.min(min,mid[i]);max=Math.max(max,mid[i]);}
        float highCenter=plan.lowX()-.5f,join=highCenter+min,gap=join-plan.ridge();
        if(gap<.35f||gap>.65f)return List.of();
        float lower=min+.20f;if(lower+gap>max-.10f)lower=max-.10f-gap;
        float[] cap=RoofCompletionMesh.strip(mid,RoofCompletionPlan.X_SLOPE,lower,lower+gap);
        float shift=plan.ridge()-(highCenter+lower),capLift=-mid[5]/mid[6]*shift;
        // Keep placement on an exact half-tile center. Fractional movement stays
        // in the mesh, avoiding absolute world float rounding at x>10000.
        for(int i=0;i<cap.length;i+=8)cap[i]+=highCenter-plan.ridge()+shift;
        for(int row=plan.mainLow()-2;row<=plan.mainLow();row++) {
            float cy=row+.5f;
            for(int rank=0;rank<2;rank++) {
                float sourceCenter=plan.lowX()-rank+.5f,center=2*plan.ridge()-sourceCenter;
                float lift=rank==0?-.8165f:0;
                float[] v=RoofCompletionMesh.reflect(mid,RoofCompletionPlan.X_SLOPE);
                v=aboveMain(v,cy,lift,main,plan.mainLow()+.5f);
                if(v.length>0)result.add(new RoofCompletionMesh.Piece(4,"dormer-opposite",center,cy,lift,v,plan.source()));
            }
            float[] sourceCap=aboveMain(cap,cy,capLift,main,plan.mainLow()+.5f);
            float[] oppositeCap=aboveMain(RoofCompletionMesh.reflect(cap,RoofCompletionPlan.X_SLOPE),cy,capLift,main,plan.mainLow()+.5f);
            if(sourceCap.length>0)result.add(new RoofCompletionMesh.Piece(4,"dormer-ridge-source",plan.ridge(),cy,capLift,sourceCap,plan.source()));
            if(oppositeCap.length>0)result.add(new RoofCompletionMesh.Piece(4,"dormer-ridge-opposite",plan.ridge(),cy,capLift,oppositeCap,plan.source()));
        }
        return result;
    }
    private RoofDormerMesh() {}
}
