package com.envyworld.ranks;

import com.envyworld.EnvyWorld;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Detects the player's rank from the tab list, scoreboard and chat, with the
 * server command tree as a fallback. The tab list / scoreboard may be hidden,
 * so the last successful result is cached in the config.
 */
public final class RankDetector {
	private static final Set<String> AMBIGUOUS_COMMANDS = Set.of(
			"ban", "unban", "mute", "unmute", "warn", "kick", "invsee", "echest",
			"speed", "spawn", "tp", "tppos", "fly", "heal", "feed",
			// Present in the tree does NOT mean usable — never treat these as proof.
			"cuff", "jail", "unjail", "vanish", "staff", "admin", "helpop",
			"op", "deop", "whitelist", "lp", "permissions", "maintenance",
			"reload", "restart", "stop", "plugins", "checkban", "checkmute",
			"alts", "dupeip", "history", "ec", "socialspy", "spy", "freeze",
			"staffchat", "report", "give", "gamemode", "gmc", "gms", "gma", "gmsp");
	private static final long CHAT_MEMORY_MS = 180_000L;

	private static Rank current;
	private static String source = "";
	private static int confidence;
	private static String evidence = "";
	private static Set<String> available = Set.of();
	private static String lastSystemMessage = "";
	private static long lastSystemMessageAt;
	private static String detectedMode = "";
	private static String lastLoggedResult = "";
	private static String lastLoggedTabText = "";
	private static int ticks;

	private static final Map<String, String> MODE_KEYWORDS = new LinkedHashMap<>();

	static {
		// Specific sub-servers first so a sidebar like "Выживание: Ио" doesn't match
		// the generic "Выживание" pattern before its own entry.
		MODE_KEYWORDS.put("альматея|алматея|almateya|almatea", "Выживание: Альматея");
		MODE_KEYWORDS.put("калипсо|kalipso|calypso|калипс", "Выживание: Калипсо");
		MODE_KEYWORDS.put("лотус|lotus", "Выживание: Лотус");
		MODE_KEYWORDS.put("европа|europe|europa", "Выживание: Европа");
		MODE_KEYWORDS.put("выживание ио|сервер ио|survival io|ио|io", "Выживание: Ио");
        MODE_KEYWORDS.put("выживание #1|выживание 1", "Выживание 1: Ио");
        MODE_KEYWORDS.put("выживание #2|выживание 2", "Выживание 2: Европа");
        MODE_KEYWORDS.put("выживание #3|выживание 3", "Выживание 3: Лотус");
        MODE_KEYWORDS.put("выживание #4|выживание 4", "Выживание 4: Калипсо");
        MODE_KEYWORDS.put("выживание #5|выживание 5", "Выживание 5: Альматея");
		MODE_KEYWORDS.put("бедварс|bedwars|bed wars", "Бедварс");
		MODE_KEYWORDS.put("скайварс|skywars|sky wars", "Скайварс");
		MODE_KEYWORDS.put("один блок|одинблок|oneblock|one block", "ОдинБлок");
		MODE_KEYWORDS.put("пвп|pvp|kitpvp|арена", "ПвП");
		MODE_KEYWORDS.put("лимбо|limbo", "Лимбо");
		MODE_KEYWORDS.put("выживание|survival", "Выживание");
		MODE_KEYWORDS.put("хаб|hub|лобби|lobby", "Хаб");
	}

	private static final Map<String, Integer> STAFF_NICK_MAP = Map.ofEntries(
			// Тех.Админ (всё остальное)
			Map.entry("dehasher", 99),
			// Тех.Админ мини-игр
			Map.entry("akv5", 98),
			// Админ
			Map.entry("muaso", 90));

	// Staff nick format is strict: EW_Moder1 .. EW_Moder9_9 and the
	// equivalent EW_Helper names. EW_Moderator3 is deliberately invalid.
	// Plain user nicks (tornd_6, tertasik, nnxyp) never match.
	private static Integer staffTierForNick(String nick) {
		if (nick == null || nick.isBlank()) {
			return null;
		}
		Integer exact = STAFF_NICK_MAP.get(nick);
		if (exact != null) {
			return exact;
		}
		if (nick.matches("ew_moder[1-9](_[1-9])?")) {
			return 80;
		}
		if (nick.matches("ew_helper[1-9](_[1-9])?")) {
			return 70;
		}
		return null;
	}

	private RankDetector() {
	}

	public static synchronized void tick(Minecraft client) {
		if (client == null) {
			return;
		}
		if (++ticks % 40 != 0) {
			return;
		}
		refresh(client);
	}

