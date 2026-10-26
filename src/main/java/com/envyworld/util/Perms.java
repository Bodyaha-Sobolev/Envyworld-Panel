package com.envyworld.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;

import java.util.function.Predicate;

/**
 * Permission helper.
 *
 * <p>If the Fabric Permissions API is installed (LuckPerms and friends), the given node is checked,
 * so donation ranks can be allowed to use commands without OP. Otherwise the vanilla permission
 * level from the config is used as a fallback.
 */
public final class Perms {
	private static final boolean PERMISSIONS_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

	private Perms() {
	}

	public static Predicate<CommandSourceStack> require(String node, int fallbackLevel) {
		return source -> check(source, node, fallbackLevel);
	}

	public static boolean check(CommandSourceStack source, String node, int fallbackLevel) {
		if (PERMISSIONS_API) {
			return LuckPermsBridge.check(source, node, fallbackLevel);
		}
		return vanilla(source, fallbackLevel);
	}

	private static boolean vanilla(CommandSourceStack source, int level) {
		return switch (Math.max(0, Math.min(4, level))) {
			case 0 -> true;
			case 1 -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
			case 2 -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
			case 3 -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
			default -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER);
		};
	}

	/** Separate class so the optional API is only touched when it is actually installed. */
	private static final class LuckPermsBridge {
		private static boolean check(CommandSourceStack source, String node, int fallbackLevel) {
			int level = Math.max(0, Math.min(4, fallbackLevel));
			return me.lucko.fabric.api.permissions.v0.Permissions.check(source, node, PermissionLevel.byId(level));
		}
	}
}
