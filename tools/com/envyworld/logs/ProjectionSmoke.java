package com.envyworld.logs;
import com.envyworld.client.PlayerBoxProjection;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class ProjectionSmoke {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static float[] project(double yaw,float fov,int width,int height){
        double angle=Math.toRadians(yaw);Vec3 eye=new Vec3(Math.sin(angle)*5,1,Math.cos(angle)*5);
        var view=new Matrix4f().lookAt((float)eye.x,1,(float)eye.z,0,1,0,0,1,0).m30(0).m31(0).m32(0);
        var matrix=new Matrix4f().perspective((float)Math.toRadians(fov),(float)width/height,.05f,1024).mul(view);
        Vec3 right=new Vec3(view.m00(),0,view.m20()).normalize();float[] out=new float[4];
        check(PlayerBoxProjection.project(matrix,eye,right,new AABB(-.52,0,-.52,.52,1.8,.52),.3,width,height,new Vector4f(),out),"Visible body projects at yaw "+yaw);return out;
    }
    public static void run(){
        var original=project(0,70,1920,1080);float width=original[2]-original[0],height=original[3]-original[1];
        for(double yaw:new double[]{17,45,89,137,180,225,315}){var box=project(yaw,70,1920,1080);check(Math.abs(box[2]-box[0]-width)<.01f&&Math.abs(box[3]-box[1]-height)<.01f,"Orbiting must not squeeze a world-aligned square at yaw "+yaw);}
        var wide=project(45,100,1920,1080);var narrow=project(45,50,1920,1080);check(wide[2]-wide[0]<width&&narrow[2]-narrow[0]>width,"FOV changes physical projection consistently");
        var small=project(45,70,320,180);check(Math.abs((small[2]-small[0])*6-width)<.01f,"GUI scale must not distort the projected width");
        float[] out=new float[4];check(!PlayerBoxProjection.project(new Matrix4f().perspective((float)Math.toRadians(70),16f/9,.05f,1024),Vec3.ZERO,new Vec3(1,0,0),new AABB(-.52,0,4,.52,1.8,5),.3,1920,1080,new Vector4f(),out),"Players behind the camera do not produce inverted or infinite rectangles");
        System.out.println("PASS: invariant body width across camera orbits, FOV, viewport scale and behind-camera rejection");
    }
}
