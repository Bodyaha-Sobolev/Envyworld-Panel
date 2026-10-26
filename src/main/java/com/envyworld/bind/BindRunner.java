package com.envyworld.bind;

import com.envyworld.EnvyWorld;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs configured commands when a player attacks something, uses an item, or presses the macro key.
 * Commands are executed by the server console, so any vanilla/modded command can be used.
 */
public final class BindRunner {
	private static final Map<String, Long> LAST_RUN = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_KEY = new ConcurrentHashMap<>();
	private static final int MAX_AREA_TARGETS = 16;
	private static final long KEY_MIN_INTERVAL_MS = 200L;

	private BindRunner() {
	}

	public static void trigger(BindTrigger trigger, Player player, Entity target) {
		if (trigger == BindTrigger.KEY || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		MinecraftServer server = serverPlayer.level().getServer();
		if (server == null) {
			return;
		}

		String item = heldItem(serverPlayer);
		long now = System.currentTimeMillis();

		for (Binding binding : EnvyWorld.config().binds) {
			if (!binding.matchesTrigger(trigger) || !matchesItem(binding, item) || onCooldown(binding, serverPlayer, now)) {
				continue;
			}
			run(server, binding, serverPlayer, target, item);
		}
	}

	/**
	 * Macro key (V by default): runs every {@link BindTrigger#KEY} bind matching the held item.
	 * When the bind has a radius, it runs once per entity in front of the player.
	 *
	 * @return number of executed commands
	 */
	public static int triggerKey(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return 0;
		}

		long now = System.currentTimeMillis();
		Long lastKey = LAST_KEY.put(player.getUUID(), now);
		if (lastKey != null && now - lastKey < KEY_MIN_INTERVAL_MS) {
			return 0;
		}

		String item = heldItem(player);
		int runs = 0;

		for (Binding binding : EnvyWorld.config().binds) {
			if (!binding.matchesTrigger(BindTrigger.KEY) || !matchesItem(binding, item) || onCooldown(binding, player, now)) {
				continue;
			}

			if (binding.radius > 0) {
				for (Entity target : areaTargets(player, binding.radius)) {
					run(server, binding, player, target, item);
					runs++;
				}
			} else {
				run(server, binding, player, null, item);
				runs++;
			}
		}

		return runs;
	}

	/** Entities in a cone in front of the player, sorted by distance. */
	public static List<Entity> areaTargets(ServerPlayer player, double radius) {
		Vec3 look = player.getLookAngle();
		Vec3 eye = player.getEyePosition();
		AABB box = player.getBoundingBox().inflate(radius);

		List<Entity> candidates = player.level().getEntities(player, box,
				entity -> entity.isAlive() && !entity.isSpectator() && !entity.isRemoved());
		List<Entity> result = new ArrayList<>();

		for (Entity entity : candidates) {
			Vec3 to = entity.position().add(0.0, entity.getBbHeight() / 2.0, 0.0).subtract(eye);
			double distance = to.length();

			if (distance < 1.0E-4 || distance > radius) {
				continue;
			}
			if (look.dot(to.scale(1.0 / distance)) < 0.35) {
				continue;
			}

			result.add(entity);
		}

		result.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
		return result.size() > MAX_AREA_TARGETS ? new ArrayList<>(result.subList(0, MAX_AREA_TARGETS)) : result;
	}

	private static void run(MinecraftServer server, Binding binding, ServerPlayer player, Entity target, String item) {
		String command = expand(binding.command, player, target, item);
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
	}

	private static boolean matchesItem(Binding binding, String item) {
		return binding.item.equalsIgnoreCase("any") || binding.item.equalsIgnoreCase(item);
	}

	private static boolean onCooldown(Binding binding, ServerPlayer player, long now) {
		if (binding.cooldownTicks <= 0) {
			return false;
		}

		String key = binding.id + ':' + player.getUUID();
		Long last = LAST_RUN.get(key);

		if (last != null && now - last < binding.cooldownTicks * 50L) {
			return true;
		}

		LAST_RUN.put(key, now);
		return false;
	}

	private static String heldItem(ServerPlayer player) {
		ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
		Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key == null ? "minecraft:air" : key.toString();
	}

	public static String expand(String template, ServerPlayer player, Entity target, String item) {
		String result = template;

		result = result.replace("%player%", player.getName().getString());
		result = result.replace("%player_uuid%", player.getUUID().toString());
		result = result.replace("%item%", item);
		result = result.replace("%world%", player.level().dimension().identifier().toString());
		result = result.replace("%x%", coordinate(player.getX()));
		result = result.replace("%y%", coordinate(player.getY()));
		result = result.replace("%z%", coordinate(player.getZ()));

		if (target != null) {
			result = result.replace("%target%", target.getName().getString());
			result = result.replace("%target_uuid%", target.getUUID().toString());
			result = result.replace("%target_health%", target instanceof LivingEntity living
					? String.format(Locale.ROOT, "%.1f", living.getHealth())
					: "0");
		} else {
			result = result.replace("%target%", "");
			result = result.replace("%target_uuid%", "");
			result = result.replace("%target_health%", "");
		}

		return result;
	}

	private static String coordinate(double value) {
		return String.valueOf((int) Math.floor(value));
	}
}
