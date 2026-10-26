package com.envyworld.mixin;
import net.minecraft.client.*;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
public abstract class GameModeKeysMixin {
 @Shadow private boolean usedDebugKeyAsModifier;
 @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
 private void envyworld$commandSwitcher(long window,int action,KeyEvent key,CallbackInfo info){var c=Minecraft.getInstance();if(window==c.getWindow().handle()&&action==GLFW.GLFW_PRESS&&c.player!=null&&c.screen==null&&c.options.keyDebugSwitchGameMode.matches(key)&&GLFW.glfwGetKey(window,GLFW.GLFW_KEY_F3)==GLFW.GLFW_PRESS){usedDebugKeyAsModifier=true;c.setScreen(new net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen());info.cancel();}}
}
