package com.envyworld.rules;

public class RulePreset {
	public String code = "";
	public String title = "";
	public RuleAction action = RuleAction.NOTE;
	/** Default duration, e.g. {@code 1h}. Empty = permanent for bans / mutes. */
	public String duration = "";
	/** Soft lower bound from the rules (informational). */
	public String min = "";
	/** Soft upper bound from the rules (informational). */
	public String max = "";
	public String note = "";

	public String display() {
		StringBuilder builder = new StringBuilder("[").append(code).append("] ").append(title);
		if (!duration.isBlank()) {
			builder.append(" (").append(duration).append(')');
		}
		return builder.toString();
	}
}
