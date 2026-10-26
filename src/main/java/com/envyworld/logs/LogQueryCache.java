package com.envyworld.logs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Immediate local search with independent, incrementally published remote query caches. */
final class LogQueryCache {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final String ENDPOINT = "https://api.dehasher.net/site/logs/envyworld/punishments";
    private static final long TTL = 10_000;
    private static final Map<Key, Entry> queries = new LinkedHashMap<>(64, .75f, true);
    private static final LinkedHashMap<String, Indexed> archive = new LinkedHashMap<>();
    private static long revision;

    record Key(String query, String server) {
        Key {
            query = query == null ? "" : query.trim();
            server = server == null || server.isBlank() ? "all" : server;
        }
    }
    private record Indexed(String id, PunishLogClient.Row row, String search, long timestamp) {}
    private record Batch(String type, List<PunishLogClient.Row> rows, boolean limited) {}
    private static final class Entry {
        String state = "idle", error = "";
        long fetchedAt, memoRevision = -1;
        boolean limited;
        int pending, successes;
        List<PunishLogClient.Row> memo = List.of();
        final Map<String, List<Indexed>> results = new LinkedHashMap<>();
        final List<String> errors = new ArrayList<>();
        CompletableFuture<PunishLogClient.Snapshot> inflight;
    }

    static synchronized PunishLogClient.Snapshot snapshot(String query, String server) {
        Key key = new Key(query, server);
        Entry entry = entry(key);
        if (entry.memoRevision != revision) {
            // Shared rows give immediate search feedback only until that query's
            // category has its own response. Other server queries cannot mutate it.
            entry.memo = java.util.stream.Stream.concat(
                            archive.values().stream().filter(item -> !entry.results.containsKey(item.row().type())),
                            entry.results.values().stream().flatMap(List::stream))
                    .filter(item -> matches(item, key))
                    .sorted(Comparator.comparingLong(Indexed::timestamp).reversed().thenComparing(Indexed::id)).map(Indexed::row).toList();
            entry.memoRevision = revision;
        }
        String state = entry.state.equals("idle") && !entry.memo.isEmpty() ? "cached" : entry.state;
        long age = entry.fetchedAt == 0 ? -1 : System.currentTimeMillis() - entry.fetchedAt;
        return new PunishLogClient.Snapshot(entry.memo, state, entry.error, age, entry.limited);
    }

    static synchronized CompletableFuture<PunishLogClient.Snapshot> request(String query, String server, boolean force) {
        Key key = new Key(query, server);
        Entry entry = entry(key);
        if (entry.inflight != null) return entry.inflight;
        if (!force && entry.fetchedAt > 0 && System.currentTimeMillis() - entry.fetchedAt < TTL) {
            return CompletableFuture.completedFuture(snapshot(query, server));
        }
        entry.state = "loading";
        entry.error = "";
        entry.limited = false;
        entry.pending = PunishLogClient.TYPES.length;
        entry.successes = 0;
        entry.errors.clear();
        CompletableFuture<PunishLogClient.Snapshot> result = new CompletableFuture<>();
        entry.inflight = result;
        for (String type : PunishLogClient.TYPES) {
            fetch(type, key).whenComplete((batch, failure) -> complete(key, entry, batch, failure, result));
        }
        return result;
    }

