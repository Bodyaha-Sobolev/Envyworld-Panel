package com.envyworld.mixin;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class LevelProjectionMixin {
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void envyworld$worldMatrix(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,Camera camera,Matrix4f view,Matrix4f projection,Matrix4f culling,GpuBufferSlice fog,Vector4f color,boolean sky,CallbackInfo callback){
        com.envyworld.client.WorldProjection.capture(camera,view,projection,delta.getGameTimeDeltaPartialTick(false));
    }
}
