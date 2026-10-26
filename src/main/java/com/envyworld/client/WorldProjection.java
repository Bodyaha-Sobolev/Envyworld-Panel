package com.envyworld.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** The exact world matrices used by this frame, including hurt/view bob and screen effects. */
public final class WorldProjection {
    private static final Matrix4f matrix=new Matrix4f();
    private static Vec3 eye=Vec3.ZERO;
    private static Vec3 right=new Vec3(1,0,0);
    private static Object level;
    private static float partial;
    private WorldProjection(){}
    public static void capture(Camera camera,Matrix4f view,Matrix4f projection,float tick){PlayerModelBounds.beginFrame();matrix.set(projection).mul(view);eye=camera.position();right=new Vec3(view.m00(),0,view.m20()).normalize();partial=tick;level=Minecraft.getInstance().level;}
    public static boolean copy(Matrix4f out){if(level==null||level!=Minecraft.getInstance().level)return false;out.set(matrix);return true;}
    public static Vec3 eye(){return eye;}
    public static Vec3 right(){return right;}
    public static float partial(){return partial;}
    public static void reset(){PlayerModelBounds.reset();level=null;eye=Vec3.ZERO;}
}
