package com.envyworld.logs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Query isolation, immediate search, ordering, server filtering and visible API errors. */
public class LogCacheSmokeTest {
    private static final String SAMPLE = """
        {"success":true,"limited":true,"rows":[
          {"who_was_punished":"Alice","who_punished":"Mod","reason":"Rule <ник>","date":"2026-10-01 12:00","until":"Today<br>(removed)","server":"Хаб"},
          {"who_was_punished":"Bob","who_punished":"Alice","reason":"Spam","date":"2026-10-20 12:00","until":"","server":"ОдинБлок"},
          {"who_was_punished":"AliceExtra","who_punished":"Mod","reason":"Other","date":"2026-10-21 12:00","until":"","server":"Хаб"}
        ]}
        """;

    public static void main(String[] args) throws Exception {
        ScoreboardRankSmoke.run();
        ProfileChecksSmoke.run();
        UiBehaviorSmokeTest.run();
        ModerationOverlaySmoke.run();
        LifecycleLayoutSmoke.run();
        FreezeGuardSmoke.run();
        BurstBudgetSmoke.run();
        RenderContextSmoke.run();
        NotificationReconcileSmoke.run();
        PanelAndSequenceSmoke.run();
        TelegramAuthSmoke.run();
        ProjectionSmoke.run();
        publish("seed", "all", "mutes", SAMPLE, null);
        check(new LogQueryCache.Key("Bodyaha", "all").query().equals("Bodyaha"), "API query must preserve nickname capitalization");
        Method remoteQuery = LogQueryCache.class.getDeclaredMethod("remoteQuery", String.class); remoteQuery.setAccessible(true);
        check(remoteQuery.invoke(null, "ALICE").equals("Alice"), "Use canonical capitalization of a known nickname for the case-sensitive API");
        check(remoteQuery.invoke(null, "Bodyaha").equals("Bodyaha"), "Unknown nickname must retain the typed case");
        long start = System.nanoTime();
        var alice = PunishLogClient.snapshot("ALICE", "all");
        check(alice.rows().size() == 3, "Case-insensitive substring lookup");
        check((System.nanoTime() - start) / 1_000_000 < 200, "Cached search must not wait for a network request");
        check(alice.rows().getFirst().punished().equals("AliceExtra"), "Newest-first ordering");
        check(PunishLogClient.snapshot("alice", "Хаб").rows().size() == 2, "Server filter");
        List<PunishLogClient.Row> exact = alice.rows().stream().filter(row -> row.punished().equalsIgnoreCase("Alice")
                || row.punisher().equalsIgnoreCase("Alice")).toList();
        check(exact.size() == 2, "Profile must exclude partial nick matches");
        check(exact.stream().filter(row -> row.punished().equalsIgnoreCase("Alice")).count() == 1, "Received penalties");
        check(exact.stream().filter(row -> row.punisher().equalsIgnoreCase("Alice")).count() == 1, "Issued penalties");
        check(exact.stream().anyMatch(row -> row.reason().equals("Rule <ник>") && row.until().contains(" | ")), "Preserve text and clean HTML");
        publish("alice", "all", "mutes", SAMPLE, null);
        check(PunishLogClient.snapshot("alice", "all").limited(), "Propagate the API result limit");
        check(PunishLogClient.snapshot("bob", "all").rows().size() == 1, "Other query completion must not replace Bob's view");
        publish("alice", "all", "mutes", null, new IllegalStateException("API unavailable"));
        var failed = PunishLogClient.snapshot("alice", "all");
        check(failed.state().equals("error") && failed.rows().size() == 3, "Preserve cached rows and expose refresh errors");
        check(PunishLogClient.sortTimestamp("31 октября в 12:00") != PunishLogClient.sortTimestamp("28 октября в 12:00"), "Do not clamp all month-end dates to 28");
        check(PunishLogClient.sortTimestamp("2026-02-30 99:99") == Long.MIN_VALUE, "Invalid dates must not break the async cache");
        check(PunishLogClient.sortTimestamp("2026-10-01T12:00:03") == java.time.OffsetDateTime.parse("2026-10-01T12:00:03+03:00").toEpochSecond(), "ISO T must survive lowercasing and preserve seconds");
        check(PunishLogClient.sortTimestamp("3 июля 2024 в 15:02") == java.time.OffsetDateTime.parse("2024-07-03T15:02:00+03:00").toEpochSecond(), "Respect explicit Russian years");
        check(PunishLogClient.sortTimestamp("2026-10-01 12:00") < PunishLogClient.sortTimestamp("2026-10-20 12:00"), "Historical ISO dates must not turn into today");
        publish("alice", "Хаб", "mutes", SAMPLE, null);
        var hubBefore = PunishLogClient.snapshot("alice", "Хаб").rows();
        publish("alice", "all", "mutes", "{\"success\":true,\"rows\":[]}", null);
        check(PunishLogClient.snapshot("alice", "all").rows().isEmpty(), "An authoritative empty response must remove stale query matches");
        publish("alice", "ОдинБлок", "mutes", SAMPLE, null);
        check(PunishLogClient.snapshot("alice", "all").rows().isEmpty(), "Later server responses must not repopulate another query's completed categories");
        check(PunishLogClient.snapshot("alice", "Хаб").rows().equals(hubBefore), "Switching back must preserve server-specific ordering and rows");
        check(PunishLogClient.snapshot("alice", "ОдинБлок").rows().size() == 1, "Selected-server response remains scoped");
        var active = new PunishLogClient.Row("mutes", "Alice", "Mod", "Rule", "", Long.toString(System.currentTimeMillis() / 1000 + 120), "Хаб");
        check(PunishLogClient.expiryTimestamp(active.until()) == Long.parseLong(active.until()), "Epoch timestamps must never shift with the computer timezone");
        check(PunishLogClient.expiryTimestamp("2026-10-23T15:00:00+03:00") == java.time.Instant.parse("2026-10-23T12:00:00Z").getEpochSecond(), "Explicit timezone offsets");
        PunishmentClock.remember(active);
        String first = PunishmentClock.status(active).label();
        Thread.sleep(1100);
        check(!first.equals(PunishmentClock.status(active).label()), "Remaining time must advance without another API response");
        var removed = new PunishLogClient.Row("mutes", "Alice", "Mod", "Rule", "", "Завтра в 12:00 | (наказание снято игроком Mod)", "Хаб");
        check(PunishmentClock.status(removed).label().equals("Снято"), "Removed penalties must never show an active timer");
        var forever = new PunishLogClient.Row("bans", "Alice", "Mod", "Rule", "", "Навсегда", "Хаб");
        check(PunishmentClock.status(forever).label().equals("Навсегда"), "Permanent penalties");
        check(PunishLogClient.expiryTimestamp("Завтра в 12:00") > System.currentTimeMillis() / 1000, "Future expiry must not be moved to last year");
        com.envyworld.client.gui.SkinLookupSmokeTest.main(args);
        System.out.println("PASS: immediate cached search, query isolation, newest-first dates, exact profiles, server filters, HTML and API error/limit handling.");
    }

