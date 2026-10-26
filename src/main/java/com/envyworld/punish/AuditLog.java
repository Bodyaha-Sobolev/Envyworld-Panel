package com.envyworld.punish;

import com.envyworld.EnvyWorld;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Append-only JSONL audit trail: every punishment and every removal is written here.
 * Stored in {@code config/envyworld/audit.log}.
 */
public final class AuditLog {
	private static final Gson GSON = new Gson();

	private AuditLog() {
	}

	public static void write(String action, String target, String issuer, PunishmentType type, String reason, long expiresAt, String extra) {
		JsonObject entry = new JsonObject();
		entry.addProperty("time", Instant.now().toString());
		entry.addProperty("server", EnvyWorld.config().serverName);
		entry.addProperty("action", action);
		entry.addProperty("type", type == null ? "-" : type.id());
		entry.addProperty("target", target);
		entry.addProperty("issuer", issuer);
		entry.addProperty("reason", reason == null ? "" : reason);
		entry.addProperty("expiresAt", expiresAt);
		if (extra != null && !extra.isBlank()) {
			entry.addProperty("extra", extra);
		}

		Path file = EnvyWorld.dataDir().resolve("audit.log");
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(entry) + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException exception) {
			EnvyWorld.LOGGER.error("Failed to write audit log", exception);
		}
	}
}
