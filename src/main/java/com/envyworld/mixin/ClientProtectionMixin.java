package com.envyworld.mixin;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EntityRenderDispatcher.class)
public abstract class ClientProtectionMixin {
    @Inject(method="shouldRender",at=@At("RETURN"),cancellable=true,require=0)
    private <E extends Entity> void envyworld$entityBudget(E entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> ci){
        if(ci.getReturnValueZ()&&!com.envyworld.client.ClientProtection.allowEntity(entity))ci.setReturnValue(false);
    }
}
