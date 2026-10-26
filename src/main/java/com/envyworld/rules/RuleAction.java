package com.envyworld.rules;

import java.util.Locale;

public enum RuleAction {
	MUTE,
	BAN,
	BANIP,
	WARN,
	KICK,
	/** Punishment cannot be applied automatically (privilege removal, manual decision, ...). */
	NOTE;

	public static RuleAction parse(String raw) {
		if (raw == null) {
			return NOTE;
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return NOTE;
		}
	}
}