	public static synchronized void refresh(Minecraft client) {
		if (client == null || client.player == null || client.getConnection() == null) {
			current = null;
			source = "";
			confidence = 0;
			evidence = "Нет подключения";
			available = Set.of();
			detectedMode = "";
			return;
		}

		ClientPacketListener connection = client.getConnection();
		available = readAvailableCommands(connection);
        String tabText = collectTabText(client);
		detectedMode = detectMode(tabText);

		// 1) Manual override always wins.
		String override = EnvyWorld.config().rankOverride;
		if (override != null && !override.isBlank()) {
			Rank rank = RankCatalog.byId(override);
			if (rank != null) {
				current = rank;
				source = "Вручную";
				confidence = 100;
				evidence = "Выбран вручную в меню";
				return;
			}
		}

		// Mode comes ONLY from the tab list — the server name ("Ио", "Хаб", …)
		// is written there. Scoreboard and command tree are not used for this.
		if (!tabText.equals(lastLoggedTabText)) {
			lastLoggedTabText = tabText;
			EnvyWorld.LOGGER.info("RankDetector: tab text = [{}]", normalise(tabText));
		}
		String playerNick = client.getUser().getName().toLowerCase(Locale.ROOT).trim();
		if (playerNick.isBlank() && client.player != null) {
			playerNick = client.player.getName().getString().toLowerCase(Locale.ROOT).trim();
		}

		// 2) Own local nick decides staff. Nothing else can prove staff rights —
		//    commands in the tree are just advertised, not usable.
		Integer staffTier = staffTierForNick(playerNick);
		if (staffTier != null) {
			Rank best = switch (staffTier) {
				case 99, 98, 90 -> firstOf("admin");
				case 85 -> firstOf("moderator");
				default -> staffTier >= 80 ? firstOf("moderator") : firstOf("helper");
			};
			current = best;
			source = "Локальный ник игрока: " + playerNick;
			confidence = 100;
			evidence = "Ваш локальный ник распознан как staff: " + playerNick;
			logResult(playerNick);
			remember(best);
			return;
		}

		// 3) Donation rules by game mode:
		//    - On survival servers read the explicit donation row in the local sidebar.
		//    - Everywhere else only EnvyPremium exists as a subscription; it needs
		//      its full signature set, otherwise the player is a plain Игрок.
		boolean survivalMode = detectedMode.startsWith("Выживание");
		Rank best = null;

        if (survivalMode) {
            best=scoreboardDonation(client);
            if(best!=null){
                current=best;source="Scoreboard · ваша привилегия";confidence=100;
                evidence="Привилегия из строки доната вашего scoreboard: "+best.name;
                logResult(playerNick);remember(best);return;
            }
            // Advertised commands are not permission checks: even ordinary players
            // receive Envy command nodes. Never infer a survival donation from them.

		} else {
			Rank premium = RankCatalog.byId("envypremium");
			if (hasFullSignature(premium)) {
				best = premium;
			} else {
				EnvyWorld.LOGGER.info("RankDetector: envypremium missing {}",
						missingSignature(premium));
			}
		}
		EnvyWorld.LOGGER.info("RankDetector: donation check mode={} → {} (commands: {})",
				detectedMode, best == null ? "none" : best.id, available.size());

		if (best != null) {
			current = best;
			source = "Дерево команд · уникальная команда ранга";
			confidence = 100;
			evidence = "Найдена уникальная команда ранга " + best.name;
			logResult(playerNick);
			remember(best);
			return;
		}

		// 4) No evidence → player rank. Donation cache is not reused across modes:
		//    a donator on survival is a plain player in the hub.
		Rank player = RankCatalog.byId("player");
		current = player;
		source = "По умолчанию · локальный ник: " + playerNick;
		confidence = 60;
		evidence = survivalMode ? "Ожидаем строку привилегии в scoreboard; дерево команд не доказывает донат" : "Ник " + playerNick + " не является staff-ником; ранг — Игрок";
		logResult(playerNick);
	}

