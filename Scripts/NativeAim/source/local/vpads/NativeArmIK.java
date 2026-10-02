package local.vpads;

import java.util.*;
import org.joml.*;

/** Render-only two-bone IK. Shoulders, arm lengths and the native finger pose are preserved. */
public final class NativeArmIK {
    private NativeArmIK(){}
    public record Result(Map<String,Matrix4f> joints,Matrix4f rightHandDelta){}
    public static Result align(Map<String,Matrix4f> nativeJoints,Map<String,Matrix4f> goals,float amount) {
        return align(nativeJoints,goals,amount,null,null);
    }
    static Result align(Map<String,Matrix4f> nativeJoints,Map<String,Matrix4f> goals,float amount,Vector3f cameraRight,Vector3f cameraUp) {
        Map<String,Matrix4f> result=new LinkedHashMap<>();
        for(var e:nativeJoints.entrySet())result.put(e.getKey(),new Matrix4f(e.getValue()));
        float blend=java.lang.Math.max(0,java.lang.Math.min(1,amount));
        Matrix4f right=new Matrix4f();if(blend==0)return new Result(result,right);
        for(String side:new String[]{"R","L"}){
            String stem="Bip01_"+side+"_",upper=stem+"UpperArm",fore=stem+"Forearm",hand=stem+"Hand";
            Matrix4f a=nativeJoints.get(upper),b=nativeJoints.get(fore),c=nativeJoints.get(hand),goal=goals.get(hand);
            if(a==null||b==null||c==null||goal==null)throw new IllegalArgumentException("Missing native arm");
            Vector3f shoulder=a.getTranslation(new Vector3f()),elbow=b.getTranslation(new Vector3f()),wrist=c.getTranslation(new Vector3f());
            Vector3f wanted=new Vector3f(wrist).lerp(goal.getTranslation(new Vector3f()),blend);
            float l1=shoulder.distance(elbow),l2=elbow.distance(wrist);
            if(l1<1e-5f||l2<1e-5f)throw new IllegalArgumentException("Zero native arm length");
            Vector3f direction=new Vector3f(wanted).sub(shoulder);float distance=direction.length();
            if(distance<1e-6f)direction.set(new Vector3f(wrist).sub(shoulder));
            if(direction.lengthSquared()<1e-10f)direction.set(0,0,-1);
            direction.normalize();
            distance=java.lang.Math.max(java.lang.Math.abs(l1-l2)+1e-5f,java.lang.Math.min(l1+l2-1e-5f,distance));
            wanted.set(direction).mul(distance).add(shoulder);
            // Use the current elbow as the pole, preserving its native roll and bend side.
            Vector3f pole=new Vector3f(elbow).sub(shoulder);
            if(cameraRight!=null&&cameraUp!=null){
                Vector3f steady=new Vector3f(cameraRight).mul(side.equals("R")?1:-1).fma(-.6f,cameraUp);
                steady.fma(-steady.dot(direction),direction);
                if(steady.lengthSquared()>1e-8f)pole.lerp(steady.normalize().mul(pole.length()),blend);
            }
            pole.fma(-pole.dot(direction),direction);
            if(pole.lengthSquared()<1e-10f){pole.set(side.equals("R")?1:-1,-1,0);pole.fma(-pole.dot(direction),direction);}
            if(pole.lengthSquared()<1e-10f){pole.set(0,0,1);pole.fma(-pole.dot(direction),direction);}
            pole.normalize();
            float along=(l1*l1-l2*l2+distance*distance)/(2*distance);
            float height=(float)java.lang.Math.sqrt(java.lang.Math.max(0,l1*l1-along*along));
            Vector3f newElbow=new Vector3f(shoulder).fma(along,direction).fma(height,pole);
            Matrix4f newUpper=rotateAt(a,new Vector3f(elbow).sub(shoulder),new Vector3f(newElbow).sub(shoulder),shoulder);
            Matrix4f newFore=rotateAt(b,new Vector3f(wrist).sub(elbow),new Vector3f(wanted).sub(newElbow),newElbow);
            Quaternionf rotation=c.getUnnormalizedRotation(new Quaternionf()).normalize()
                .slerp(goal.getUnnormalizedRotation(new Quaternionf()).normalize(),blend);
            Matrix4f newHand=new Matrix4f().translationRotateScale(wanted,rotation,c.getScale(new Vector3f()));
            Matrix4f delta=new Matrix4f(newHand).mul(new Matrix4f(c).invert());
            result.put(upper,newUpper);result.put(fore,newFore);result.put(hand,newHand);
            // Finger locals and props keep exactly the animation already blended by the game.
            for(var e:nativeJoints.entrySet())if(e.getKey().startsWith(stem+"Finger")||e.getKey().equals("Bip01_Prop"+(side.equals("R")?"1":"2")))
                result.put(e.getKey(),new Matrix4f(delta).mul(e.getValue()));
            if(side.equals("R"))right=delta;
        }
        for(Matrix4f m:result.values())if(!m.isFinite())throw new IllegalArgumentException("Nonfinite native IK");
        return new Result(result,right);
    }
    private static Matrix4f rotateAt(Matrix4f original,Vector3f before,Vector3f after,Vector3f position){
        Quaternionf q=new Quaternionf().rotationTo(before.normalize(),after.normalize());
        Matrix4f out=new Matrix4f().rotation(q).mul(original);out.setTranslation(position);return out;
    }
}
