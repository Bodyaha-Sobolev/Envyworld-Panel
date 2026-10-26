package com.envyworld.mixin;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Guard the engine itself so immediate and delayed SoundManager paths share a budget. */
@Mixin(SoundEngine.class)
public abstract class SoundProtectionMixin {
    @Inject(method="play",at=@At("HEAD"),cancellable=true,require=0)
    private void envyworld$soundBudget(SoundInstance sound,CallbackInfoReturnable<SoundEngine.PlayResult> ci){
        if(!com.envyworld.client.ClientProtection.allowSound(sound))ci.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
    }
    @Inject(method="play",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"),cancellable=true,require=0)
    private void envyworld$resolvedSound(SoundInstance sound,CallbackInfoReturnable<SoundEngine.PlayResult> ci){
        if(com.envyworld.EnvyWorld.config().client.clientProtection&&!com.envyworld.client.ClientProtection.finiteSound(sound))ci.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
    }
}