    private static Rank scoreboardDonation(Minecraft client){
        Objective sidebar=com.envyworld.client.ServerScoreboard.sidebar(client);
        if(sidebar==null||client.level==null)return null;
        Scoreboard board=client.level.getScoreboard();
        List<String> lines=board.listPlayerScores(sidebar).stream().filter(e->!e.isHidden())
                .sorted(java.util.Comparator.comparingInt(PlayerScoreEntry::value).reversed())
                .limit(15).map(e->PlayerTeam.formatNameForTeam(board.getPlayersTeam(e.owner()),e.ownerName()).getString()).toList();
        return donationFromSidebar(lines,RankCatalog.all());
    }
    public static Rank donationFromSidebar(List<String> lines,List<Rank> ranks){
        for(int i=0;i<lines.size();i++){
            String line=normaliseRankLabel(lines.get(i));
            var matcher=java.util.regex.Pattern.compile("(?:^| )(?:(?:ваш|ваша) )?(?:донат|привилегия|ранг|группа)(?: (.*))?$").matcher(line);
            if(!matcher.find())continue;
            String value=matcher.group(1);
            if((value==null||value.isBlank())&&i+1<lines.size())value=normaliseRankLabel(lines.get(i+1));
            if(value==null)continue;
            if(java.util.Set.of("нет","отсутствует","обычный игрок","игрок","player","default").contains(value))return ranks.stream().filter(r->"player".equals(r.id)).findFirst().orElse(null);
            Rank best=null;int bestLength=0;
            for(Rank rank:ranks){
                if(!rank.isDonation())continue;
                List<String> names=new ArrayList<>(rank.keywords==null?List.of():rank.keywords);
                names.add(rank.name);names.add(rank.id);names.add(rank.code);
                for(String name:names){
                    String key=normaliseRankLabel(name);
                    if(!key.isBlank()&&(" "+value+" ").contains(" "+key+" ")&&key.length()>bestLength){best=rank;bestLength=key.length();}
                }
            }
            if(best!=null)return best;
        }
        return null;
    }
    public static String normaliseRankLabel(String value){
        String clean=value==null?"":value.replaceAll("(?i)(?:[&§]#[0-9a-f]{6}|\\{#[0-9a-f]{6}\\})","")
                .replaceAll("(?i)[&§][0-9a-fk-orx]","");
        clean=java.text.Normalizer.normalize(clean,java.text.Normalizer.Form.NFKC);
        String small="ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢ",plain="abcdefghijklmnopqrstuvwxyz";
        for(int i=0;i<small.length();i++)clean=clean.replace(small.charAt(i),plain.charAt(i));
        return normalise(clean);
    }

	private static void logResult(String playerNick) {
		String result = playerNick + " | " + (current == null ? "null" : current.id)
				+ " | mode=" + detectedMode + " | source=" + source;
		if (!result.equals(lastLoggedResult)) {
			lastLoggedResult = result;
			EnvyWorld.LOGGER.info("RankDetector: {}", result);
		}
	}

	private static Rank firstOf(String... ids) {
		for (String id : ids) {
			Rank rank = RankCatalog.byId(id);
			if (rank != null) {
				return rank;
			}
		}
		return RankCatalog.all().isEmpty() ? null : RankCatalog.all().get(0);
	}

	private static void remember(Rank rank) {
		if (rank == null || rank.id == null) {
			return;
		}
		if (!rank.id.equalsIgnoreCase(EnvyWorld.config().lastRankId)) {
			EnvyWorld.config().lastRankId = rank.id;
			EnvyWorld.config().lastRankSource = source;
			EnvyWorld.saveConfig();
		}
	}

	private static int textScore(Rank rank, String text) {
		String normalised = normalise(text);
		Set<String> tokens = new HashSet<>(List.of(normalised.split(" ")));
		int score = 0;

		List<String> keywords = new ArrayList<>(rank.keywords == null ? List.of() : rank.keywords);
		if (rank.name != null && !rank.name.isBlank()) {
			keywords.add(rank.name);
		}
		if (rank.code != null && !rank.code.isBlank()) {
			keywords.add(rank.code);
		}

		Set<String> seen = new LinkedHashSet<>();
		for (String keyword : keywords) {
			String value = normalise(keyword);
			if (value.isBlank() || !seen.add(value)) {
				continue;
			}
			if (value.contains(" ")) {
				if ((" " + normalised + " ").contains(" " + value + " ")) {
					score += 80;
				}
			} else if (tokens.contains(value)) {
				score += 70;
			} else if (value.length() >= 4 && tokens.stream().anyMatch(token ->
					token.startsWith(value) && token.length() <= value.length() + 5)) {
				score += 45;
			}
			if (score >= 240) {
				break;
			}
		}

		return score;
	}

	// A rank counts when at least ONE of its unique signature commands is present
	// in the server-advertised command tree.
	private static boolean hasAnySignatureCommand(Rank rank) {
		if (rank.signature == null || rank.signature.isEmpty()) {
			return false;
		}
		for (String signature : rank.signature) {
			String name = normaliseCommand(signature);
			if (!name.isBlank() && !AMBIGUOUS_COMMANDS.contains(name) && available.contains(name)) {
				return true;
			}
		}
		return false;
	}

