package com.envyworld.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe HUD rendering without claiming the GameRenderer call site from other render mods. */
@Mixin(Gui.class)
public abstract class HudCaptureMixin {
    @Inject(method="renderScoreboardSidebar",at=@At("HEAD"),cancellable=true)
    private void envyworld$hideSidebar(GuiGraphics graphics,DeltaTracker delta,CallbackInfo ci){
        if(com.envyworld.client.ServerScoreboard.hidden())ci.cancel();
    }
    @Inject(method="render",at=@At("TAIL"))
    private void envyworld$hudGraphics(GuiGraphics graphics,DeltaTracker delta,CallbackInfo ci){
        com.envyworld.client.gui.EnvyHud.captureGraphics(graphics);
    }
}
