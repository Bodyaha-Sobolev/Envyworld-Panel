package com.envyworld.punish;

import com.envyworld.EnvyWorld;
import com.envyworld.util.Durations;
import com.envyworld.util.Msg;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.permissions.Permissions;

import java.util.Date;
import java.util.UUID;

/**
 * Applies punishments. Bans (including IP bans) use the vanilla ban lists, so vanilla tooling and
 * the {@code /banlist} command keep working. Mutes and warnings are stored by the mod.
 */
public final class PunishmentService {
	private PunishmentService() {
	}

	public static void ban(CommandSourceStack source, NameAndId target, long durationMillis, String reason) throws CommandSyntaxException {
		checkImmunity(source, target);

		UserBanList bans = source.getServer().getPlayerList().getBans();
		if (bans.isBanned(target)) {
			throw error(target.name() + " уже забанен");
		}

		Date expires = durationMillis <= 0L ? null : new Date(System.currentTimeMillis() + durationMillis);
		String issuer = source.getTextName();
		bans.add(new UserBanListEntry(target, new Date(), issuer, expires, reason));

		long expiresAt = expires == null ? 0L : expires.getTime();
		AuditLog.write("BAN", target.name(), issuer, PunishmentType.BAN, reason, expiresAt, "uuid=" + target.id());

		ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
		if (online != null) {
			online.connection.disconnect(banScreen(reason, expiresAt));
		}

		broadcast(target.name() + " забанен на " + Durations.format(expiresAt == 0L ? 0L : expiresAt - System.currentTimeMillis())
				+ ". Причина: " + reason + " (выдал " + issuer + ")");
	}

	public static void banIp(CommandSourceStack source, NameAndId target, long durationMillis, String reason) throws CommandSyntaxException {
		checkImmunity(source, target);

		ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
		if (online == null) {
			throw error("Для IP-бана игрок должен быть онлайн (нужен его IP-адрес)");
		}

		String address = online.getIpAddress();
		IpBanList bans = source.getServer().getPlayerList().getIpBans();
		if (bans.isBanned(address)) {
			throw error("IP " + address + " уже забанен");
		}

		Date expires = durationMillis <= 0L ? null : new Date(System.currentTimeMillis() + durationMillis);
		String issuer = source.getTextName();
		bans.add(new IpBanListEntry(address, new Date(), issuer, expires, reason));

		long expiresAt = expires == null ? 0L : expires.getTime();
		AuditLog.write("BANIP", target.name(), issuer, PunishmentType.BAN, reason, expiresAt, "ip=" + address);

		online.connection.disconnect(banScreen("IP-бан: " + reason, expiresAt));
		broadcast(target.name() + " забанен по IP на " + Durations.format(expiresAt == 0L ? 0L : expiresAt - System.currentTimeMillis())
				+ ". Причина: " + reason + " (выдал " + issuer + ")");
	}

	public static void mute(CommandSourceStack source, NameAndId target, long durationMillis, String reason) throws CommandSyntaxException {
		checkImmunity(source, target);

		long now = System.currentTimeMillis();
		long expiresAt = durationMillis <= 0L ? 0L : now + durationMillis;
		String issuer = source.getTextName();

		EnvyWorld.punishments().add(new Punishment(UUID.randomUUID().toString(), target.id(), target.name(),
				PunishmentType.MUTE, reason, issuer, EnvyWorld.config().serverName, now, expiresAt));
		AuditLog.write("MUTE", target.name(), issuer, PunishmentType.MUTE, reason, expiresAt, null);

		ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
		if (online != null) {
			online.sendSystemMessage(Msg.prefixed(Component.literal("Вам выдан мут на " + Durations.remaining(expiresAt)
					+ ". Причина: " + reason).withStyle(ChatFormatting.RED)));
		}

		broadcast(target.name() + " получил мут на " + Durations.remaining(expiresAt)
				+ ". Причина: " + reason + " (выдал " + issuer + ")");
	}

