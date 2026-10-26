package com.envyworld.logs;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Punishment log facade. Query caches never replace each other's results. */
public final class PunishLogClient {
    public static final String[] TYPES = {"bans", "mutes", "warnings"};
    public static final String[] TYPE_NAMES = {"Баны", "Муты", "Предупреждения"};
    public record Snapshot(List<Row> rows, String state, String error, long ageMs, boolean limited) {}
    private static volatile String lastQuery = "", lastServer = "all";
    private PunishLogClient() {}
	public static final String[] SERVERS = {
			"all",
			"Анархия #1",
			"БедВарс",
			"Выживание #1",
			"Выживание #2",
			"Выживание #3",
			"Выживание #4",
			"Выживание #5",
			"ОдинБлок",
			"ПвП",
			"СкайВарс",
			"Хаб"
	};
	public record Row(String type, String punished, String punisher, String reason,
					  String date, String until, String server) {
		public String typeLabel() {
			return switch (type == null ? "" : type) {
				case "mutes" -> "Мут";
				case "warnings" -> "Варн";
				default -> "Бан";
			};
		}
	}

    public static Snapshot snapshot(String query, String server) { return LogQueryCache.snapshot(query, server); }
    public static CompletableFuture<Snapshot> request(String query, String server) { return LogQueryCache.request(query, server, false); }
    public static void refreshAll(String query, String server) { lastQuery = query; lastServer = server; LogQueryCache.request(query, server, true); }
    public static void tickAll(String query, String server) { LogQueryCache.request(query, server, false); }
    public static List<Row> rows() { return snapshot(lastQuery, lastServer).rows(); }
    public static String state() { return snapshot(lastQuery, lastServer).state(); }
    public static String error() { return snapshot(lastQuery, lastServer).error(); }
    public static int total() { return rows().size(); }
    public static boolean limited() { return snapshot(lastQuery, lastServer).limited(); }
    public static long ageMs() { return snapshot(lastQuery, lastServer).ageMs(); }
    public static String lastQuery() { return lastQuery; }
    public static String lastServer() { return lastServer; }
    public static CompletableFuture<List<Row>> fetchPlayerHistory(String nick) {
        if (nick == null || nick.isBlank()) return CompletableFuture.completedFuture(List.of());
        return request(nick, "all").thenApply(result -> {
            if (result.state().equals("error")) throw new IllegalStateException(result.error());
            return result.rows().stream().filter(row -> row.punished().equalsIgnoreCase(nick.trim())
                    || row.punisher().equalsIgnoreCase(nick.trim())).toList();
        });
    }
    public static String typeName(String type) {
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(type)) return TYPE_NAMES[i];
        return type;
    }
	static long sortTimestamp(String value) {
		return timestamp(value, true);
	}

	/**
	 * Accepts Russian labels ("сегодня", "вчера", "23 октября"), ISO
	 * ("2025-10-23 14:32", "2025-10-23T14:32:10"), dotted dates
	 * ("23.10.2025 14:32"), and short month names ("23 окт 14:32").
	 */
	private static java.time.LocalDateTime parseDateTime(String text) {
		return parseDateTime(text, java.time.LocalDate.now(java.time.ZoneId.of("Europe/Moscow")), true);
	}

    static long expiryTimestamp(String text) {
        return timestamp(text, false);
    }

    private static long timestamp(String input, boolean past) {
        try {
            String text = input == null ? "" : input.trim();
            if (text.matches("\\d{9,13}")) {
                long epoch = Long.parseLong(text);
                return epoch > 1_000_000_000_000L ? epoch / 1000 : epoch;
            }
            if (text.matches("\\d{4}-.*(?:[zZ]|[+-]\\d{2}:\\d{2})"))
                return java.time.OffsetDateTime.parse(text.replace(' ', 'T').toUpperCase(Locale.ROOT)).toEpochSecond();
            var value = parseDateTime(text.toLowerCase(Locale.ROOT),
                    java.time.LocalDate.now(java.time.ZoneId.of("Europe/Moscow")), past);
            return value == null ? Long.MIN_VALUE : value.atZone(java.time.ZoneId.of("Europe/Moscow")).toEpochSecond();
        } catch (java.time.DateTimeException | NumberFormatException invalid) { return Long.MIN_VALUE; }
    }

    private static java.time.LocalDateTime parseDateTime(String text, java.time.LocalDate today, boolean past) {
		if (text.isEmpty()) {
			return null;
		}
		// Pure epoch (seconds or millis).
		if (text.matches("\\d{9,13}")) {
			long v = Long.parseLong(text);
			if (v > 1_000_000_000_000L) v /= 1000L;
			return java.time.LocalDateTime.ofEpochSecond(v, 0,
					java.time.ZoneOffset.systemDefault().getRules()
							.getOffset(java.time.Instant.now()));
		}
		// ISO: 2025-10-23[ T]14:32[:ss][Z]
		java.util.regex.Matcher iso = java.util.regex.Pattern
				.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})[tT\\s]+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?")
				.matcher(text);
		if (iso.find()) {
			return java.time.LocalDateTime.of(
					Integer.parseInt(iso.group(1)),
					Integer.parseInt(iso.group(2)),
					Integer.parseInt(iso.group(3)),
					Integer.parseInt(iso.group(4)),
					Integer.parseInt(iso.group(5)), iso.group(6) == null ? 0 : Integer.parseInt(iso.group(6)));
		}
		// Dotted: 23.10.2025 14:32 or 23/10/2025 14:32
		java.util.regex.Matcher dotted = java.util.regex.Pattern
				.compile("(\\d{1,2})[./](\\d{1,2})[./](\\d{4})\\D+(\\d{1,2}):(\\d{2})")
				.matcher(text);
		if (dotted.find()) {
			return java.time.LocalDateTime.of(
					Integer.parseInt(dotted.group(3)),
					Integer.parseInt(dotted.group(2)),
					Integer.parseInt(dotted.group(1)),
					Integer.parseInt(dotted.group(4)),
					Integer.parseInt(dotted.group(5)));
		}
		// Russian: "сегодня в 14:32", "вчера в 14:32", "23 октября в 14:32"
		java.util.regex.Matcher time = java.util.regex.Pattern
				.compile("(\\d{1,2}):(\\d{2})").matcher(text);
		if (!time.find()) {
			return null;
		}
		int hour = Integer.parseInt(time.group(1));
		int minute = Integer.parseInt(time.group(2));
		java.time.LocalDate date = today;
		// "позавчера" contains the substring "вчера" — check it first.
		if (text.contains("позавчера")) {
			date = today.minusDays(2);
		} else if (text.contains("вчера")) {
			date = today.minusDays(1);
		} else if (text.contains("послезавтра")) {
			date = today.plusDays(2);
		} else if (text.contains("завтра")) {
			date = today.plusDays(1);
		} else if (!text.contains("сегодня")) {
			java.util.regex.Matcher dateMatcher = java.util.regex.Pattern
					.compile("(\\d{1,2})\\s+([а-яА-ЯёЁ]+)(?:\\s+(\\d{4}))?").matcher(text);
			if (dateMatcher.find()) {
				String monthWord = dateMatcher.group(2).toLowerCase(Locale.ROOT);
				int month = switch (monthWord) {
					case "января", "январь", "янв" -> 1;
					case "февраля", "февраль", "фев" -> 2;
					case "марта", "март", "мар" -> 3;
					case "апреля", "апрель", "апр" -> 4;
					case "мая", "май" -> 5;
					case "июня", "июнь", "июн" -> 6;
					case "июля", "июль", "июл" -> 7;
					case "августа", "август", "авг" -> 8;
					case "сентября", "сентябрь", "сен", "сент" -> 9;
					case "октября", "октябрь", "окт" -> 10;
					case "ноября", "ноябрь", "ноя", "нояб" -> 11;
					case "декабря", "декабрь", "дек" -> 12;
					default -> -1;
				};
				if (month > 0) {
					var trailingYear=java.util.regex.Pattern.compile("\\b(20\\d{2})\\s*(?:года|год|г\\.?)?\\s*$").matcher(text);
                    boolean explicitYear = dateMatcher.group(3) != null || trailingYear.find();
                    int year = dateMatcher.group(3)!=null?Integer.parseInt(dateMatcher.group(3)):explicitYear?Integer.parseInt(trailingYear.group(1)):today.getYear();
					int day = Math.max(1, Math.min(java.time.YearMonth.of(year, month).lengthOfMonth(), Integer.parseInt(dateMatcher.group(1))));
					java.time.LocalDate candidate = java.time.LocalDate.of(year, month, day);
					// A date in the future is probably from last year.
					if (!explicitYear && past && candidate.isAfter(today)) {
						candidate = candidate.minusYears(1);
					}
					if (!explicitYear && !past && candidate.isBefore(today.minusDays(180))) candidate = candidate.plusYears(1);
					date = candidate;
				}
			}
		}
		return java.time.LocalDateTime.of(date, java.time.LocalTime.of(hour, minute));
	}

}
