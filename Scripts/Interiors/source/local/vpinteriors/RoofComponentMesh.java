package local.vpinteriors;
import java.util.*;

/** Native hull reflection, then actual triangle-footprint intersection subtraction. */
public final class RoofComponentMesh {
    private static List<RoofCompletionMesh.Piece> raw(RoofComponentPlan.Component p,RoofCompletionMesh.Vertices data) {
        int component=p.axis==0?2:0;List<RoofCompletionMesh.Piece> out=new ArrayList<>();
        float[] high=data.get(p.index(p.tiers-1));if(high==null)return List.of();
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(int i=0;i<high.length;i+=8) {min=Math.min(min,high[i+component]);max=Math.max(max,high[i+component]);}
        float center=p.along(p.tiers-1)+.5f,gap=(center-p.ridge)+min;
        if(gap<-.075f||gap>.70f)return List.of();
        float[] cap=null;float lift=0;
        if(gap>.01f) {
            float lower=min+.20f;if(lower+gap>max-.10f)lower=max-.10f-gap;
            cap=RoofCompletionMesh.strip(high,p.axis,lower,lower+gap);
            float shift=p.ridge-center-lower;lift=-high[5+component]/high[6]*shift;
            for(int i=0;i<cap.length;i+=8)cap[i+component]+=center-p.ridge+shift;
        }
        for(int c=p.start;c<p.end;c++) {
            for(int rank=0;rank<p.tiers;rank++) {
                float mirror=2*p.ridge-(p.along(rank)+.5f);
                float[] v=data.get(p.index(rank));if(v==null)return List.of();
                out.add(new RoofCompletionMesh.Piece(p.index(rank),"component-opposite",p.axis==0?c+.5f:mirror,p.axis==0?mirror:c+.5f,0,RoofCompletionMesh.reflect(v,p.axis),p.source(c,rank)));
            }
            if(cap!=null) {
                float x=p.axis==0?c+.5f:p.ridge,y=p.axis==0?p.ridge:c+.5f;
                out.add(new RoofCompletionMesh.Piece(p.index(p.tiers-1),"component-ridge-source",x,y,lift,cap,p.source(c,p.tiers-1)));
                out.add(new RoofCompletionMesh.Piece(p.index(p.tiers-1),"component-ridge-opposite",x,y,lift,RoofCompletionMesh.reflect(cap,p.axis),p.source(c,p.tiers-1)));
            }
        }return out;
    }
    private static List<float[]> clip(List<float[]> poly,float nx,float ny,float nz,float d,boolean positive) {
        List<float[]> out=new ArrayList<>();if(poly.isEmpty())return out;
        float[] a=poly.get(poly.size()-1);float fa=nx*a[0]+ny*a[1]+nz*a[2]+d;
        for(float[] b:poly) {
            float fb=nx*b[0]+ny*b[1]+nz*b[2]+d;boolean ai=positive?fa>=0:fa<=0,bi=positive?fb>=0:fb<=0;
            if(ai!=bi) {float ratio=fa/(fa-fb);float[] v=new float[8];for(int j=0;j<8;j++)v[j]=a[j]+ratio*(b[j]-a[j]);out.add(v);}
            if(bi)out.add(b.clone());a=b;fa=fb;
        }return out;
    }
    private static float[] triangles(List<List<float[]>> polygons) {
        List<float[]> out=new ArrayList<>();
        for(var polygon:polygons)for(int k=1;k+1<polygon.size();k++) {
            float[] a=polygon.get(0),b=polygon.get(k),c=polygon.get(k+1);
            float ax=b[0]-a[0],ay=b[1]-a[1],az=b[2]-a[2],bx=c[0]-a[0],by=c[1]-a[1],bz=c[2]-a[2];
            if((ay*bz-az*by)*a[5]+(az*bx-ax*bz)*a[6]+(ax*by-ay*bx)*a[7]>1e-9f) {out.add(a);out.add(b);out.add(c);}
        }
        float[] data=new float[out.size()*8];for(int i=0;i<out.size();i++)System.arraycopy(out.get(i),0,data,i*8,8);return data;
    }
    /** Preserve generated geometry outside the blocker triangle's actual XZ hull. */
    public static float[] aboveTriangle(float[] generated,float lift,float[] blocker,int offset,float dx,float dz,float blockerLift) {
        float[] bx=new float[3],bz=new float[3];for(int k=0;k<3;k++) {bx[k]=blocker[offset+k*8]+dx;bz[k]=blocker[offset+k*8+2]+dz;}
        float orientation=(bx[1]-bx[0])*(bz[2]-bz[0])-(bz[1]-bz[0])*(bx[2]-bx[0]);
        if(Math.abs(orientation)<1e-8f||blocker[offset+6]<.5f)return generated;
        float nx=blocker[offset+5],ny=blocker[offset+6],nz=blocker[offset+7];
        float constant=nx*blocker[offset]+ny*blocker[offset+1]+nz*blocker[offset+2];
        float planeD=ny*(lift-blockerLift)-nx*dx-nz*dz-constant;
        List<List<float[]>> output=new ArrayList<>();
        for(int t=0;t<generated.length;t+=24) {
            List<float[]> inside=new ArrayList<>();for(int k=0;k<3;k++)inside.add(Arrays.copyOfRange(generated,t+k*8,t+(k+1)*8));
            boolean above=true;for(float[] point:inside)above&=nx*point[0]+ny*point[1]+nz*point[2]+planeD>=-1e-5f;
            if(above) {output.add(inside);continue;}
            boolean outsideHull=false;
            for(int edge=0;edge<3;edge++) {
                int next=(edge+1)%3;float ex=bx[next]-bx[edge],ez=bz[next]-bz[edge],d=ez*bx[edge]-ex*bz[edge];
                boolean outside=true;for(float[] point:inside)outside&=(orientation>0?1:-1)*(-ez*point[0]+ex*point[2]+d)<=1e-6f;
                outsideHull|=outside;
            }
            if(outsideHull) {output.add(inside);continue;}
            for(int edge=0;edge<3&&!inside.isEmpty();edge++) {
                int next=(edge+1)%3;float ex=bx[next]-bx[edge],ez=bz[next]-bz[edge];
                float d=ez*bx[edge]-ex*bz[edge];boolean positive=orientation>0;
                var outside=clip(inside,-ez,0,ex,d,!positive);if(outside.size()>=3)output.add(outside);
                inside=clip(inside,-ez,0,ex,d,positive);
            }
            if(inside.size()>=3) {inside=clip(inside,nx,ny,nz,planeD,true);if(inside.size()>=3)output.add(inside);}
        }return triangles(output);
    }
    private static float[] clipMesh(float[] generated,RoofCompletionMesh.Piece piece,float[] other,float dx,float dz,float lift) {
        // A quick hull rejection keeps this work proportional to local joins.
        if(Math.abs(dx)>2||Math.abs(dz)>2)return generated;
        for(int t=0;t<other.length&&generated.length>0;t+=24)
            if(other[t+6]>.5f)generated=aboveTriangle(generated,piece.lift(),other,t,dx,dz,lift);
        return generated;
    }
    public static List<RoofCompletionMesh.Piece> pieces(RoofComponentPlan.Component p,List<RoofComponentPlan.Component> all,RoofCompletionMesh.Vertices data) {
        List<RoofCompletionMesh.Piece> result=new ArrayList<>();List<RoofCompletionMesh.Piece> existing=new ArrayList<>();
        for(var other:all)if(other!=p&&other.axis!=p.axis&&other.level==p.level)existing.addAll(raw(other,data));
        for(var piece:raw(p,data)) {
            float[] v=piece.vertices();int x=(int)Math.floor(piece.x()),y=(int)Math.floor(piece.y());
            for(int oy=y-1;oy<=y+1&&v.length>0;oy++)for(int ox=x-1;ox<=x+1&&v.length>0;ox++) {
                var cell=p.grid.at(ox,oy,p.level);if(cell==null||!cell.loaded())continue;
                for(var tile:cell.tiles())if(tile.roof()!=-1) {
                    float[] blocker=p.geometry.vertices(tile);
                    if(RoofComponentPlan.perpendicular(blocker,p.axis))v=clipMesh(v,piece,blocker,ox+.5f-piece.x(),oy+.5f-piece.y(),0);
                }
            }
            for(var other:existing)if(v.length>0)v=clipMesh(v,piece,other.vertices(),other.x()-piece.x(),other.y()-piece.y(),other.lift());
            if(v.length>0)result.add(new RoofCompletionMesh.Piece(piece.index(),piece.kind(),piece.x(),piece.y(),piece.lift(),v,piece.source()));
        }return result;
    }
    private RoofComponentMesh() {}
}
