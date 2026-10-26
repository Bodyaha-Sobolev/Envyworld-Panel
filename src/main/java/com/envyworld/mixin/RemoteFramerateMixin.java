package com.envyworld.mixin;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FramerateLimitTracker.class)
public abstract class RemoteFramerateMixin {
    @Inject(method="getFramerateLimit",at=@At("HEAD"),cancellable=true)
    private void envyworld$liveLimit(CallbackInfoReturnable<Integer> result){
        if(com.envyworld.client.RemoteGame.streaming())result.setReturnValue(Math.max(60,Minecraft.getInstance().options.framerateLimit().get()));
    }
    @Inject(method="getThrottleReason",at=@At("HEAD"),cancellable=true)
    private void envyworld$liveWake(CallbackInfoReturnable<FramerateLimitTracker.FramerateThrottleReason> result){
        if(com.envyworld.client.RemoteGame.streaming())result.setReturnValue(FramerateLimitTracker.FramerateThrottleReason.NONE);
    }
}
