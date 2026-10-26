package com.envyworld.logs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Detects new API events, not edits, removals, an initial snapshot or older backfill. */
public final class NewLogTracker {
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final Map<String, Long> newest = new HashMap<>();
    public synchronized void reset() { counts.clear(); newest.clear(); }
    public synchronized List<PunishLogClient.Row> accept(String type, List<PunishLogClient.Row> rows, long nowSeconds) {
        boolean baseline = !newest.containsKey(type);
        long previousNewest = newest.getOrDefault(type, Long.MIN_VALUE), high = previousNewest;
        Map<String, List<PunishLogClient.Row>> groups = new LinkedHashMap<>();
        for (var row : rows) {
            long date = PunishLogClient.sortTimestamp(row.date());
            high = Math.max(high, date);
            String group = type + '|' + row.punished().toLowerCase(java.util.Locale.ROOT) + '|'
                    + row.punisher().toLowerCase(java.util.Locale.ROOT) + '|' + row.server() + '|' + date;
            groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(row);
        }
        List<PunishLogClient.Row> result = new ArrayList<>();
        for (var group : groups.entrySet()) {
            int old = counts.getOrDefault(group.getKey(), 0), size = group.getValue().size();
            long date = PunishLogClient.sortTimestamp(group.getValue().getFirst().date());
            if (!baseline && date >= previousNewest && date >= nowSeconds - 300 && date <= nowSeconds + 60)
                for (int i = old; i < size; i++) result.add(group.getValue().get(i));
            counts.put(group.getKey(), Math.max(old, size));
        }
        newest.put(type, high);
        while (counts.size() > 50_000) counts.remove(counts.keySet().iterator().next());
        return result;
    }
}
