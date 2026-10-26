package com.envyworld.config;

import com.envyworld.bind.Binding;

import java.util.ArrayList;
import java.util.List;

/**
 * Global mod settings, stored in {@code config/envyworld/config.json}.
 */
public class RulesConfig {
	/** Shown in the audit log, e.g. "Хаб", "ПвП", "Европа". */
	public String serverName = "EnvyWorld";
	/** Broadcast punishment messages to everyone online. */
	public boolean announcePunishments = true;
	/** Cancel chat messages from muted players. */
	public boolean muteBlocksChat = true;
	/** Refuse punishments without a reason. */
	public boolean requireReason = true;
	/** Warn (in chat) when a reason is shorter than this. 0 disables the check. */
	public int minReasonLength = 15;
	/** Do not let non-owners punish players that are OP. */
	public boolean protectStaff = true;

	/** Fallback vanilla permission levels (1 = moderators ... 4 = owners). */
	public int punishLevel = 2;
	public int banIpLevel = 3;
	public int bindLevel = 3;
	public int reloadLevel = 3;

	/**
	 * Player names or UUIDs that may use GUI actions without permission nodes or privilege commands.
	 * Useful for staff ranks that only have the "GM" role. Example: {@code ["Steve", "Alex"]}.
	 */
	public List<String> guiStaff = new ArrayList<>();

	/** Command binds (see {@code /rules bind}). */
	public List<Binding> binds = new ArrayList<>();

	/** Manual rank override; empty means auto-detect. */
	public String rankOverride = "";
	/** Last successfully detected rank, used when the tab list / scoreboard is hidden. */
	public String lastRankId = "";
	/** Last detection source, for display only. */
	public String lastRankSource = "";
	/** Locally saved custom item presets (Items -> Свои предметы). */
	public List<com.envyworld.items.CustomItemPreset> customItems = new ArrayList<>();
	/** True after the bundled starter presets have been added once. */
	public boolean customItemsSeeded = false;
	/** Appended to every punishment reason the GUI sends (e.g. Telegram contact). */
	public String punishReasonSuffix = "";
    public ClientSettings client = new ClientSettings();
    public boolean starterItemsRemoved = false;
    public int bundledCollectionVersion;

	public Binding bindById(String id) {
		for (Binding binding : binds) {
			if (binding.id.equalsIgnoreCase(id)) {
				return binding;
			}
		}
		return null;
	}

	public List<Binding> sortedBinds() {
		List<Binding> result = new ArrayList<>(binds);
		result.sort((a, b) -> a.id.compareToIgnoreCase(b.id));
		return result;
	}
}
