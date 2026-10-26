package com.envyworld.ranks;

import com.envyworld.EnvyWorld;
import com.google.gson.Gson;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rank catalogue bundled with the mod. Can be overridden by
 * {@code config/envyworld/ranks.json} without rebuilding the jar.
 */
public final class RankCatalog {
	private static final Gson GSON = new Gson();
	private static final Map<String, Rank> BY_ID = new LinkedHashMap<>();
	private static final List<Rank> ALL = new ArrayList<>();

	private RankCatalog() {
	}

	public static void load(Logger logger) {
		List<Rank> loaded = readBundled(logger);

		Path override = EnvyWorld.dataDir().resolve("ranks.json");
		if (Files.exists(override)) {
			try (Reader reader = Files.newBufferedReader(override, StandardCharsets.UTF_8)) {
				Rank[] values = GSON.fromJson(reader, Rank[].class);
				if (values != null && values.length > 0) {
					loaded = new ArrayList<>(List.of(values));
					logger.info("Loaded {} rank(s) from {}", loaded.size(), override);
				}
			} catch (Exception exception) {
				logger.error("Failed to read {}; using bundled ranks", override, exception);
			}
		}

		BY_ID.clear();
		ALL.clear();

		for (Rank rank : loaded) {
			if (rank == null || rank.id == null || rank.id.isBlank()
					|| "owner".equalsIgnoreCase(rank.id)
					|| "владелец".equalsIgnoreCase(rank.name)) {
				continue;
			}
			normalise(rank);
			BY_ID.put(rank.id.toLowerCase(Locale.ROOT), rank);
			ALL.add(rank);
		}

		ALL.sort((a, b) -> {
			int tier = Integer.compare(a.tier, b.tier);
			return tier != 0 ? tier : a.name.compareToIgnoreCase(b.name);
		});
	}

	private static List<Rank> readBundled(Logger logger) {
		try (InputStream stream = RankCatalog.class.getResourceAsStream("/envyworld/ranks.json")) {
			if (stream == null) {
				logger.error("Bundled /envyworld/ranks.json is missing");
				return List.of();
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				Rank[] values = GSON.fromJson(reader, Rank[].class);
				return values == null ? List.of() : new ArrayList<>(List.of(values));
			}
		} catch (Exception exception) {
			logger.error("Failed to read bundled ranks", exception);
			return List.of();
		}
	}

	private static void normalise(Rank rank) {
		if (rank.code == null || rank.code.isBlank()) {
			rank.code = rank.id;
		}
		if (rank.name == null || rank.name.isBlank()) {
			rank.name = rank.id;
		}
		if (rank.kind == null || rank.kind.isBlank()) {
			rank.kind = "player";
		}
		if (rank.keywords == null) {
			rank.keywords = new ArrayList<>();
		}
		if (rank.signature == null) {
			rank.signature = new ArrayList<>();
		}
		if (rank.own == null) {
			rank.own = new ArrayList<>();
		}
		if (rank.commands == null || rank.commands.isEmpty()) {
			rank.commands = new ArrayList<>(rank.own);
		}
		if (rank.perks == null) {
			rank.perks = new ArrayList<>();
		}
	}

	public static List<Rank> all() {
		return List.copyOf(ALL);
	}
    public static List<RankCommand> commandsForMode(Rank rank,String mode){
        if(rank==null)return List.of();
        if(mode==null||mode.isBlank()||mode.equals("—"))return rank.commands;
        if(rank.commandsByMode==null||rank.commandsByMode.isEmpty())return rank.commands;
        String key=mode.startsWith("Выживание")?"survival":mode.equals("ОдинБлок")?"oneblock":"minigames";
        List<String> roots=rank.commandsByMode.getOrDefault(key,List.of());
        return rank.commands.stream().filter(c->roots.contains(c.name())).toList();
    }

	public static Rank byId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		return BY_ID.get(id.toLowerCase(Locale.ROOT));
	}

	/** Donation ladder from cheapest to most expensive. */
	public static List<Rank> donationLadder() {
		List<Rank> result = new ArrayList<>();
		for (Rank rank : ALL) {
			if ("donation".equalsIgnoreCase(rank.kind)) {
				result.add(rank);
			}
		}
		return result;
	}

	public static List<Rank> staffRanks() {
		List<Rank> result = new ArrayList<>();
		for (Rank rank : ALL) {
			if (rank.isStaff()) {
				result.add(rank);
			}
		}
		return result;
	}
}
