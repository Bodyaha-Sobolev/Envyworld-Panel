package com.envyworld.command;

import com.envyworld.EnvyWorld;
import com.envyworld.bind.BindRunner;
import com.envyworld.bind.BindTrigger;
import com.envyworld.bind.Binding;
import com.envyworld.config.RulesConfig;
import com.envyworld.punish.Punishment;
import com.envyworld.punish.PunishmentService;
import com.envyworld.punish.PunishmentType;
import com.envyworld.rules.RuleAction;
import com.envyworld.rules.RuleBook;
import com.envyworld.rules.RulePreset;
import com.envyworld.util.Durations;
import com.envyworld.util.Msg;
import com.envyworld.util.Perms;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public final class RulesCommand {
	private static final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	private static final int MAX_LINES = 50;

	private static final SuggestionProvider<CommandSourceStack> DURATIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(List.of("15m", "30m", "1h", "2h", "3h", "6h", "12h", "1d", "7d", "30d", "perm"), builder);

	private static final SuggestionProvider<CommandSourceStack> RULES = (context, builder) ->
			SharedSuggestionProvider.suggest(RuleBook.codes(), builder);

	private static final SuggestionProvider<CommandSourceStack> BIND_IDS = (context, builder) ->
			SharedSuggestionProvider.suggest(EnvyWorld.config().binds.stream().map(binding -> binding.id).toList(), builder);

	private static final SuggestionProvider<CommandSourceStack> ITEMS = (context, builder) -> {
		builder.suggest("any");
		String remaining = builder.getRemainingLowerCase();

		for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
			String value = id.toString();
			if (remaining.isEmpty() || value.contains(remaining)) {
				builder.suggest(value);
			}
		}

		return builder.buildFuture();
	};

	private RulesCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		RulesConfig config = EnvyWorld.config();

		dispatcher.register(Commands.literal("rules")
				.then(Commands.literal("help").executes(context -> help(context.getSource())))
				.then(Commands.literal("punish")
						.requires(Perms.require("envyworld.punish", config.punishLevel))
						.then(Commands.argument("targets", GameProfileArgument.gameProfile())
								.then(timedBranch("ban", "envyworld.punish", config.punishLevel))
								.then(timedBranch("banip", "envyworld.banip", config.banIpLevel))
								.then(timedBranch("mute", "envyworld.punish", config.punishLevel))
								.then(reasonBranch("warn"))
								.then(reasonBranch("kick"))
								.then(ruleBranch())))
				.then(Commands.literal("unban")
						.requires(Perms.require("envyworld.punish", config.punishLevel))
						.then(Commands.argument("targets", GameProfileArgument.gameProfile())
								.executes(context -> unban(context, "Снято вручную"))
								.then(Commands.argument("reason", StringArgumentType.greedyString())
										.executes(context -> unban(context, reason(context))))))
				.then(Commands.literal("unmute")
						.requires(Perms.require("envyworld.punish", config.punishLevel))
						.then(Commands.argument("targets", GameProfileArgument.gameProfile())
								.executes(context -> unmute(context, "Снято вручную"))
								.then(Commands.argument("reason", StringArgumentType.greedyString())
										.executes(context -> unmute(context, reason(context))))))
				.then(Commands.literal("check")
						.requires(Perms.require("envyworld.punish", config.punishLevel))
						.then(Commands.argument("targets", GameProfileArgument.gameProfile())
								.executes(RulesCommand::check)))
				.then(Commands.literal("list")
						.requires(Perms.require("envyworld.punish", config.punishLevel))
						.executes(context -> list(context.getSource(), "all"))
						.then(Commands.literal("bans").executes(context -> list(context.getSource(), "bans")))
						.then(Commands.literal("mutes").executes(context -> list(context.getSource(), "mutes")))
						.then(Commands.literal("warns").executes(context -> list(context.getSource(), "warns"))))
				.then(Commands.literal("rules").executes(context -> rules(context.getSource())))
				.then(Commands.literal("bind")
						.requires(Perms.require("envyworld.bind", config.bindLevel))
						.then(bindBranch("attack", BindTrigger.ATTACK_ENTITY))
						.then(bindBranch("use", BindTrigger.USE_ITEM))
						.then(keyBindBranch()))
				.then(Commands.literal("unbind")
						.requires(Perms.require("envyworld.bind", config.bindLevel))
						.then(Commands.argument("id", StringArgumentType.word()).suggests(BIND_IDS)
								.executes(context -> unbind(context.getSource(), StringArgumentType.getString(context, "id")))))
				.then(Commands.literal("binds")
						.requires(Perms.require("envyworld.bind", config.bindLevel))
						.executes(context -> binds(context.getSource())))
				.then(Commands.literal("triggerkey")
						.executes(context -> triggerKey(context.getSource())))
				.then(Commands.literal("reload")
						.requires(Perms.require("envyworld.reload", config.reloadLevel))
						.executes(context -> reload(context.getSource()))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> timedBranch(String name, String node, int level) {
		return Commands.literal(name)
				.requires(Perms.require(node, level))
				.then(Commands.argument("duration", StringArgumentType.word()).suggests(DURATIONS)
						.executes(context -> punish(context, name, StringArgumentType.getString(context, "duration"), ""))
						.then(Commands.argument("reason", StringArgumentType.greedyString())
								.executes(context -> punish(context, name, StringArgumentType.getString(context, "duration"), reason(context)))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> reasonBranch(String name) {
		return Commands.literal(name)
				.executes(context -> punish(context, name, "", ""))
				.then(Commands.argument("reason", StringArgumentType.greedyString())
						.executes(context -> punish(context, name, "", reason(context))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> ruleBranch() {
		return Commands.literal("rule")
				.then(Commands.argument("code", StringArgumentType.word()).suggests(RULES)
						.executes(context -> applyRule(context, null, ""))
						.then(Commands.argument("duration", StringArgumentType.word()).suggests(DURATIONS)
								.executes(context -> applyRule(context, StringArgumentType.getString(context, "duration"), ""))
								.then(Commands.argument("reason", StringArgumentType.greedyString())
										.executes(context -> applyRule(context, StringArgumentType.getString(context, "duration"), reason(context))))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> bindBranch(String name, BindTrigger trigger) {
		return Commands.literal(name)
				.then(Commands.argument("item", StringArgumentType.word()).suggests(ITEMS)
						.then(Commands.argument("command", StringArgumentType.greedyString())
								.executes(context -> createBind(context.getSource(), trigger,
										StringArgumentType.getString(context, "item"), 0,
										StringArgumentType.getString(context, "command")))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> keyBindBranch() {
		return Commands.literal("key")
				.then(Commands.argument("item", StringArgumentType.word()).suggests(ITEMS)
						.then(Commands.argument("radius", IntegerArgumentType.integer(0, 64))
								.then(Commands.argument("command", StringArgumentType.greedyString())
										.executes(context -> createBind(context.getSource(), BindTrigger.KEY,
												StringArgumentType.getString(context, "item"),
												IntegerArgumentType.getInteger(context, "radius"),
												StringArgumentType.getString(context, "command"))))));
	}

	private static int punish(CommandContext<CommandSourceStack> context, String action, String durationRaw, String reasonRaw) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(context, "targets");

		long duration = 0L;
		if (action.equals("ban") || action.equals("banip") || action.equals("mute")) {
			duration = parseDuration(durationRaw);
		}

		String reason = normalizeReason(source, reasonRaw);
		int count = apply(source, targets, action, duration, reason);
		Msg.ok(source, "Наказание выдано: " + count + " игрок(ов)");
		return count;
	}

	private static int applyRule(CommandContext<CommandSourceStack> context, String durationOverride, String reasonRaw) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(context, "targets");

		String code = StringArgumentType.getString(context, "code");
		RulePreset preset = RuleBook.get(code);

		if (preset == null) {
			throw error("Правило " + code + " не найдено. Список правил: /rules rules");
		}
		if (preset.action == RuleAction.NOTE) {
			throw error("[" + preset.code + "] " + preset.title + " — " + (preset.note.isBlank()
					? "наказание выдаётся вручную" : preset.note));
		}

		long duration;
		if (durationOverride != null && !durationOverride.isBlank() && !durationOverride.equals("-")) {
			duration = parseDuration(durationOverride);
			checkRange(source, preset, duration);
		} else {
			duration = preset.duration == null || preset.duration.isBlank() ? 0L : parseDuration(preset.duration);
		}

		String reason = reasonRaw == null || reasonRaw.isBlank()
				? "[" + preset.code + "] " + preset.title
				: normalizeReason(source, reasonRaw);

		if (!preset.note.isBlank()) {
			Msg.warn(source, "Примечание: " + preset.note);
		}

		String action = switch (preset.action) {
			case MUTE -> "mute";
			case BAN, BANIP -> "ban";
			case WARN -> "warn";
			case KICK -> "kick";
			default -> "ban";
		};

		int count;
		if (preset.action == RuleAction.BANIP) {
			count = apply(source, targets, "banip", duration, reason);
		} else {
			count = apply(source, targets, action, duration, reason);
		}
		Msg.ok(source, "Наказание по правилу [" + preset.code + "] выдано: " + count + " игрок(ов)");
		return count;
	}

	private static int apply(CommandSourceStack source, Collection<NameAndId> targets, String action, long duration, String reason) throws CommandSyntaxException {
		int count = 0;

		for (NameAndId target : targets) {
			switch (action) {
				case "ban" -> PunishmentService.ban(source, target, duration, reason);
				case "banip" -> PunishmentService.banIp(source, target, duration, reason);
				case "mute" -> PunishmentService.mute(source, target, duration, reason);
				case "warn" -> PunishmentService.warn(source, target, reason);
				case "kick" -> PunishmentService.kick(source, target, reason);
				default -> throw new IllegalStateException("Unknown action " + action);
			}
			count++;
		}

		return count;
	}

	private static int unban(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		int count = 0;

		for (NameAndId target : GameProfileArgument.getGameProfiles(context, "targets")) {
			if (PunishmentService.unban(source, target, reason)) {
				Msg.ok(source, "Бан снят с " + target.name());
				count++;
			} else {
				Msg.warn(source, target.name() + " не забанен");
			}
		}

		return count;
	}

	private static int unmute(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		int count = 0;

		for (NameAndId target : GameProfileArgument.getGameProfiles(context, "targets")) {
			if (PunishmentService.unmute(source, target, reason)) {
				Msg.ok(source, "Мут снят с " + target.name());
				count++;
			} else {
				Msg.warn(source, target.name() + " не имеет активного мута");
			}
		}

		return count;
	}

	private static int check(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();

		for (NameAndId target : GameProfileArgument.getGameProfiles(context, "targets")) {
			Msg.raw(source, Component.literal("— " + target.name() + " —").withStyle(ChatFormatting.GOLD));

			UserBanListEntry entry = source.getServer().getPlayerList().getBans().get(target);
			if (entry != null && (entry.getExpires() == null || entry.getExpires().after(new Date()))) {
				String expires = entry.getExpires() == null ? "навсегда" : DATE.format(entry.getExpires());
				Msg.raw(source, line("Бан", expires + " · " + entry.getReason() + " · выдал " + entry.getSource(), ChatFormatting.RED));
			}

			Punishment mute = EnvyWorld.punishments().active(target.id(), PunishmentType.MUTE);
			if (mute != null) {
				Msg.raw(source, line("Мут", Durations.remaining(mute.expiresAt) + " · " + mute.reason + " · выдал " + mute.issuedBy, ChatFormatting.YELLOW));
			}

			List<Punishment> history = EnvyWorld.punishments().history(target.id());
			long warns = history.stream().filter(punishment -> punishment.type == PunishmentType.WARN).count();

			if (warns > 0) {
				String last = history.stream()
						.filter(punishment -> punishment.type == PunishmentType.WARN)
						.limit(3)
						.map(punishment -> punishment.reason + " (" + punishment.issuedBy + ")")
						.reduce((a, b) -> a + "; " + b)
						.orElse("");
				Msg.raw(source, line("Варны", warns + " · " + last, ChatFormatting.GOLD));
			}

			if (entry == null && mute == null && warns == 0) {
				Msg.info(source, "Активных наказаний нет");
			}
		}

		return 1;
	}

	private static int list(CommandSourceStack source, String filter) {
		int shown = 0;

		if (!filter.equals("mutes") && !filter.equals("warns")) {
			Msg.raw(source, Component.literal("— Баны —").withStyle(ChatFormatting.GOLD));
			for (UserBanListEntry entry : source.getServer().getPlayerList().getBans().getEntries()) {
				if (entry.getExpires() != null && entry.getExpires().before(new Date())) {
					continue;
				}
				if (shown++ >= MAX_LINES) {
					break;
				}
				String expires = entry.getExpires() == null ? "навсегда" : DATE.format(entry.getExpires());
				Msg.info(source, entry.getUser().name() + " · " + expires + " · " + entry.getReason());
			}
		}

		if (!filter.equals("bans") && !filter.equals("warns")) {
			Msg.raw(source, Component.literal("— Муты —").withStyle(ChatFormatting.GOLD));
			for (Punishment punishment : EnvyWorld.punishments().active(PunishmentType.MUTE)) {
				if (shown++ >= MAX_LINES) {
					break;
				}
				Msg.info(source, punishment.cleanName() + " · " + Durations.remaining(punishment.expiresAt) + " · " + punishment.reason);
			}
		}

		if (!filter.equals("bans") && !filter.equals("mutes")) {
			List<Punishment> warns = EnvyWorld.punishments().active(PunishmentType.WARN);
			Msg.raw(source, Component.literal("— Варны (" + warns.size() + ") —").withStyle(ChatFormatting.GOLD));
			for (Punishment punishment : warns) {
				if (shown++ >= MAX_LINES) {
					break;
				}
				Msg.info(source, punishment.cleanName() + " · " + punishment.reason);
			}
		}

		if (shown == 0) {
			Msg.info(source, "Записей нет");
		}
		return shown;
	}

	private static int rules(CommandSourceStack source) {
		Msg.raw(source, Component.literal("— Правила EnvyWorld —").withStyle(ChatFormatting.GOLD));

		for (RulePreset preset : RuleBook.all()) {
			String suffix = preset.action == RuleAction.NOTE
					? "вручную"
					: preset.action + (preset.duration.isBlank() ? "" : " " + preset.duration);
			Msg.raw(source, Component.literal("[" + preset.code + "] ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.literal(suffix + " · ").withStyle(ChatFormatting.AQUA))
					.append(Component.literal(preset.title).withStyle(ChatFormatting.GRAY)));
		}

		return RuleBook.all().size();
	}

	private static int createBind(CommandSourceStack source, BindTrigger trigger, String rawItem, int radius, String rawCommand) throws CommandSyntaxException {
		String item = normalizeItem(rawItem);
		String command = rawCommand.trim();

		if (command.startsWith("/")) {
			command = command.substring(1);
		}
		if (command.isBlank()) {
			throw error("Команда не может быть пустой");
		}

		Binding binding = new Binding(nextBindId(), trigger, item, command);
		binding.radius = Math.max(0, radius);
		EnvyWorld.config().binds.add(binding);
		EnvyWorld.saveConfig();

		Msg.ok(source, "Привязка " + binding.id + " создана: " + trigger.id() + " · " + item
				+ (radius > 0 ? " · радиус " + radius : ""));
		Msg.info(source, "Плейсхолдеры: %player%, %target%, %item%, %world%, %x%, %y%, %z%");
		return 1;
	}

	private static int triggerKey(CommandSourceStack source) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			int runs = BindRunner.triggerKey(player);
			if (runs > 0) {
				Msg.info(source, "Выполнено макросов: " + runs);
			}
			return runs;
		} catch (CommandSyntaxException exception) {
			Msg.error(source, "Команда доступна только игроку");
			return 0;
		}
	}

	private static int unbind(CommandSourceStack source, String id) {
		Binding binding = EnvyWorld.config().bindById(id);

		if (binding == null) {
			Msg.error(source, "Привязка " + id + " не найдена");
			return 0;
		}

		EnvyWorld.config().binds.remove(binding);
		EnvyWorld.saveConfig();
		Msg.ok(source, "Привязка " + binding.id + " удалена");
		return 1;
	}

	private static int binds(CommandSourceStack source) {
		List<Binding> bindings = EnvyWorld.config().sortedBinds();
		Msg.raw(source, Component.literal("— Привязки (" + bindings.size() + ") —").withStyle(ChatFormatting.GOLD));

		for (Binding binding : bindings) {
			String command = binding.command.length() > 48 ? binding.command.substring(0, 45) + "..." : binding.command;
			Msg.raw(source, Component.literal(binding.id + " ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.literal(binding.trigger.id() + " · " + binding.item
									+ (binding.radius > 0 ? " · r" + binding.radius : "") + " · ")
							.withStyle(binding.enabled ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY))
					.append(Component.literal(command).withStyle(ChatFormatting.GRAY)));
		}

		if (bindings.isEmpty()) {
			Msg.info(source, "Привязок нет");
		}
		return bindings.size();
	}

	private static int reload(CommandSourceStack source) {
		EnvyWorld.reload();
		RuleBook.load(EnvyWorld.LOGGER);
		Msg.ok(source, "Конфиг перечитан: " + EnvyWorld.config().binds.size() + " привязок");
		return 1;
	}

	private static int help(CommandSourceStack source) {
		Msg.raw(source, Component.literal("— EnvyWorld: правила и наказания —").withStyle(ChatFormatting.GOLD));
		Msg.info(source, "/rules punish <ник> <ban|banip|mute> <срок> [причина]");
		Msg.info(source, "/rules punish <ник> <warn|kick> [причина]");
		Msg.info(source, "/rules punish <ник> rule <пункт> [срок|-] [причина]");
		Msg.info(source, "/rules unban <ник> [причина] · /rules unmute <ник> [причина]");
		Msg.info(source, "/rules check <ник> · /rules list [bans|mutes|warns] · /rules rules");
		Msg.info(source, "/rules bind <attack|break|use> <предмет|any> <команда>");
		Msg.info(source, "/rules bind key <предмет|any> <радиус> <команда> — макрос на клавишу V");
		Msg.info(source, "/rules binds · /rules unbind <id> · /rules reload");
		Msg.info(source, "Сроки: 30m, 2h, 6h, 7d, 30d, perm (навсегда)");
		return 1;
	}

	private static String normalizeReason(CommandSourceStack source, String raw) throws CommandSyntaxException {
		String reason = raw == null ? "" : raw.trim();

		if (reason.isBlank()) {
			if (EnvyWorld.config().requireReason) {
				throw error("Укажите причину — по правилу 3.10 она должна быть развёрнутой");
			}
			return "Причина не указана";
		}

		if (EnvyWorld.config().minReasonLength > 0 && reason.length() < EnvyWorld.config().minReasonLength) {
			Msg.warn(source, "Причина короткая — по правилу 3.10 она должна быть понятной игрокам");
		}

		return reason;
	}

	private static long parseDuration(String raw) throws CommandSyntaxException {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		if (raw.equalsIgnoreCase("perm") || raw.equalsIgnoreCase("permanent")) {
			return 0L;
		}

		try {
			return Durations.parse(raw);
		} catch (IllegalArgumentException exception) {
			throw error("Неверный срок: " + raw + ". Пример: 30m, 6h, 7d, perm");
		}
	}

	private static void checkRange(CommandSourceStack source, RulePreset preset, long duration) {
		if (duration <= 0L) {
			return;
		}

		try {
			if (!preset.min.isBlank() && duration < Durations.parse(preset.min)) {
				Msg.warn(source, "Срок меньше указанного в правиле минимума (" + preset.min + ")");
			}
			if (!preset.max.isBlank() && duration > Durations.parse(preset.max)) {
				Msg.warn(source, "Срок больше указанного в правиле максимума (" + preset.max + ")");
			}
		} catch (IllegalArgumentException ignored) {
			// Broken preset data should never block moderation.
		}
	}

	private static String normalizeItem(String raw) throws CommandSyntaxException {
		String item = raw.trim().toLowerCase();

		if (item.equals("any")) {
			return "any";
		}
		if (!item.contains(":")) {
			item = "minecraft:" + item;
		}

		Identifier id = Identifier.tryParse(item);
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			throw error("Неизвестный предмет: " + raw);
		}
		return id.toString();
	}

	private static String nextBindId() {
		int next = 1;

		for (Binding binding : EnvyWorld.config().binds) {
			if (binding.id.startsWith("bind")) {
				try {
					next = Math.max(next, Integer.parseInt(binding.id.substring(4)) + 1);
				} catch (NumberFormatException ignored) {
					// Custom ids are allowed.
				}
			}
		}

		return "bind" + next;
	}

	private static String reason(CommandContext<CommandSourceStack> context) {
		return StringArgumentType.getString(context, "reason");
	}

	private static Component line(String label, String value, ChatFormatting color) {
		return Component.literal("» ").withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal(label + ": ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(value).withStyle(color));
	}

	private static CommandSyntaxException error(String text) {
		return new SimpleCommandExceptionType(Msg.error(text)).create();
	}
}
