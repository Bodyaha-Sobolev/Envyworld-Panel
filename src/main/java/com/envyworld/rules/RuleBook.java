package com.envyworld.rules;

import com.envyworld.EnvyWorld;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.Map;

/**
 * Rule presets taken from https://envyworld.gg/rules (global / chat / process / explanations).
 * Stored as a bundled resource and can be overridden by {@code config/envyworld/rules.json}.
 */
public final class RuleBook {
	private static final Gson GSON = new Gson();
	private static final Map<String, RulePreset> PRESETS = new LinkedHashMap<>();

	private RuleBook() {
	}

	public static void load(Logger logger) {
		PRESETS.clear();

		Path override = EnvyWorld.dataDir().resolve("rules.json");
		if (Files.exists(override)) {
			try (Reader reader = Files.newBufferedReader(override, StandardCharsets.UTF_8)) {
				read(reader, logger, override.toString());
				return;
			} catch (Exception exception) {
				logger.error("Failed to read rule presets from {}", override, exception);
			}
		}

		try (InputStream stream = RuleBook.class.getResourceAsStream("/envyworld/rules.json")) {
			if (stream == null) {
				logger.warn("Bundled rule presets are missing");
				return;
			}
			read(new InputStreamReader(stream, StandardCharsets.UTF_8), logger, "bundled rules.json");
		} catch (Exception exception) {
			logger.error("Failed to read bundled rule presets", exception);
		}
	}

	private static void read(Reader reader, Logger logger, String source) {
		List<RulePreset> loaded = GSON.fromJson(reader, new TypeToken<List<RulePreset>>() {
		}.getType());

		if (loaded == null) {
			return;
		}

		for (RulePreset preset : loaded) {
			if (preset.code != null && !preset.code.isBlank()) {
				PRESETS.put(preset.code, preset);
			}
		}

		logger.info("Loaded {} rule preset(s) from {}", PRESETS.size(), source);
	}

	public static RulePreset get(String code) {
		return PRESETS.get(code);
	}

	public static List<String> codes() {
		return new ArrayList<>(PRESETS.keySet());
	}

	public static List<RulePreset> all() {
		return new ArrayList<>(PRESETS.values());
	}
}
