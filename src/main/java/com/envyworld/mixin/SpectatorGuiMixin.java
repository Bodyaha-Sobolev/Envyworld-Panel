package com.envyworld.mixin;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
@Mixin(Gui.class)
public abstract class SpectatorGuiMixin {
 @WrapOperation(method={"renderHotbarAndDecorations","renderCrosshair"},at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;"))
 private GameType envyworld$normalHotbar(MultiPlayerGameMode mode,Operation<GameType> original){return com.envyworld.client.SpectatorView.displayed(original.call(mode));}
}
