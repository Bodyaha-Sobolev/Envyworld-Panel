package com.envyworld.mixin;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParticleEngine.class)
public abstract class ParticleProtectionMixin {
    @Inject(method="add",at=@At("HEAD"),cancellable=true,require=0)
    private void envyworld$particleBudget(Particle particle,CallbackInfo ci){if(!com.envyworld.client.ClientProtection.allowParticle())ci.cancel();}
}
