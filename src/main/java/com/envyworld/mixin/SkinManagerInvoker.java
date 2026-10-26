package com.envyworld.mixin;

import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SkinManager.class)
public interface SkinManagerInvoker {
    @Invoker("registerTextures")
    CompletableFuture<PlayerSkin> envyworld$registerTextures(UUID id, MinecraftProfileTextures textures);
}
