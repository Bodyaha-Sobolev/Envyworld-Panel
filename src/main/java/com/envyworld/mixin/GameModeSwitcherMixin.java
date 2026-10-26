package com.envyworld.mixin;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the native selector; let the server decide command permissions. */
@Mixin(GameModeSwitcherScreen.class)
public abstract class GameModeSwitcherMixin {
    @Inject(method="switchToHoveredGameMode(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/debug/GameModeSwitcherScreen$GameModeIcon;)V", at=@At("HEAD"), cancellable=true)
    private static void envyworld$command(Minecraft client, @Coerce Object selected, CallbackInfo info) {
        if (client.player != null && client.gameMode != null && selected instanceof Enum<?> mode) {
            String name = mode.name().toLowerCase(Locale.ROOT);
            if (!client.gameMode.getPlayerMode().getName().equals(name))
                client.player.connection.sendCommand("gamemode " + name);
        }
        info.cancel();
    }
}
