package com.envyworld.mixin;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntityRenderer.class)
public abstract class SpectatorNamesMixin {
 @Inject(method="shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",at=@At("HEAD"),cancellable=true)
 private void envyworld$singleNameplate(LivingEntity entity,double distance,CallbackInfoReturnable<Boolean> result){if(entity instanceof Player&&(com.envyworld.client.gui.SpectatorOverlay.active() && com.envyworld.EnvyWorld.config().client.espNames))result.setReturnValue(false);}
}
