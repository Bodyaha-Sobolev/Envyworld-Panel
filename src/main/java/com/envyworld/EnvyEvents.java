package com.envyworld;

import com.envyworld.command.RulesCommand;
import com.envyworld.punish.Punishment;
import com.envyworld.punish.PunishmentType;
import com.envyworld.util.Durations;
import com.envyworld.util.Msg;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class EnvyEvents {
	private EnvyEvents() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> RulesCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(EnvyWorld::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> EnvyWorld.onServerStopped());

		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> allowChat(sender));
	}

	private static boolean allowChat(ServerPlayer sender) {
		if (!EnvyWorld.config().muteBlocksChat) {
			return true;
		}

		Punishment mute = EnvyWorld.punishments().active(sender.getUUID(), PunishmentType.MUTE);
		if (mute == null) {
			return true;
		}

		sender.sendSystemMessage(Msg.prefixed(Component.literal("Вы заглушены. Осталось: " + Durations.remaining(mute.expiresAt))
				.withStyle(ChatFormatting.RED)));
		return false;
	}
}
