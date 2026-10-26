package com.envyworld.util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Msg {
	public static final String PREFIX_TEXT = "Envy";

	private Msg() {
	}

	public static MutableComponent prefixed(Component body) {
		return Component.literal(PREFIX_TEXT).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
				.append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY))
				.append(body);
	}

	public static Component info(String text) {
		return prefixed(Component.literal(text).withStyle(ChatFormatting.GRAY));
	}

	public static Component ok(String text) {
		return prefixed(Component.literal(text).withStyle(ChatFormatting.GREEN));
	}

	public static Component error(String text) {
		return prefixed(Component.literal(text).withStyle(ChatFormatting.RED));
	}

	public static Component warn(String text) {
		return prefixed(Component.literal(text).withStyle(ChatFormatting.YELLOW));
	}

	public static Component accent(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GOLD);
	}

	public static void ok(CommandSourceStack source, String text) {
		source.sendSuccess(() -> ok(text), false);
	}

	public static void info(CommandSourceStack source, String text) {
		source.sendSuccess(() -> info(text), false);
	}

	public static void error(CommandSourceStack source, String text) {
		source.sendFailure(error(text));
	}

	public static void warn(CommandSourceStack source, String text) {
		source.sendSuccess(() -> warn(text), false);
	}

	public static void raw(CommandSourceStack source, Component component) {
		source.sendSuccess(() -> component, false);
	}
}
