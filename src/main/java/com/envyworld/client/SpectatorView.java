package com.envyworld.client;
import com.envyworld.EnvyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;
/** Presentation only. Server game mode, abilities and permissions are untouched. */
public final class SpectatorView {
 private SpectatorView(){}
 public static boolean active(){var c=Minecraft.getInstance();return EnvyWorld.config().client.spectatorCreativeUi&&c.player!=null&&c.gameMode!=null&&c.gameMode.getPlayerMode()==GameType.SPECTATOR;}
 public static GameType displayed(GameType actual){return actual==GameType.SPECTATOR&&active()?GameType.CREATIVE:actual;}
}
