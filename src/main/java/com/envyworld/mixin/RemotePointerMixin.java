package com.envyworld.mixin;
import com.envyworld.client.RemoteGame;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MouseHandler.class)
public abstract class RemotePointerMixin {
 @Inject(method="getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D",at=@At("HEAD"),cancellable=true)
 private void envyworld$pointerX(Window window,CallbackInfoReturnable<Double> result){if(RemoteGame.pointerActive())result.setReturnValue(RemoteGame.pointerX());}
 @Inject(method="getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D",at=@At("HEAD"),cancellable=true)
 private void envyworld$pointerY(Window window,CallbackInfoReturnable<Double> result){if(RemoteGame.pointerActive())result.setReturnValue(RemoteGame.pointerY());}
}