    static void publish(String query, String server, String type, String json, Throwable failure) throws Exception {
        var key = new LogQueryCache.Key(query, server);
        Method getEntry = LogQueryCache.class.getDeclaredMethod("entry", LogQueryCache.Key.class);
        getEntry.setAccessible(true);
        Object entry = getEntry.invoke(null, key);
        Field pending = entry.getClass().getDeclaredField("pending"); pending.setAccessible(true); pending.setInt(entry, 1);
        Field successes = entry.getClass().getDeclaredField("successes"); successes.setAccessible(true); successes.setInt(entry, 0);
        Field errors = entry.getClass().getDeclaredField("errors"); errors.setAccessible(true); ((List<?>) errors.get(entry)).clear();
        Method parse = LogQueryCache.class.getDeclaredMethod("parse", String.class, String.class);
        parse.setAccessible(true);
        Object batch = json == null ? null : parse.invoke(null, type, json);
        Method complete = LogQueryCache.class.getDeclaredMethod("complete", LogQueryCache.Key.class, entry.getClass(), parse.getReturnType(), Throwable.class, CompletableFuture.class);
        complete.setAccessible(true);
        CompletableFuture<PunishLogClient.Snapshot> result = new CompletableFuture<>();
        complete.invoke(null, key, entry, batch, failure, result);
        check(result.isDone(), "Completed request must publish its snapshot");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
