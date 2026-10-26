package com.envyworld.mixin;

import com.envyworld.logs.OnlineScanner;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method="sendCommand",at=@At("HEAD"),cancellable=true)
    private void envyworld$localScoreboard(String command,CallbackInfo ci){
        if(com.envyworld.client.ServerScoreboard.intercept(command))ci.cancel();
    }
    @Inject(method="handleParticleEvent",at=@At("HEAD"),cancellable=true,require=0)
    private void envyworld$particleBurst(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket packet,CallbackInfo ci){
        if(com.envyworld.EnvyWorld.config().client.clientProtection&&(packet.getCount()>2048
                ||!Double.isFinite(packet.getX())||!Double.isFinite(packet.getY())||!Double.isFinite(packet.getZ())
                ||!Float.isFinite(packet.getXDist())||!Float.isFinite(packet.getYDist())||!Float.isFinite(packet.getZDist())||!Float.isFinite(packet.getMaxSpeed())))ci.cancel();
    }

    @Inject(method="sendChat",at=@At("TAIL"))
    private void envyworld$chatSent(String text,CallbackInfo ci){com.envyworld.logs.SessionChat.accept("Отправлено",text);}
    @Inject(method="sendCommand",at=@At("TAIL"))
    private void envyworld$commandSent(String command,CallbackInfo ci){com.envyworld.logs.SessionChat.accept("Команда","/"+command);}
    @Inject(method = "handleCommandSuggestions", at = @At("TAIL"))
	private void envyworld$collectSuggestions(ClientboundCommandSuggestionsPacket packet, CallbackInfo info) {
		OnlineScanner.accept(packet);
	}
}
