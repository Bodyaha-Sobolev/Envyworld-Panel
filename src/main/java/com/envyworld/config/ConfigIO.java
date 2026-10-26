package com.envyworld.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigIO {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private ConfigIO() {
	}

	public static <T> T load(Path file, Class<T> type, Logger logger) {
		if (!Files.exists(file)) {
			T fresh;
			try {
				fresh = type.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Cannot create " + type.getName(), exception);
			}
			save(file, fresh, logger);
			return fresh;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			T loaded = GSON.fromJson(reader, type);
			return loaded == null ? type.getDeclaredConstructor().newInstance() : loaded;
		} catch (Exception exception) {
			logger.error("Failed to read {}", file, exception);
			try {
				return type.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException reflection) {
				throw new IllegalStateException("Cannot create " + type.getName(), reflection);
			}
		}
	}

	public static void save(Path file, Object value, Logger logger) {
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(value, writer);
			}
		} catch (IOException exception) {
			logger.error("Failed to write {}", file, exception);
		}
	}
}
