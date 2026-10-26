package com.envyworld.client;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Camera-facing body prism: stable shoulder width while orbiting a player. */
public final class PlayerBoxProjection {
    private PlayerBoxProjection(){}
    public static boolean project(Matrix4fc matrix,Vec3 eye,Vec3 right,AABB body,double halfDepth,int width,int height,Vector4f scratch,float[] out){
        out[0]=out[1]=Float.POSITIVE_INFINITY;out[2]=out[3]=Float.NEGATIVE_INFINITY;
        double cx=(body.minX+body.maxX)*.5,cz=(body.minZ+body.maxZ)*.5,halfWidth=(body.maxX-body.minX)*.5;
        for(int i=0;i<8;i++){
            double horizontal=(i&1)==0?-halfWidth:halfWidth,depth=(i&4)==0?-halfDepth:halfDepth;
            scratch.set((float)(cx+right.x*horizontal-right.z*depth-eye.x),(float)(((i&2)==0?body.minY:body.maxY)-eye.y),(float)(cz+right.z*horizontal+right.x*depth-eye.z),1);
            matrix.transform(scratch);if(scratch.w<=.01f)return false;
            float x=(scratch.x/scratch.w*.5f+.5f)*width,y=(.5f-scratch.y/scratch.w*.5f)*height;
            if(!Float.isFinite(x)||!Float.isFinite(y))return false;
            out[0]=Math.min(out[0],x);out[2]=Math.max(out[2],x);out[1]=Math.min(out[1],y);out[3]=Math.max(out[3],y);
        }
        return out[2]>=0&&out[0]<=width&&out[3]>=0&&out[1]<=height;
    }
}
