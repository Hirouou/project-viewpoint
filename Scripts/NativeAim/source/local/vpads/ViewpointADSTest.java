package local.vpads;
import java.lang.reflect.Field;

/** Lua controls for the original body. F7 disables only the render alignment. */
public final class ViewpointADSTest {
    private static Field enabled,thirdPerson,freeCamera;
    public static void setViewmodelState(Object player,String item,boolean active,boolean aiming){NativeBodyAim.selectState(player,item,active&&isViewpointActive(),aiming);}
    public static boolean hasViewmodel(String item){return NativeBodyAim.hasCalibration(item);}
    public static boolean isViewpointActive(){
        try{
            if(enabled==null){enabled=Class.forName("viewpoint.core.View").getField("enabled");thirdPerson=Class.forName("viewpoint.input.ThirdPerson").getField("active");freeCamera=Class.forName("viewpoint.input.FreeCam").getField("active");}
            return enabled.getBoolean(null)&&!freeCamera.getBoolean(null);
        }catch(ReflectiveOperationException|LinkageError e){return false;}
    }
    public static boolean isFirstPerson(){
        try{return isViewpointActive()&&!thirdPerson.getBoolean(null);}
        catch(ReflectiveOperationException|LinkageError e){return false;}
    }
    public static String captureCamera(Object player){return NativeBodyAim.owner()==player?NativeBodyAim.cameraSample():"{\"error\":\"No local native frame\"}";}
    public static String diagnose(Object player){return "ViewpointADS 0.4.8 native skeleton\nfirst-person aim target=camera yaw\nactive="+NativeBodyAim.active()+"\nreason="+NativeBodyAim.reason()+"\nrender palette edits="+NativeBodyAim.nativeParts()+"\nduplicate arm/weapon meshes=0\n"+captureCamera(player)+"\n";}
}
