package com.envyworld.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses human friendly durations like {@code 30s}, {@code 10m}, {@code 2h30m}, {@code 7d}, {@code perm}.
 */
public final class Durations {
	private static final Pattern PART = Pattern.compile("(\\d+)([smhdw])");

	/** Value used for "forever" punishments. */
	public static final long PERMANENT = 0L;

	private Durations() {
	}

	/**
	 * @return duration in milliseconds, or {@link #PERMANENT} for permanent punishments
	 * @throws IllegalArgumentException when the input cannot be parsed
	 */
	public static long parse(String input) {
		if (input == null || input.isBlank()) {
			throw new IllegalArgumentException("empty duration");
		}

		String value = input.trim().toLowerCase(Locale.ROOT);

		if (value.equals("perm") || value.equals("permanent") || value.equals("forever") || value.equals("never")) {
			return PERMANENT;
		}

		// Bare number is treated as minutes.
		if (value.chars().allMatch(Character::isDigit)) {
			long minutes = Long.parseLong(value);
			return minutes <= 0 ? PERMANENT : minutes * 60_000L;
		}

		Matcher matcher = PART.matcher(value);
		long total = 0L;
		int matched = 0;

		while (matcher.find()) {
			long amount = Long.parseLong(matcher.group(1));
			char unit = matcher.group(2).charAt(0);

			total += switch (unit) {
				case 's' -> amount * 1_000L;
				case 'm' -> amount * 60_000L;
				case 'h' -> amount * 3_600_000L;
				case 'd' -> amount * 86_400_000L;
				case 'w' -> amount * 604_800_000L;
				default -> 0L;
			};
			matched += matcher.group().length();
		}

		if (matched != value.length() || total <= 0L) {
			throw new IllegalArgumentException("bad duration: " + input);
		}

		return total;
	}

	/** Formats a duration in milliseconds as {@code 1d 2h 30m}. */
	public static String format(long millis) {
		if (millis == PERMANENT) {
			return "навсегда";
		}

		long seconds = Math.max(1L, millis / 1000L);
		long days = seconds / 86_400L;
		long hours = (seconds % 86_400L) / 3_600L;
		long minutes = (seconds % 3_600L) / 60L;
		long secs = seconds % 60L;

		StringBuilder builder = new StringBuilder();
		if (days > 0) {
			builder.append(days).append("д ");
		}
		if (hours > 0) {
			builder.append(hours).append("ч ");
		}
		if (minutes > 0) {
			builder.append(minutes).append("м ");
		}
		if (builder.isEmpty() || (secs > 0 && days == 0)) {
			builder.append(secs).append("с ");
		}

		return builder.toString().trim();
	}

	/** Human readable "expires in" text for a punishment timestamp (0 = permanent). */
	public static String remaining(long expiresAt) {
		if (expiresAt <= 0L) {
			return format(PERMANENT);
		}

		long left = expiresAt - System.currentTimeMillis();
		return left <= 0L ? "истекло" : format(left);
	}
}
