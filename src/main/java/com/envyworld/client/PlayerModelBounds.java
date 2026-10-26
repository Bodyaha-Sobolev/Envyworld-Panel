package com.envyworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.HashMap;
import java.util.Map;

/** Same-frame neutral skin/armor envelope. Limb animation never changes its dimensions. */
public final class PlayerModelBounds {
    private static final class Bounds { long frame; final float[] rect=new float[4]; }
    private static final Map<Integer,Bounds> entries=new HashMap<>();
    private static final Matrix4f projection=new Matrix4f();
    private static final Matrix4f baseModel=new Matrix4f();
    private static final Vector4f vertex=new Vector4f();
    private static final Map<ModelPart.Cube,float[]> corners=new java.util.IdentityHashMap<>();
    private static final Map<String,String[]> paths=new HashMap<>();
    private static long frame;
    private PlayerModelBounds(){}
    public static void beginFrame(){frame++;if(entries.size()>128)entries.clear();if(corners.size()>16384){corners.clear();paths.clear();}}
    public static void reset(){entries.clear();corners.clear();paths.clear();frame++;}
    public static void capture(HumanoidModel<?> model,AvatarRenderState state,PoseStack poses){
        var c=Minecraft.getInstance();
        if(!com.envyworld.client.gui.SpectatorOverlay.active()||c.player.getId()==state.id||!WorldProjection.copy(projection))return;
        double range=Math.max(16,Math.min(512,com.envyworld.EnvyWorld.config().client.espDistance));
        if(state.distanceToCameraSq>range*range)return;
        var b=entries.computeIfAbsent(state.id,id->new Bounds());var r=b.rect;
        if(b.frame!=frame){r[0]=r[1]=Float.POSITIVE_INFINITY;r[2]=r[3]=Float.NEGATIVE_INFINITY;b.frame=frame;}
        int w=c.getWindow().getGuiScaledWidth(),h=c.getWindow().getGuiScaledHeight();
        baseModel.set(poses.last().pose());
        model.root().visit(poses,(pose,path,index,cube)->{
            var part=model.root();if(!part.visible)return;
            for(String child:paths.computeIfAbsent(path,key->java.util.Arrays.stream(key.split("/")).filter(value->!value.isEmpty()).toArray(String[]::new))){if(!part.hasChild(child))return;part=part.getChild(child);if(!part.visible)return;}
            float[] points=corners.computeIfAbsent(cube,key->{
                // Build rest-pose coordinates without mutating the live model or running setupAnim again.
                Matrix4f rest=new Matrix4f();var ancestor=model.root();applyRest(rest,ancestor);
                for(String child:paths.get(path)){ancestor=ancestor.getChild(child);applyRest(rest,ancestor);}
                float minX=Float.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
                for(var polygon:key.polygons)for(var point:polygon.vertices()){minX=Math.min(minX,point.worldX());minY=Math.min(minY,point.worldY());minZ=Math.min(minZ,point.worldZ());maxX=Math.max(maxX,point.worldX());maxY=Math.max(maxY,point.worldY());maxZ=Math.max(maxZ,point.worldZ());}
                // A small fixed head allowance covers the helmet when looking down, without animated resizing.
                if(path.startsWith("/head"))minY-=.065f;
                float[] result=new float[24];var v=new Vector4f();
                for(int i=0;i<8;i++){v.set((i&1)==0?minX:maxX,(i&2)==0?minY:maxY,(i&4)==0?minZ:maxZ,1);rest.transform(v);result[i*3]=v.x;result[i*3+1]=v.y;result[i*3+2]=v.z;}return result;
            });
            for(int i=0;i<points.length;i+=3){
                // The native pose already contains camera-relative translation. Do not subtract the eye twice.
                vertex.set(points[i],points[i+1],points[i+2],1);baseModel.transform(vertex);projection.transform(vertex);
                if(vertex.w<=.01f)continue;
                float x=(vertex.x/vertex.w*.5f+.5f)*w,y=(.5f-vertex.y/vertex.w*.5f)*h;
                if(!Float.isFinite(x)||!Float.isFinite(y))continue;
                r[0]=Math.min(r[0],x);r[1]=Math.min(r[1],y);r[2]=Math.max(r[2],x);r[3]=Math.max(r[3],y);
            }
        });
    }
    private static void applyRest(Matrix4f matrix,ModelPart part){
        var p=part.getInitialPose();matrix.translate(p.x()/16,p.y()/16,p.z()/16);
        matrix.rotate(new org.joml.Quaternionf().rotationZYX(p.zRot(),p.yRot(),p.xRot()));matrix.scale(p.xScale(),p.yScale(),p.zScale());
    }
    public static boolean copy(int id,float padding,float[] rect){
        var b=entries.get(id);if(b==null||b.frame!=frame||!Float.isFinite(b.rect[0]))return false;
        rect[0]=b.rect[0]-padding;rect[1]=b.rect[1]-padding;
        rect[2]=b.rect[2]+padding;rect[3]=b.rect[3]+padding;return true;
    }
}