	// EnvyPremium on non-survival modes requires its whole signature set —
	// partial matches like a stray /glow in the hub do not count.
	// EnvyPremium on non-survival modes requires its whole signature set —
	// partial matches like a stray /glow in the hub do not count. Trailing
	// commas and stray chars in the JSON are stripped before comparing.
	private static boolean hasFullSignature(Rank rank) {
		if (rank == null || rank.signature == null || rank.signature.isEmpty()) {
			return false;
		}
		boolean any = false;
		for (String signature : rank.signature) {
			String name = normaliseCommand(signature);
			if (name.isBlank() || AMBIGUOUS_COMMANDS.contains(name)) {
				continue;
			}
			if (!available.contains(name)) {
				return false;
			}
			any = true;
		}
		return any;
	}

	private static List<String> missingSignature(Rank rank) {
		List<String> missing = new ArrayList<>();
		if (rank == null || rank.signature == null) {
			return missing;
		}
		for (String signature : rank.signature) {
			String name = normaliseCommand(signature);
			if (name.isBlank() || AMBIGUOUS_COMMANDS.contains(name)) {
				continue;
			}
			if (!available.contains(name)) {
				missing.add(name);
			}
		}
		return missing;
	}

	// Text for mode detection: tab header/footer + own tab entry only. This is
	// where EnvyWorld writes the server name (Ио, Хаб, Лотус, …).
    private static String collectTabText(Minecraft client) {
        // Server-owned header/footer only. Player prefixes can be changed freely.
        return tabHeaderFooter(client);
    }

	private static String detectMode(String text) {
		String normalised = " " + normalise(text).replaceAll("[^\\p{L}\\p{N}_]+", " ") + " ";
		for (Map.Entry<String, String> entry : MODE_KEYWORDS.entrySet()) {
			for (String keyword : entry.getKey().split("\\|")) {
				String value = keyword.trim().toLowerCase(Locale.ROOT);
				if (value.isBlank()) {
					continue;
				}
				if (value.equals("ио") || value.equals("io")) {
					if (normalised.contains(" " + value + " ")) {
						return entry.getValue();
					}
				} else if (normalised.contains(value)) {
					return entry.getValue();
				}
			}
		}
		return "";
	}

	private static String describeEvidence(Rank rank, String text, int textScore, int commandScore) {
		if (textScore > 0) {
			return "Найдено в табе/скорборде/чате: " + rank.name;
		}
		if (commandScore > 0) {
			return "Определено по командам, доступным на сервере";
		}
		return "Совпадений не найдено";
	}

	public static String detectedMode() {
		return detectedMode.isBlank() ? "—" : detectedMode;
	}

	public static String collectRankText(Minecraft client) {
		StringBuilder builder = new StringBuilder();
		LocalPlayer player = client.player;
		if (player == null) {
			return "";
		}

		ClientPacketListener connection = client.getConnection();
		if (connection != null) {
			PlayerInfo self = connection.getPlayerInfo(player.getUUID());
			if (self != null) {
				append(builder, self.getTabListDisplayName());
				PlayerTeam team = self.getTeam();
				if (team != null) {
					append(builder, team.getName());
					append(builder, team.getPlayerPrefix());
					append(builder, team.getPlayerSuffix());
				}
			}
			// Other players' prefixes are NOT scanned — "[Модератор] EW_Moder7" in the
			// tab must never affect the local player's rank.
		}

		append(builder, player.getDisplayName());
		append(builder, tabHeaderFooter(client));

		if (player.level() != null) {
			Scoreboard scoreboard = player.level().getScoreboard();
			Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
			if (sidebar != null) {
				append(builder, sidebar.getDisplayName());
				Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(sidebar);
				for (PlayerScoreEntry entry : entries) {
					append(builder, entry.display());
					append(builder, entry.ownerName());
				}
			}
		}

		// Chat is NOT a rank source — server messages mention ranks of other players.
		return builder.toString();
	}

	private static String tabHeaderFooter(Minecraft client) {
		try {
			PlayerTabOverlay overlay = client.gui.getTabList();
			StringBuilder builder = new StringBuilder();
			// Do not rely on mapped field names: launcher/mapping combinations may
			// expose header/footer under obfuscated names. Read every Component field.
			for (Field field : PlayerTabOverlay.class.getDeclaredFields()) {
				if (!Component.class.isAssignableFrom(field.getType())) {
					continue;
				}
				field.setAccessible(true);
				append(builder, (Component) field.get(overlay));
			}
			return builder.toString();
		} catch (Throwable ignored) {
			return "";
		}
	}

