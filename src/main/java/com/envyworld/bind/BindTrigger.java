package com.envyworld.bind;

import java.util.Locale;

public enum BindTrigger {
	/** Player hits an entity (left click). */
	ATTACK_ENTITY("attack"),
	/** Player left clicks a block. */
	ATTACK_BLOCK("break"),
	/** Player right clicks with an item (air or block). */
	USE_ITEM("use"),
	/** Manual trigger: player presses the macro key (default V) while holding the item. */
	KEY("key"),
    RADIUS("radius"),
    /** A keyboard command that does not require a player under the crosshair. */
    COMMAND("command"),
    PROJECTILE_HIT("projectile");

	private final String id;

	BindTrigger(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static BindTrigger byId(String input) {
		String value = input.toLowerCase(Locale.ROOT);

		for (BindTrigger trigger : values()) {
			if (trigger.id.equals(value)) {
				return trigger;
			}
		}

		return switch (value) {
			case "hit", "удар", "атака" -> ATTACK_ENTITY;
			case "блок" -> ATTACK_BLOCK;
			case "использование", "использовать", "right_click", "rclick" -> USE_ITEM;
			case "кнопка", "hotkey", "macro", "макрос" -> KEY;
			default -> null;
		};
	}
}
