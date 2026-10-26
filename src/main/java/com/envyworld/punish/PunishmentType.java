package com.envyworld.punish;

import java.util.Locale;

public enum PunishmentType {
	WARN("warn", false),
	KICK("kick", false),
	MUTE("mute", true),
	BAN("ban", true);

	private final String id;
	private final boolean timed;

	PunishmentType(String id, boolean timed) {
		this.id = id;
		this.timed = timed;
	}

	public String id() {
		return id;
	}

	/** Whether a duration makes sense for this punishment (mute / ban). */
	public boolean timed() {
		return timed;
	}

	public static PunishmentType byId(String input) {
		String value = input.toLowerCase(Locale.ROOT);

		for (PunishmentType type : values()) {
			if (type.id.equals(value)) {
				return type;
			}
		}

		return switch (value) {
			case "warn", "warning", "предупреждение" -> WARN;
			case "kick", "кик" -> KICK;
			case "mute", "мут" -> MUTE;
			case "ban", "бан" -> BAN;
			default -> null;
		};
	}
}