	private static void append(StringBuilder builder, Component component) {
		if (component != null) {
			builder.append(' ').append(component.getString()).append(' ');
		}
	}

	private static void append(StringBuilder builder, String value) {
		if (value != null && !value.isBlank()) {
			builder.append(' ').append(value).append(' ');
		}
	}

	private static Set<String> readAvailableCommands(ClientPacketListener connection) {
		TreeSet<String> result = new TreeSet<>();
		try {
			CommandDispatcher<ClientSuggestionProvider> dispatcher = connection.getCommands();
			if (dispatcher == null) {
				return Set.of();
			}
			for (CommandNode<ClientSuggestionProvider> node : dispatcher.getRoot().getChildren()) {
				if (node.getName() != null && !node.getName().isBlank()) {
					result.add(node.getName());
				}
			}
		} catch (RuntimeException ignored) {
			// The command tree is not available yet (e.g. during login).
		}
		return Set.copyOf(result);
	}

	public static void observe(Component message) {
		if (message == null) {
			return;
		}
		String text = normalise(message.getString());
		if (text.contains("ранг") || text.contains("привилег") || text.contains("донат")
				|| text.contains("статус") || text.contains("поздравля") || text.contains("вы теперь")
				|| text.contains("rank") || text.contains("privilege")) {
			lastSystemMessage = message.getString();
			lastSystemMessageAt = System.currentTimeMillis();
		}
	}

	public static Rank current() {
		return current;
	}

    public static boolean isStaffNick(String nick) {
        return nick != null && staffTierForNick(nick.toLowerCase(Locale.ROOT)) != null;
    }

    public static boolean staffViewer() {
        return current != null && current.isStaff() || isStaffNick(Minecraft.getInstance().getUser().getName());
    }

    public static boolean canProfileCheck(String command) {
        if (staffViewer()) return true;
        if (!"dupeip".equals(command) || current == null) return false;
        Rank president = RankCatalog.byId("president");
        return current.isDonation() && president != null && current.tier >= president.tier;
    }

	public static String displayName() {
		return current == null ? "не определён" : current.name;
	}

	public static String source() {
		return source;
	}

	public static int confidence() {
		return confidence;
	}

	public static String evidence() {
		return evidence;
	}

	public static Set<String> availableCommands() {
		return available;
	}

	public static List<String> serverCommands() {
		return available.stream().sorted().toList();
	}

	public static boolean isAvailable(String command) {
		if (command == null) {
			return false;
		}
		String name = normaliseCommand(command);
		return !name.isBlank() && available.contains(name);
	}

	public static int availableCount() {
		return available.size();
	}

	public static List<String> subcommands(String command) {
		List<String> result = new ArrayList<>();
		Minecraft client = Minecraft.getInstance();
		if (client.getConnection() == null) {
			return result;
		}
		try {
			String name = normaliseCommand(command);
			for (CommandNode<ClientSuggestionProvider> node : client.getConnection().getCommands().getRoot().getChildren()) {
				if (!node.getName().equals(name)) {
					continue;
				}
				for (CommandNode<ClientSuggestionProvider> child : node.getChildren()) {
					if (child.getName() != null && !child.getName().isBlank() && result.size() < 8) {
						result.add(child.getName());
					}
				}
			}
		} catch (RuntimeException ignored) {
			// Ignore an incomplete command tree.
		}
		return result;
	}

	public static void setOverride(String rankId) {
		EnvyWorld.config().rankOverride = rankId == null ? "" : rankId;
		EnvyWorld.saveConfig();
		refresh(Minecraft.getInstance());
	}

	public static void clearOverride() {
		EnvyWorld.config().rankOverride = "";
		EnvyWorld.saveConfig();
		refresh(Minecraft.getInstance());
	}

	public static String normaliseCommand(String command) {
		String value = command == null ? "" : command.trim().toLowerCase(Locale.ROOT);
		while (value.startsWith("/")) {
			value = value.substring(1);
		}
		int space = value.indexOf(' ');
		if (space >= 0) {
			value = value.substring(0, space);
		}
		return value;
	}

	public static String normalise(String value) {
		if (value == null) {
			return "";
		}
		String lower = value.toLowerCase(Locale.ROOT).replace('ё', 'е');
		StringBuilder builder = new StringBuilder(lower.length());
		boolean space = true;
		for (int index = 0; index < lower.length(); index++) {
			char character = lower.charAt(index);
			if (Character.isLetterOrDigit(character)) {
				builder.append(character);
				space = false;
			} else if (!space) {
				builder.append(' ');
				space = true;
			}
		}
		return builder.toString().trim();
	}
}