	public static void warn(CommandSourceStack source, NameAndId target, String reason) throws CommandSyntaxException {
		checkImmunity(source, target);

		long now = System.currentTimeMillis();
		String issuer = source.getTextName();

		EnvyWorld.punishments().add(new Punishment(UUID.randomUUID().toString(), target.id(), target.name(),
				PunishmentType.WARN, reason, issuer, EnvyWorld.config().serverName, now, 0L));
		AuditLog.write("WARN", target.name(), issuer, PunishmentType.WARN, reason, 0L, null);

		ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
		if (online != null) {
			online.sendSystemMessage(Msg.prefixed(Component.literal("Вам выдано предупреждение. Причина: " + reason)
					.withStyle(ChatFormatting.YELLOW)));
		}

		broadcast(target.name() + " получил предупреждение. Причина: " + reason + " (выдал " + issuer + ")");
	}

	public static void kick(CommandSourceStack source, NameAndId target, String reason) throws CommandSyntaxException {
		checkImmunity(source, target);

		ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
		if (online == null) {
			throw error(target.name() + " не в сети — кик невозможен");
		}

		String issuer = source.getTextName();
		online.connection.disconnect(Component.literal("Кик с " + EnvyWorld.config().serverName + "\nПричина: " + reason
				+ "\nВыдал: " + issuer));
		AuditLog.write("KICK", target.name(), issuer, PunishmentType.KICK, reason, 0L, null);

		broadcast(target.name() + " кикнут. Причина: " + reason + " (выдал " + issuer + ")");
	}

	/** @return true when a ban was removed */
	public static boolean unban(CommandSourceStack source, NameAndId target, String reason) {
		boolean removedAccount = source.getServer().getPlayerList().getBans().remove(target);

		// Also lift an IP ban if the player is online and their address is banned.
		boolean removedIp = false;
		ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
		if (online != null) {
			IpBanList ipBans = source.getServer().getPlayerList().getIpBans();
			String address = online.getIpAddress();
			if (ipBans.isBanned(address)) {
				removedIp = ipBans.remove(address);
			}
		}

		if (removedAccount || removedIp) {
			AuditLog.write("UNBAN", target.name(), source.getTextName(), PunishmentType.BAN, reason, 0L,
					removedIp ? "account+ip" : "account");
		}
		return removedAccount || removedIp;
	}

	/** @return true when a mute was removed */
	public static boolean unmute(CommandSourceStack source, NameAndId target, String reason) {
		int removed = EnvyWorld.punishments().clear(target.id(), PunishmentType.MUTE);
		if (removed > 0) {
			AuditLog.write("UNMUTE", target.name(), source.getTextName(), PunishmentType.MUTE, reason, 0L, null);

			ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
			if (online != null) {
				online.sendSystemMessage(Msg.prefixed(Component.literal("С вас снят мут.").withStyle(ChatFormatting.GREEN)));
			}
		}
		return removed > 0;
	}

	private static void checkImmunity(CommandSourceStack source, NameAndId target) throws CommandSyntaxException {
		if (!EnvyWorld.config().protectStaff) {
			return;
		}
		if (!source.getServer().getPlayerList().isOp(target)) {
			return;
		}
		if (source.permissions().hasPermission(Permissions.COMMANDS_OWNER)) {
			return;
		}
		throw error("Нельзя наказывать персонал проекта");
	}

	private static Component banScreen(String reason, long expiresAt) {
		String until = expiresAt == 0L ? "навсегда" : "до " + Durations.format(expiresAt - System.currentTimeMillis());
		return Component.literal("Вы забанены на " + EnvyWorld.config().serverName + " " + until
				+ "\nПричина: " + reason + "\nОбжалование: envyworld.gg");
	}

	private static void broadcast(String text) {
		if (!EnvyWorld.config().announcePunishments) {
			return;
		}
		var server = EnvyWorld.server();
		if (server != null) {
			server.getPlayerList().broadcastSystemMessage(Msg.prefixed(Component.literal(text).withStyle(ChatFormatting.GRAY)), false);
		}
	}

	private static CommandSyntaxException error(String text) {
		return new SimpleCommandExceptionType(Msg.error(text)).create();
	}
}
