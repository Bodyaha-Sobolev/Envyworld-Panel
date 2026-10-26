package com.envyworld.logs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the network-wide online list by asking the server for command
 * suggestions on "/f add &lt;prefix&gt;" — exactly the same packet vanilla
 * sends when you press Tab in chat. No command is ever executed.
 *
 * <p>Requests are rate-limited: one prefix per {@value #REQUEST_INTERVAL_MS} ms
 * so the anti-flood filter never sees a burst. A full sweep takes ~30 s and
 * then sleeps for {@value #SCAN_PERIOD_MS} ms.</p>
 */
public final class OnlineScanner {
	private static final long SCAN_PERIOD_MS = 30_000L;
	private static final long REQUEST_INTERVAL_MS = 120L;
	private static final long RESPONSE_TIMEOUT_MS = 700L;
	private static final String[] PREFIXES = {
			"_", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
			"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
			"n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
	};

	private static final AtomicInteger NEXT_ID = new AtomicInteger(90_000);

	private static final Queue<String> pendingPrefixes = new ArrayDeque<>();
	private static final Set<String> collected = new LinkedHashSet<>();
	private static final Set<String> visible = new LinkedHashSet<>();
	private static final Set<Integer> pendingIds = new LinkedHashSet<>();
    private static final Set<String> checkedPrefixes = new LinkedHashSet<>();
    public static boolean hasChecked(String name) { return !name.isBlank() && checkedPrefixes.contains(name.substring(0, 1).toLowerCase(java.util.Locale.ROOT)); }
    private static final java.util.Map<Integer, String> requestPrefixes = new java.util.HashMap<>();
	private static boolean receivedThisScan;

	private static volatile List<String> online = List.of();
	private static volatile String state = "idle"; // idle | scanning | done

	private static long scanStartedAt;
	private static long nextRequestAt;
	private static int awaitingId = -1;
	private static long awaitingSince;

	private OnlineScanner() {
	}

	public static List<String> online() {
		return online;
	}

	public static String state() {
		return state;
	}

	public static int collectedCount() {
		return collected.size();
	}

	public static void tick(Minecraft client) {
		if (client == null || client.player == null || client.getConnection() == null) {
            if (scanStartedAt != 0) {
                online = List.of(); state = "idle"; scanStartedAt = 0;
                collected.clear(); visible.clear(); pendingIds.clear(); requestPrefixes.clear(); pendingPrefixes.clear(); checkedPrefixes.clear(); awaitingId = -1;
            }
			return;
		}
		long now = System.currentTimeMillis();
		ClientPacketListener connection = client.getConnection();
		if (scanStartedAt == 0) {
			startScan();
		}

		switch (state) {
			case "idle" -> {
				if (now - scanStartedAt >= SCAN_PERIOD_MS) {
					startScan();
				}
			}
			case "done" -> {
				if (now - scanStartedAt >= SCAN_PERIOD_MS) {
					startScan();
				}
			}
			case "scanning" -> {
				// Response never arrived — skip this prefix.
				if (awaitingId >= 0 && now - awaitingSince > RESPONSE_TIMEOUT_MS) {
					pendingIds.remove(awaitingId);
                    requestPrefixes.remove(awaitingId);
					awaitingId = -1;
				}
				if (awaitingId >= 0 || now < nextRequestAt) {
					return;
				}
				String prefix = pendingPrefixes.poll();
				if (prefix == null) {
					finishScan();
					return;
				}
				int id = NEXT_ID.updateAndGet(v -> v >= 99_000 ? 90_000 : v + 1);
				pendingIds.add(id);
                requestPrefixes.put(id, prefix);
				awaitingId = id;
				awaitingSince = now;
				nextRequestAt = now + REQUEST_INTERVAL_MS;
				connection.getConnection().send(
						new ServerboundCommandSuggestionPacket(id, "/f add " + prefix));
			}
		}
	}

	public static void rescanNow() {
		startScan();
	}

	private static void startScan() {
		pendingPrefixes.clear();
		for (String prefix : PREFIXES) {
			pendingPrefixes.add(prefix);
		}
		// Keep the last completed list visible while the next sweep is warming up.
		// A failed/partial sweep must never make the GUI flash to zero.
		collected.clear();
        checkedPrefixes.clear();
		pendingIds.clear();
        requestPrefixes.clear();
		receivedThisScan = false;
		awaitingId = -1;
		state = "scanning";
		scanStartedAt = System.currentTimeMillis();
		nextRequestAt = 0;
	}

	private static void finishScan() {
		if (!receivedThisScan) {
            state = "done";
            awaitingId = -1;
            return;
        }
		List<String> sorted = new ArrayList<>(visible);
		sorted.sort(String.CASE_INSENSITIVE_ORDER);
		online = List.copyOf(sorted);
		state = "done";
		awaitingId = -1;
	}

	/** Called by {@code ClientPacketListenerMixin} on every suggestion reply. */
	public static void accept(ClientboundCommandSuggestionsPacket packet) {
		if (!pendingIds.remove(packet.id())) {
			return; // a vanilla suggestion — not ours
		}
        String prefix = requestPrefixes.remove(packet.id());
        if (prefix == null) return;
        checkedPrefixes.add(prefix);
		if (packet.id() == awaitingId) {
			awaitingId = -1;
		}
		receivedThisScan = true;
        // Replace only the prefix that successfully replied. Unchecked / timed-out
        // prefixes retain their previous snapshot until a later successful reply.
        visible.removeIf(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix));
		for (ClientboundCommandSuggestionsPacket.Entry entry : packet.suggestions()) {
			String name = entry.text();
			if (name == null) {
				continue;
			}
			name = name.trim();
			// Suggestions should be bare nicks; strip the command echo just in case.
			int lastSpace = name.lastIndexOf(' ');
			if (lastSpace >= 0) {
				name = name.substring(lastSpace + 1);
			}
			if (name.matches("[A-Za-z0-9_]{2,16}")) {
                if (name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix)) {
                    collected.add(name);
                    visible.add(name);
                }
			}
		}
		// Live-update the visible list so results appear as soon as each
		// prefix responds, not only after the whole cycle finishes.
		if (receivedThisScan) {
			List<String> sorted = new ArrayList<>(visible);
			sorted.sort(String.CASE_INSENSITIVE_ORDER);
			online = List.copyOf(sorted);
		}
	}
}
