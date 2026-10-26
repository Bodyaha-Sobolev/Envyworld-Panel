package com.envyworld.logs;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Resolve relative API dates once; advance the remaining time locally with a monotonic clock. */
public final class PunishmentClock {
    public record Status(String label, int color) {}
    private record Timer(String fixed, long remaining, long started, long end) {}
    private static final Map<PunishLogClient.Row, Timer> timers = new LinkedHashMap<>();
    private PunishmentClock() {}

    public static synchronized void remember(PunishLogClient.Row row) {
        String until = row.until().toLowerCase(Locale.ROOT);
        String fixed = until.contains("снято") || until.contains("снят ") || until.contains("removed") ? "Снято"
                : until.contains("истек") || until.contains("истёк") || until.contains("expired") ? "Истекло"
                : until.contains("навсегда") || until.contains("никогда") || until.contains("перманент")
                    || until.contains("permanent") || until.contains("бессроч") ? "Навсегда" : "";
        String date = until.split("\\s*\\|\\s*|\\(", 2)[0].trim();
        long end = fixed.isEmpty() ? PunishLogClient.expiryTimestamp(date) : Long.MIN_VALUE;
        long remaining = end == Long.MIN_VALUE ? Long.MIN_VALUE : end - System.currentTimeMillis() / 1000;
        if (fixed.isEmpty() && end == Long.MIN_VALUE) fixed = until.isBlank() ? "Без срока" : "Срок: " + row.until();
        timers.put(row, new Timer(fixed, remaining, System.nanoTime(),end));
        while (timers.size() > 30_000) timers.remove(timers.keySet().iterator().next());
    }

    public static synchronized Status status(PunishLogClient.Row row) {
        if (!timers.containsKey(row)) remember(row);
        Timer timer = timers.get(row);
        if (!timer.fixed().isEmpty()) return new Status(timer.fixed(), switch (timer.fixed()) {
            case "Снято" -> 0xFF87D9B0; case "Истекло", "Без срока" -> 0xFF929BAA;
            case "Навсегда" -> 0xFFF3A2AF; default -> 0xFFB6C7DB;
        });
        long left = timer.remaining() - (System.nanoTime() - timer.started()) / 1_000_000_000L;
        if (left <= 0) return new Status("Истекло", 0xFF929BAA);
        return new Status("Осталось " + format(left), left <= 300 ? 0xFFFF7C83 : 0xFFFFB66E);
    }

    private static String format(long seconds) {
        long days = seconds / 86400, hours = seconds / 3600 % 24, minutes = seconds / 60 % 60;
        return (days > 0 ? days + "д " : "") + (days > 0 || hours > 0 ? hours + "ч " : "")
                + minutes + "м " + seconds % 60 + "с";
    }
    /** Unknown/missing expiry is not evidence that a historical punishment is still active. */
    public static boolean active(PunishLogClient.Row row) {
        String label=status(row).label();
        return label.equals("Навсегда") || label.startsWith("Осталось ");
    }
    public static synchronized String deadline(PunishLogClient.Row row){
        if(!timers.containsKey(row))remember(row);
        var timer=timers.get(row);
        if(timer.fixed().equals("Навсегда"))return "Без окончания";
        if(timer.end()==Long.MIN_VALUE)return "";
        return "До "+java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm",Locale.ROOT)
            .withZone(java.time.ZoneId.of("Europe/Moscow")).format(java.time.Instant.ofEpochSecond(timer.end()))+" МСК";
    }
}
