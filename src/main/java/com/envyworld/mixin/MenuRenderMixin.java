package com.envyworld.mixin;

import com.envyworld.client.gui.EnvyScreen;
import com.envyworld.client.gui.UiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MenuRenderMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private net.minecraft.client.gui.render.GuiRenderer guiRenderer;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private net.minecraft.client.renderer.fog.FogRenderer fogRenderer;

    // Flush the ordinary HUD beneath the glass before the screen extracts its widgets.
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",shift=At.Shift.AFTER))
    private void envyworld$renderHudBelowMenu(DeltaTracker delta,boolean renderLevel,CallbackInfo callback){
        if(Minecraft.getInstance().screen instanceof com.envyworld.client.gui.MenuScreen)
            guiRenderer.render(fogRenderer.getBuffer(net.minecraft.client.renderer.fog.FogRenderer.FogMode.NONE));
    }
    @Inject(method="render",at=@At("HEAD"))
    private void envyworld$smoothRemoteLook(DeltaTracker delta,boolean renderLevel,CallbackInfo callback){com.envyworld.client.ClientProtection.beginFrame();com.envyworld.client.RemoteGame.smoothLook();}
    @Inject(method="render",at=@At("TAIL"))
    private void envyworld$remoteFrame(DeltaTracker delta,boolean renderLevel,CallbackInfo callback){com.envyworld.client.RemoteGame.capture();}
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"),require=0)
    private void envyworld$renderMenuScreen(DeltaTracker delta, boolean renderLevel, CallbackInfo callback) {
        if (Minecraft.getInstance().screen instanceof com.envyworld.client.gui.MenuScreen) {
            com.envyworld.client.gui.EnvyHud.noticesOverlay(); UiRenderer.beforeVanillaGui();
        }
        else com.envyworld.client.gui.EnvyHud.beforeGameGui();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER),require=0)
    private void envyworld$renderWexTooltip(DeltaTracker delta, boolean renderLevel, CallbackInfo callback) {
        if (Minecraft.getInstance().screen instanceof com.envyworld.client.gui.MenuScreen) UiRenderer.afterVanillaGui();
        else { com.envyworld.client.gui.EnvyHud.afterGameGui(); UiRenderer.warmUp(); }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void envyworld$closeUiRendererer(CallbackInfo callback) {
        UiRenderer.close();
    }
}
