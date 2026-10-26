package com.envyworld.ranks;

import java.util.ArrayList;
import java.util.List;

/**
 * A rank from the EnvyWorld shop, a staff rank, or the default player rank.
 */
public class Rank {
	public String id = "";
	public String code = "";
	public String name = "";
	/** player, donation, premium or staff. */
	public String kind = "player";
	/** #RRGGBB colour used by the GUI. */
	public String color = "#A0A0B0";
	public String price = "";
	public int tier;
	public List<String> keywords = new ArrayList<>();
	/** Commands unique to this rank; used as a fallback when the tab list is hidden. */
	public List<String> signature = new ArrayList<>();
    /** Official shop commands, separated by mode; these are not proof of permission. */
    public java.util.Map<String,List<String>> commandsByMode = new java.util.LinkedHashMap<>();
    public java.util.Map<String,List<String>> signaturesByMode = new java.util.LinkedHashMap<>();
	/** Commands unique to this rank (without inherited ones). */
	public List<RankCommand> own = new ArrayList<>();
	/** All commands, including inherited ones from lower donation ranks. */
	public List<RankCommand> commands = new ArrayList<>();
	public List<String> perks = new ArrayList<>();

	public boolean isStaff() {
		return "staff".equalsIgnoreCase(kind);
	}

	public boolean isDonation() {
		return "donation".equalsIgnoreCase(kind) || "premium".equalsIgnoreCase(kind);
	}

	public String kindName() {
		return switch (kind == null ? "" : kind.toLowerCase()) {
			case "donation" -> "Донат";
			case "premium" -> "Premium";
			case "staff" -> "Персонал";
			default -> "Игрок";
		};
	}

	public int argb() {
		try {
			String value = color == null ? "" : color.replace("#", "");
			if (value.length() == 6) {
				return 0xFF000000 | Integer.parseInt(value, 16);
			}
		} catch (NumberFormatException ignored) {
			// Fall through to the default colour.
		}
		return 0xFFA0A0B0;
	}
}
