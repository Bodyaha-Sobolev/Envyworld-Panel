package com.envyworld.ranks;

/**
 * One command listed for a rank. {@code command} always includes the leading slash,
 * {@code args} is the usage suffix such as {@code [ник] [причина]}.
 */
public class RankCommand {
	public String command = "";
	public String args = "";
	public String description = "";
	public String group = "";

	public String full() {
		String value = command == null ? "" : command.trim();
		if (args != null && !args.isBlank()) {
			value += " " + args.trim();
		}
		return value;
	}

	/** Command name without the leading slash, used to compare with the server command tree. */
	public String name() {
		String value = command == null ? "" : command.trim();
		while (value.startsWith("/")) {
			value = value.substring(1);
		}

		int space = value.indexOf(' ');
		return space < 0 ? value : value.substring(0, space);
	}
}