    private static void complete(Key key, Entry entry, Batch batch, Throwable failure,
                                 CompletableFuture<PunishLogClient.Snapshot> result) {
        PunishLogClient.Snapshot completed = null;
        // Parse date labels and build the search index off the UI lock, once per row.
        List<Indexed> indexed = failure == null ? batch.rows().stream().map(row -> {
            PunishmentClock.remember(row);
            long timestamp = PunishLogClient.sortTimestamp(row.date());
            String dateKey = timestamp == Long.MIN_VALUE ? row.date() : Long.toString(timestamp);
            String id = row.type() + '\0' + normalize(row.punished()) + '\0' + normalize(row.punisher())
                    + '\0' + row.reason() + '\0' + dateKey + '\0' + row.server();
            return new Indexed(id, row, normalize(row.punished() + " " + row.punisher() + " " + row.reason()), timestamp);
        }).toList() : List.of();
        synchronized (LogQueryCache.class) {
            if (failure == null) {
                entry.successes++;
                entry.limited |= batch.limited();
                entry.results.put(batch.type(), indexed);
                for (var item : indexed) {
                    var row = item.row();
                    archive.put(item.id(), item);
                }
                while (archive.size() > 30_000) archive.remove(archive.keySet().iterator().next());
                revision++;
            } else {
                Throwable cause = failure;
                while (cause.getCause() != null) cause = cause.getCause();
                entry.errors.add(cause.getMessage() == null ? "Ошибка запроса" : cause.getMessage());
            }
            if (--entry.pending == 0) {
                entry.state = entry.errors.isEmpty() ? "ok" : entry.successes > 0 ? "partial" : "error";
                entry.error = entry.errors.isEmpty() ? "" : entry.errors.getFirst();
                entry.fetchedAt = System.currentTimeMillis();
                entry.inflight = null;
                completed = snapshot(key.query(), key.server());
            }
        }
        if (failure == null && key.query().isBlank() && key.server().equals("all")) PunishNotify.onApiBatch(batch.type(), batch.rows());
        if (completed != null) result.complete(completed);
    }

    private static Entry entry(Key key) {
        Entry value = queries.computeIfAbsent(key, ignored -> new Entry());
        if (queries.size() > 80) {
            var iterator = queries.entrySet().iterator();
            while (queries.size() > 64 && iterator.hasNext()) {
                var candidate = iterator.next();
                if (!candidate.getKey().equals(key) && candidate.getValue().inflight == null) iterator.remove();
            }
        }
        return value;
    }

    private static boolean matches(Indexed item, Key key) {
        return (key.query().isBlank() || item.search().contains(normalize(key.query())))
                && (key.server().equals("all") || item.row().server().equalsIgnoreCase(key.server()));
    }

    static Comparator<PunishLogClient.Row> newestFirst() {
        return Comparator.comparingLong((PunishLogClient.Row row) -> PunishLogClient.sortTimestamp(row.date())).reversed();
    }

    private static CompletableFuture<Batch> fetch(String type, Key key) {
        String body = "search=" + encode(remoteQuery(key.query())) + "&type=" + encode(type) + "&server=" + encode(key.server());
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT)).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", "https://dehasher.net")
                .header("Referer", "https://dehasher.net/logs/envyworld/punishments")
                .header("User-Agent", "EnvyWorld-Mod/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(response -> {
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("API: HTTP " + response.statusCode());
            return parse(type, response.body());
        });
    }

    private static synchronized String remoteQuery(String query) {
        // The API's text search is case-sensitive. Keep user input intact; an
        // already known exact nickname may supply its canonical capitalization.
        if (query.matches("[A-Za-z0-9_]{1,16}")) {
            for (Indexed item : archive.values()) if (item.row().punished().equalsIgnoreCase(query)) return item.row().punished();
            for (Indexed item : archive.values()) if (item.row().punisher().equalsIgnoreCase(query)) return item.row().punisher();
        }
        return query;
    }

    static Batch parse(String type, String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            throw new IllegalStateException(root.has("message") ? clean(root.get("message").getAsString()) : "API отклонил запрос");
        }
        List<PunishLogClient.Row> rows = new ArrayList<>();
        if (root.has("rows") && root.get("rows").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("rows")) {
                if (!element.isJsonObject()) continue;
                JsonObject row = element.getAsJsonObject();
                rows.add(new PunishLogClient.Row(type, string(row, "who_was_punished"), string(row, "who_punished"),
                        string(row, "reason"), string(row, "date"), string(row, "until"), string(row, "server")));
            }
        }
        return new Batch(type, List.copyOf(rows), root.has("limited") && root.get("limited").getAsBoolean());
    }

    private static String string(JsonObject object, String key) {
        return !object.has(key) || object.get(key).isJsonNull() ? "" : clean(object.get(key).getAsString());
    }

    private static String clean(String text) {
        return text.replaceAll("(?i)<br\\s*/?>", " | ").replaceAll("(?i)</?(?:span|b|strong|i|em|a|div|p)(?:\\s[^>]*)?>", "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&nbsp;"," ").replace('\u00a0',' ').replace('\u202f',' ');
    }
    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
