package com.envyworld.client;

import com.envyworld.EnvyWorld;
import com.envyworld.bind.BindTrigger;
import com.envyworld.bind.Binding;
import com.envyworld.items.CustomItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 * Runs local macros on the client. Triggered commands are typed into chat as the player,
 * so they use the player's normal privileges. Nothing is sent to a server-side mod.
 */
public final class ClientMacros {
	private static final Map<String, Long> LAST_RUN = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_KEY = new ConcurrentHashMap<>();
	private static final int MAX_AREA_TARGETS = 16;
	private static final long KEY_MIN_INTERVAL_MS = 200L;
    private record QueuedCommand(String text,Integer targetId,UUID targetUuid){}
    private static final class Active {
        final Binding binding;final Integer targetId;final UUID targetUuid;final MacroRepeatState timing;MacroSequence<QueuedCommand> sequence;
        Active(Binding binding,Integer targetId,UUID targetUuid,MacroRepeatState timing){this.binding=binding;this.targetId=targetId;this.targetUuid=targetUuid;this.timing=timing;}
        Binding binding(){return binding;}Integer targetId(){return targetId;}MacroRepeatState timing(){return timing;}
    }
    private static final Map<String, Active> ACTIVE = new java.util.LinkedHashMap<>();
    private static final Map<String, Boolean> heldKeys = new java.util.HashMap<>();
    public static List<String> activeNames() { return List.copyOf(ACTIVE.keySet()); }
    public static void stopAll() { ACTIVE.clear(); }
    private static boolean manual(Binding b) { return b.trigger == BindTrigger.KEY || b.trigger == BindTrigger.RADIUS || b.trigger == BindTrigger.COMMAND; }
    public static boolean usesItemFilter(Binding b){return b.customItem!=null&&!b.customItem.isBlank()||b.customModelData>0||b.item!=null&&!b.item.isBlank()&&!b.item.equalsIgnoreCase("any");}
    private static void start(Binding binding, Entity target, long now) {
        ACTIVE.put(binding.id, new Active(binding, target == null ? null : target.getId(),target==null?null:target.getUUID(),
                new MacroRepeatState(manual(binding) ? binding.keyMode : "PRESS", manual(binding) ? binding.repeatCount : 1, binding.repeatDelayMs, now)));
    }
    public static void tick(Minecraft client, boolean down, boolean clicked) {
        if (client.player == null || client.getConnection() == null || client.screen != null || !client.isWindowActive()) { stopAll(); heldKeys.clear(); return; }
        LocalPlayer player = client.player; ItemStack held = player.getMainHandItem(); String item = itemId(held);
        long now = System.currentTimeMillis(); boolean save = false;
        Map<String, Boolean> inputs = new java.util.HashMap<>();
        for (Binding binding : EnvyWorld.config().binds) {
            if((binding.trigger==BindTrigger.PROJECTILE_HIT||binding.trigger==BindTrigger.ATTACK_BLOCK))continue;
            boolean current = MacroKeys.down(binding, client.getWindow().handle(), down || clicked);
            boolean pressed = current && !heldKeys.getOrDefault(binding.id, false);
            heldKeys.put(binding.id, current); inputs.put(binding.id, current);
            if (!pressed) continue;
            if (!manual(binding)) {
                binding.enabled = !binding.enabled; ACTIVE.remove(binding.id); save = true;
                com.envyworld.client.gui.HudNotices.add(binding.id, binding.enabled ? "Макрос атаки включён" : "Макрос атаки выключен", 0xFFB4CFF1);
            } else if (binding.enabled && (!usesItemFilter(binding)||matchesItem(binding, held, item))) {
                if ("TOGGLE".equals(binding.keyMode) && ACTIVE.remove(binding.id) != null) continue;
                if (!onCooldown(binding, player, now)) start(binding, null, now);
            }
        }
        if (save) EnvyWorld.saveConfig();
        heldKeys.keySet().removeIf(id -> EnvyWorld.config().binds.stream().noneMatch(b -> b.id.equals(id)));
        var iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            Active active = iterator.next(); Binding binding = active.binding();
            if (!binding.enabled || !EnvyWorld.config().binds.contains(binding) || usesItemFilter(binding)&&!matchesItem(binding, held, item)
                    || manual(binding) && "HOLD".equals(binding.keyMode) && !inputs.getOrDefault(binding.id,false)) { iterator.remove(); continue; }
            Entity target=active.targetId()==null?null:player.level().getEntity(active.targetId());
            if(active.targetId()!=null&&(target==null||!target.isAlive()||!target.getUUID().equals(active.targetUuid))){iterator.remove();continue;}
            if(active.sequence==null){
                if(!active.timing().due(now))continue;
                active.sequence=sequence(binding,player,target,item,now);
            }
            active.sequence.tick(now,command->{
                if(command.targetId()!=null){Entity captured=player.level().getEntity(command.targetId());if(captured==null||!captured.isAlive()||!captured.getUUID().equals(command.targetUuid()))return;}
                if(client.getConnection()!=null)client.getConnection().sendCommand(command.text());
            });
            if(active.sequence.done()){active.sequence=null;active.timing().ran(now);if(active.timing().done())iterator.remove();}

        }
    }
    public static Entity lookedPlayer(LocalPlayer player, double distance) {
        double range = Math.max(1, Math.min(128,distance)); Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(range));
        double best = range * range; Entity result = null;
        for (var candidate : player.level().players()) {
            if (candidate == player || !candidate.isAlive() || candidate.isSpectator()) continue;
            var hit = candidate.getBoundingBox().inflate(.12).clip(eye,end);
            if (hit.isEmpty() || eye.distanceToSqr(hit.get()) >= best || !player.hasLineOfSight(candidate)) continue;
            best = eye.distanceToSqr(hit.get()); result = candidate;
        }
        return result;
    }

	private ClientMacros() {
	}

	public static void trigger(BindTrigger trigger, LocalPlayer player, Entity target) {
		if (trigger == BindTrigger.KEY || trigger == BindTrigger.RADIUS || trigger == BindTrigger.COMMAND || player == null || player.level() == null) {
			return;
		}

		ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
		String item = itemId(held);
		long now = System.currentTimeMillis();

		var commands=new java.util.HashSet<String>();
		for (Binding binding : EnvyWorld.config().binds) {
			if (!binding.matchesTrigger(trigger) || !matchesItem(binding, held, item)
					|| onCooldown(binding, player, now)||!commands.add(expand(binding.command,player,target,item))) {
				continue;
			}
			start(binding, target, now);
		}
	}

	/**
	 * Macro key (V by default): runs every {@link BindTrigger#KEY} bind matching the held item.
	 * When the bind has a radius, it runs once per entity in front of the player.
	 *
	 * @return number of executed commands
	 */
	public static int triggerKey(LocalPlayer player) {
		if (player == null || player.level() == null) {
			return 0;
		}

		long now = System.currentTimeMillis();
		Long lastKey = LAST_KEY.put(player.getUUID(), now);
		if (lastKey != null && now - lastKey < KEY_MIN_INTERVAL_MS) {
			return 0;
		}

		ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
		String item = itemId(held);
		int runs = 0;

		for (Binding binding : EnvyWorld.config().binds) {
			if (!binding.matchesTrigger(BindTrigger.KEY) || !matchesItem(binding, held, item)
					|| onCooldown(binding, player, now)) {
				continue;
			}

            start(binding,null,now);runs++;
		}

		return runs;
	}

	/** Entities in a cone in front of the player, sorted by distance. */
	public static List<Entity> areaTargets(LocalPlayer player, double radius) {
		Vec3 look = player.getLookAngle();
		Vec3 eye = player.getEyePosition();
		AABB box = player.getBoundingBox().inflate(radius);

		List<Entity> candidates = player.level().getEntities(player, box,
				entity -> entity instanceof net.minecraft.world.entity.player.Player && entity.isAlive() && !entity.isSpectator() && !entity.isRemoved());
		List<Entity> result = new ArrayList<>();

		for (Entity entity : candidates) {
			Vec3 to = entity.position().add(0.0, entity.getBbHeight() / 2.0, 0.0).subtract(eye);
			double distance = to.length();

			if (distance < 1.0E-4 || distance > radius) {
				continue;
			}

			result.add(entity);
		}

		result.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
		return result.size() > MAX_AREA_TARGETS ? new ArrayList<>(result.subList(0, MAX_AREA_TARGETS)) : result;
	}

    private static MacroSequence<QueuedCommand> sequence(Binding binding,LocalPlayer player,Entity target,String item,long now){
        List<Entity> targets=new ArrayList<>();
        if(binding.trigger==BindTrigger.RADIUS||binding.trigger==BindTrigger.KEY&&binding.radius>0)targets.addAll(areaTargets(player,binding.radius));
        else if(binding.trigger==BindTrigger.KEY){var looked=lookedPlayer(player,binding.lookDistance);if(looked!=null)targets.add(looked);}
        else targets.add(target);
        var steps=new ArrayList<MacroSequence.Step<QueuedCommand>>();var lines=commands(binding.command);
        for(int row=0;row<lines.size();row++){
            String template=lines.get(row);boolean needsPlayer=template.contains("%target%")||template.contains("%target_uuid%")||template.contains("<name+attack>")||template.contains("<name>");
            int delay=binding.commandDelaysMs!=null&&row<binding.commandDelaysMs.size()&&binding.commandDelaysMs.get(row)!=null?binding.commandDelaysMs.get(row):0;
            boolean first=true;
            if(targets.isEmpty()&&!needsPlayer){steps.add(new MacroSequence.Step<>(new QueuedCommand(expand(template,player,null,item,binding.radius),null,null),delay));continue;}
            for(Entity captured:targets){
                if(needsPlayer&&!(captured instanceof net.minecraft.world.entity.player.Player))continue;
                String expanded=expand(template,player,captured,item,binding.radius);
                var command=new QueuedCommand(expanded,captured==null?null:captured.getId(),captured==null?null:captured.getUUID());
                steps.add(new MacroSequence.Step<>(command,first?delay:0));first=false;
            }
        }
        return new MacroSequence<>(steps,now);
    }

    public static java.util.List<String> commands(String text){return java.util.Arrays.stream(text.split("\\n")).map(String::strip).map(s->s.startsWith("/")?s.substring(1):s).filter(s->!s.isBlank()).limit(16).toList();}

	private static boolean matchesItem(Binding binding, ItemStack stack, String item) {
		// The catalog preset owns its base item. A binding may still have the old
		// copied id after its preset was edited from, for example, a stick to a sword.
		if (binding.customItem != null && !binding.customItem.isBlank()) {
			return CustomItems.matchesPreset(stack,binding.customItem);
		}
		if (!binding.item.equalsIgnoreCase("any") && !binding.item.equalsIgnoreCase(item)) {
			return false;
		}
		if (binding.customModelData > 0) {
			return CustomItems.modelOf(stack) == binding.customModelData;
		}
		return true;
	}

	private static boolean onCooldown(Binding binding, LocalPlayer player, long now) {
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

	private static String itemId(ItemStack stack) {
		Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key == null ? "minecraft:air" : key.toString();
	}

	public static String expand(String template, LocalPlayer player, Entity target, String item) {
		return expand(template, player, target, item, 6.0);
	}

	public static String expand(String template, LocalPlayer player, Entity target, String item, double radius) {
		String result = template;

		result = result.replace("%player%", player.getName().getString());
		result = result.replace("%player_uuid%", player.getUUID().toString());
		result = result.replace("%item%", item);
		result = result.replace("%world%", player.level().dimension().identifier().toString());
		result = result.replace("%x%", coordinate(player.getX()));
		result = result.replace("%y%", coordinate(player.getY()));
		result = result.replace("%z%", coordinate(player.getZ()));

		Entity viewTarget = target != null ? target : lookedAt(player, Math.max(4.0, radius));
		Entity nearestTarget = target;
		if (nearestTarget == null) {
			List<Entity> nearest = areaTargets(player, Math.max(1.0, radius));
			nearestTarget = nearest.isEmpty() ? null : nearest.getFirst();
		}

		String targetName = target == null ? "" : target.getName().getString();
		String viewName = viewTarget == null ? "" : viewTarget.getName().getString();
		String nearestName = nearestTarget == null ? "" : nearestTarget.getName().getString();

		// Old EnvyWorld macro placeholders, kept for compatibility with existing presets.
		result = result.replace("<name+attack>", targetName);
		result = result.replace("<name+view>", viewName);
		result = result.replace("<name+look>", viewName);
		result = result.replace("<name+radius>", nearestName);
		result = result.replace("<name>", targetName);

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

		result = result.replace("%view%", viewName);
		result = result.replace("%nearest%", nearestName);
		return result;
	}

	private static Entity lookedAt(LocalPlayer player, double distance) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(distance));
		Entity best = null;
		double bestDistance = distance * distance;

		for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(distance),
				Entity::isAlive)) {
			var hit = entity.getBoundingBox().inflate(0.25).clip(eye, end);
			if (hit.isEmpty()) {
				continue;
			}
			double found = eye.distanceToSqr(hit.get());
			if (found < bestDistance) {
				bestDistance = found;
				best = entity;
			}
		}

		return best;
	}

	private static String coordinate(double value) {
		return String.valueOf((int) Math.floor(value));
	}
}
