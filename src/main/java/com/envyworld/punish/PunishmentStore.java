package com.envyworld.punish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistent list of punishments, stored in {@code config/envyworld/punishments.json}.
 */
public class PunishmentStore {
	private static final int HISTORY_LIMIT = 1000;

	private final Path file;
	private final Logger logger;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final List<Punishment> entries = new ArrayList<>();

	public PunishmentStore(Path file, Logger logger) {
		this.file = file;
		this.logger = logger;
	}

	public synchronized void load() {
		entries.clear();

		if (!Files.exists(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			List<Punishment> loaded = gson.fromJson(reader, new TypeToken<List<Punishment>>() {
			}.getType());

			if (loaded != null) {
				entries.addAll(loaded);
			}
		} catch (Exception exception) {
			logger.error("Failed to read punishments from {}", file, exception);
		}

		// Drop expired entries on load, they only clutter the file.
		entries.removeIf(punishment -> !punishment.active());
	}

	public synchronized void save() {
		try {
			Files.createDirectories(file.getParent());
			trim();
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(entries, writer);
			}
		} catch (IOException exception) {
			logger.error("Failed to save punishments to {}", file, exception);
		}
	}

	public synchronized void add(Punishment punishment) {
		entries.removeIf(existing -> existing.uuid.equals(punishment.uuid) && existing.type == punishment.type);
		entries.add(punishment);
		save();
	}

	/** @return active punishment of the given type, or {@code null} */
	public synchronized Punishment active(UUID uuid, PunishmentType type) {
		long now = System.currentTimeMillis();

		Punishment found = null;
		for (Punishment punishment : entries) {
			if (!punishment.type.equals(type) || !punishment.uuid.equals(uuid)) {
				continue;
			}
			if (punishment.expiresAt > 0L && punishment.expiresAt <= now) {
				continue;
			}
			if (found == null || punishment.issuedAt > found.issuedAt) {
				found = punishment;
			}
		}
		return found;
	}

	public synchronized List<Punishment> active(PunishmentType type) {
		return entries.stream()
				.filter(punishment -> punishment.type == type && punishment.active())
				.sorted(Comparator.comparingLong((Punishment p) -> p.expiresAt <= 0L ? Long.MAX_VALUE : p.expiresAt))
				.collect(Collectors.toList());
	}

	public synchronized List<Punishment> history(UUID uuid) {
		return entries.stream()
				.filter(punishment -> punishment.uuid.equals(uuid))
				.sorted(Comparator.comparingLong((Punishment p) -> p.issuedAt).reversed())
				.collect(Collectors.toList());
	}

	/** Removes every entry of the given type for a player. @return amount removed */
	public synchronized int clear(UUID uuid, PunishmentType type) {
		int before = entries.size();
		entries.removeIf(punishment -> punishment.uuid.equals(uuid) && punishment.type == type);
		int removed = before - entries.size();

		if (removed > 0) {
			save();
		}
		return removed;
	}

	private void trim() {
		if (entries.size() <= HISTORY_LIMIT) {
			return;
		}

		entries.sort(Comparator.comparingLong((Punishment p) -> p.issuedAt));
		while (entries.size() > HISTORY_LIMIT) {
			entries.remove(0);
		}
	}
}
