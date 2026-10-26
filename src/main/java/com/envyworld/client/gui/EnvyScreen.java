package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;
import com.envyworld.bind.BindTrigger;
import com.envyworld.bind.Binding;
import com.envyworld.client.ClientBinds;
import com.envyworld.items.CustomItemPreset;
import com.envyworld.items.CustomItems;
import com.envyworld.logs.PunishLogClient;
import com.envyworld.ranks.Rank;
import com.envyworld.ranks.RankCatalog;
import com.envyworld.ranks.RankCommand;
import com.envyworld.ranks.RankDetector;
import com.envyworld.rules.RuleAction;
import com.envyworld.rules.RuleBook;
import com.envyworld.rules.RulePreset;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Main EnvyWorld GUI: tabs for punishments, command macros, an item catalogue and the rule list.
 *
 * <p>Layout is built around a fixed header (tabs) and footer (status + actions), so lists never
 * overlap buttons or status text. All content sizes are computed in {@link #init()}.</p>
 */
public class EnvyScreen extends Screen implements MenuScreen {
    private final ScrollBars scrollBars = new ScrollBars();
    private static final int MACRO_ROW=44;
    private int frameMouseX, frameMouseY;
    private int ownProfileX,ownProfileW;
    public void openSettings(){switchTab(8);}
    public void openPlayer(String nick){historySearch=nick;historyMode=0;switchTab(7);fetchHistory();}
	private static final String[] TABS = {"Наказания", "Макросы", "Предметы", "Права", "Правила", "Логи", "Онлайн", "История", "Настройки"};
	private static final String[] PUNISH_TYPES = PunishmentTypes.ALL.stream().map(PunishmentTypes.Type::command).toArray(String[]::new);

	private static final int PAD = 10;
	private static final int TOPBAR_H = 30;               // 60 physical px at GUI scale 2
	private static final int FOOTER_H = 28;
	private static final int PLAYER_ROW = 14;
    private static final int LOG_ROW = 38;
    private static final int ONLINE_ROW = 26;
	private static final int RULE_ROW = 14;
	private static final int CELL = 20;
	private static final int RANK_ROW = 26;
	private static final int RANK_COMMAND_ROW = 16;

	// Envyworld render stack with EnvyWorld's neutral glass / white accent palette.
	private static final int BG = MenuTheme.PALETTE.backgroundPrimary();
	private static final int TOPBAR = 0x70000000;          // subtle tint over the shared translucent backdrop
	private static final int PANEL = MenuTheme.PALETTE.backgroundControl();
	private static final int PANEL_LIGHT = MenuTheme.PALETTE.backgroundHover();
	private static final int BORDER = MenuTheme.PALETTE.borderPrimary();
	private static final int SEPARATOR = MenuTheme.PALETTE.separator();
	private static final int TEXT = MenuTheme.PALETTE.textPrimary();
	private static final int MUTED = MenuTheme.PALETTE.textMuted();
	private static final int ACCENT = MenuTheme.PALETTE.accent();
	private static final int ACCENT_DARK = 0x28FFFFFF;     // accent border
	private static final int SELECTED = MenuTheme.PALETTE.accentTint();
	private static final int GREEN = 0xFF4ADE80;
	private static final int RED = MenuTheme.PALETTE.danger();
	private static final int GOLD = 0xFFE2E7EF;

	// Fade animation

	private float fadeProgress = 1f;
    private final UiMotion menuMotion=new UiMotion(0);
    private boolean isClosing;
    private boolean closeReady;


	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int contentX;
	private int contentY;
	private int contentW;
	private int contentH;
	private int footerY;
	private int statusX;
	private int statusY;
	private int statusWidth;
	private int tab;

	// Punishments
	private String targetName = "";
	private String punishType = "ban";
	private boolean punishTypeOpen;
	private int punishTypeX;
	private int punishTypeY;
	private int punishTypeW;
	private String punishDuration = "1h";
	private String punishReason = "";
	private String punishReasonSuffix = "";
	private String playerQuery = "";
	private boolean ruleMode = true;
	private RulePreset selectedRule;
	private int playerScroll;
	private int ruleScroll;
	private int playerListX;
	private int playerListY;
	private int playerListW;
	private int playerListH;
	private int ruleListX;
	private int ruleListY;
	private int ruleListW;
	private int ruleListH;
	private int ruleToggleX;
	private int ruleToggleY;
	private int ruleToggleW;
	private int ruleToggleH;
	private int manualToggleX;
	private int manualToggleW;
	private EditBox playerSearchField;
	private EditBox targetField;
	private EditBox durationField;
	private EditBox reasonField;
	private EditBox reasonSuffixField;
	private Button typeButton;

	// Macros
	private String macroTrigger = "attack";
	private String macroItem = "";
	private String macroCustomItem = "";
	private int macroCustomModel = 0;
	private String macroRadius = "0";
	private String macroCommand = "";
    private String macroName="",macroDescription="";
    private java.util.List<Integer> macroCommandDelays=new java.util.ArrayList<>();
    private int macroKeyCode = -1, macroKeyModifiers;
    private String macroLookDistance = "64";
    private boolean guidedReasonConfirmed;
    private boolean fillingRuleReason;
    private String lastItemClick = "";
    private long lastItemClickAt, deleteItemArmed;
    private String macroKeyMode = "PRESS", macroRepeats = "1", macroDelay = "1000";
	private Binding selectedBind;
	private int bindScroll;
	private int bindListX;
	private int bindListY;
	private int bindListW;
	private int bindListH;
	private int itemBoxX;
	private int itemBoxY;
	private int itemBoxW;
	private int itemBoxH;
	private EditBox macroRadiusField;
	private EditBox macroCommandField;
	private Button macroTriggerButton;

	// Items
	private String itemQuery = "";
	private String selectedItem = "minecraft:diamond_sword";
	private List<Item> filteredItems = new ArrayList<>();
	private int itemScroll;
	private int itemGridX;
	private int itemGridY;
	private int itemGridW;
	private int itemGridH;
	private int itemInfoWidth;
	private EditBox itemSearchField;
	private boolean itemCustomMode;
	private boolean pickBaseMode;
	private String customBaseItem = "minecraft:stick";
	private CustomItemPreset selectedCustomItem;
	private CustomItemPreset customDraft;
	private int customListX;
	private int customListY;
	private int customListW;
	private int customListH;
	private int customScroll;
	private int customEditorX;
	private int customEditorY;
	private int customEditorW;
	private EditBox customNameField;
	private EditBox customLoreField;
	private EditBox customEnchantField;
	private EditBox customModelField;
	private EditBox customCountField;
	private int customPreviewX;
	private int customPreviewY;
	private int customPreviewW;
	private int customPreviewH;
	private String customColor = "LIGHT_PURPLE";
	private boolean customUnbreakable;
	private boolean customGlint = true;
	private Button customColorButton;
	private Button customUnbreakableButton;
	private Button customGlintButton;
	private Button itemModeButton;
	/** Enchantment picker state. */
	private final java.util.List<String> enchantPickerIds = java.util.List.of(
			"sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect",
			"looting", "sweeping_edge", "efficiency", "silk_touch", "unbreaking",
			"fortune", "power", "punch", "flame", "infinity", "protection",
			"fire_protection", "blast_protection", "projectile_protection",
			"respiration", "aqua_affinity", "thorns", "feather_falling",
			"depth_strider", "frost_walker", "soul_speed", "swift_sneak",
			"mending", "loyalty", "impaling", "riptide", "channeling",
			"multishot", "quick_charge", "piercing", "luck_of_the_sea", "lure",
			"density", "breach", "wind_burst", "binding_curse", "vanishing_curse");
	private String enchantPickSelected = "sharpness";
	private int enchantPickScroll;
	private EditBox customEnchantLevelField;
	private int enchantPickerX;
	private int enchantPickerY;
	private int enchantPickerW;
	private int enchantPickerH;

	// Ranks
	private int rankListX;
	private int rankListY;
	private int rankListW;
	private int rankListH;
	private int rankScroll;
	private int rankCommandsX;
	private int rankCommandsY;
	private int rankCommandsW;
	private int rankCommandsH;
	private int rankCommandScroll;
	private int serverCommandsX;
	private int serverCommandsY;
	private int serverCommandsW;
	private int serverCommandsH;
	private int serverCommandScroll;
	private Rank selectedRank;

	// Rules tab
	private int rulesListX;
	private int rulesListY;
	private int rulesListW;
	private int rulesListH;
	private int rulesScroll;

	// Punishment log tab (all three types merged; auto-refresh ~10 s)
	private String logServer = "all";
	private String logSearch = "";
	private int logScroll;
	private int logListX;
	private int logListY;
	private int logListW;
	private int logListH;
	private int logServerX;
	private int logServerY;
	private int logServerW;
    private boolean logServerOpen;
    private int logServerMenuScroll;
    private long navLastFrame;
    private SettingsPanel settingsPanel;
	private final float[] navHover = new float[TABS.length];
	private int logAutoTicks;
	private String logTypeFilter = "all";
	private EditBox logSearchField;
	private PunishLogClient.Row logSelected;
	private int onlineScroll;
	private EditBox onlineSearchField;
	private String onlineSearch = "";
    private int onlineFilter;
    private String lastOnlineNick = "";
    private long lastOnlineClick;
    private int onlineTotal, onlineNearby;
	private EditBox historySearchField;
	private String historySearch = "";
	private java.util.List<PunishLogClient.Row> historyRows = java.util.List.of();
	private String historyState = "idle";
	private int historyScroll;
	private com.envyworld.logs.PunishLogClient.Row lastClickedLogRow;
	private long lastLogClickAt;
	private String profileName = "";
	private PlayerProfileView profileView;
	private int profileCardX, profileCardY, profileCardW, profileCardH;
	private int historyListX, historyListY, historyListW, historyListH;
	private int historyMode;
    private String profileCheckCommand = "dupeip";
    private Button dupeIpButton, ipHistoryButton;
	private int historyRequestId;
	private String historyError = "";
	private boolean historyLimited;

	// Preview
	private int previewX;
	private int previewY;
	private int previewW;
	private int previewH;

	private String statusText = "";
	private int statusColor = MUTED;

	public EnvyScreen() {
		super(Component.literal("EnvyWorld"));
		// The suffix lives in config so it persists between sessions.
		punishReasonSuffix = EnvyWorld.config().punishReasonSuffix;
	}

	@Override
	protected void init() {
		this.clearWidgets();
        isClosing=false;closeReady=false;
		super.init();

		// Edge-to-edge: the menu covers the entire game window.
		panelX = 0;
		panelY = 0;
		panelW = Math.max(320, this.width);
		panelH = Math.max(240, this.height);

		contentX = PAD;
		contentY = TOPBAR_H + 8;
		contentW = panelW - PAD * 2;
		footerY = panelH - FOOTER_H;
		contentH = Math.max(80, footerY - 6 - contentY);
		statusX = contentX;
		statusY = footerY + 9;
		statusWidth = contentW;

		RankDetector.refresh(this.minecraft);
		if (selectedRank == null) {
			selectedRank = RankDetector.current();
			if (selectedRank == null && !RankCatalog.all().isEmpty()) {
				selectedRank = RankCatalog.all().get(0);
			}
		}

		if (macroItem == null || macroItem.isBlank()) {
			macroItem = heldItemId();
		}

		switch (tab) {
			case 0 -> initPunish();
			case 1 -> initMacros();
			case 2 -> initItems();
			case 3 -> initRanks();
			case 4 -> initRules();
			case 5 -> initPunishLog();
			case 6 -> initOnline();
			case 7 -> initHistory();
            default -> initSettings();
		}
	}

	// ------------------------------------------------------------------ layout

	private void initPunish() {
		if (selectedRule == null && !RuleBook.all().isEmpty()) {
			selectedRule = RuleBook.all().get(0);
            if(punishReason.isBlank())punishReason="["+selectedRule.code+"] "+selectedRule.title;
		}

		int leftW = Math.max(220, Math.min(380, contentW * 2 / 5));
		int rightX = contentX + leftW + 10;
		int rightW = contentW - leftW - 10;

		playerListX = contentX;
		playerListY = contentY + 42;
		playerListW = leftW;

		int playerRows = clamp((contentH - 200) / PLAYER_ROW, 5, 10);
		playerListH = PLAYER_ROW * playerRows;

		ruleListX = contentX;
		ruleListY = playerListY + playerListH + 14;
		ruleListW = leftW;
		int rulesTop = ruleListY - contentY;
		ruleListH = Math.max(RULE_ROW, contentH - rulesTop);

		playerSearchField = new UiEditBox(this.font, contentX, contentY, leftW, 20, Component.literal("Поиск игрока"));
		playerSearchField.setMaxLength(32);
		playerSearchField.setValue(playerQuery);
		playerSearchField.setResponder(value -> playerQuery = value);
		playerSearchField.setHint(Component.literal("Поиск по нику, например Bodyaha"));
		addRenderableWidget(playerSearchField);

		targetField = new UiEditBox(this.font, rightX, contentY + 33, rightW, 20, Component.literal("Ник игрока"));
		targetField.setMaxLength(32);
		targetField.setValue(targetName);
		targetField.setResponder(value -> {if(!targetName.equals(value))guidedReasonConfirmed=false;targetName=value;});
		targetField.setHint(Component.literal("Ник или IP, например Bodyaha"));
		addRenderableWidget(targetField);

		durationField = new UiEditBox(this.font, rightX, contentY + 67, 64, 20, Component.literal("Срок"));
		durationField.setMaxLength(12);
		durationField.setValue(punishDuration);
		durationField.setResponder(value -> punishDuration = value);
		durationField.setHint(Component.literal("1h / 7d / perm"));
		addRenderableWidget(durationField);

		punishTypeX = rightX + 72;
		punishTypeY = contentY + 67;
		punishTypeW = rightW - 72;
		typeButton = UiButton.builder(Component.literal(PunishmentTypes.buttonLabel(punishType)), button -> {
			punishTypeOpen = !punishTypeOpen;
			button.setMessage(Component.literal(PunishmentTypes.get(punishType).label() + (punishTypeOpen ? "  ^" : "  v")));
		}).bounds(punishTypeX, punishTypeY, punishTypeW, 20).build();
		addRenderableWidget(typeButton);

		reasonField = new UiEditBox(this.font, rightX, contentY + 101, rightW, 20, Component.literal("Причина"));
		reasonField.setMaxLength(2048);
		reasonField.setValue(punishReason);
		reasonField.setResponder(value -> {punishReason=value;if(!fillingRuleReason)guidedReasonConfirmed=true;});
		reasonField.setHint(Component.literal("Например: [2.1] Оскорбление, насмешка"));
		addRenderableWidget(reasonField);

		reasonSuffixField = new UiEditBox(this.font, rightX, contentY + 139, rightW, 20,
				Component.literal("Есть вопросы? Telegram: @user"));
		reasonSuffixField.setMaxLength(200);
		reasonSuffixField.setValue(punishReasonSuffix);
		reasonSuffixField.setResponder(value -> {
			punishReasonSuffix = value;
			EnvyWorld.config().punishReasonSuffix = value;
			EnvyWorld.saveConfig();
		});
		reasonSuffixField.setHint(Component.literal("Есть вопросы? Telegram: @user"));
		addRenderableWidget(reasonSuffixField);

		ruleToggleX = rightX;
		ruleToggleY = contentY;
		ruleToggleW = 100;
		ruleToggleH = 20;
		manualToggleX = rightX + 104;
		manualToggleW = 100;

		previewX = rightX;
		previewY = contentY + 167;
		previewW = rightW;
		previewH = Math.max(24, contentH - 167);
        String generated=generatedPunishCommand();
        if(!generated.equals(commandBase)){commandBase=generated;commandOverride=null;}
        commandDraftField=new UiEditBox(font,previewX+8,previewY+21,previewW-16,20,Component.literal("Команда"));
        commandDraftField.setMaxLength(4096);commandDraftField.setValue(commandOverride==null?generated:commandOverride);
        commandDraftField.setResponder(v->{if(!syncingCommand){commandBase=generatedPunishCommand();commandOverride=v;guidedReasonConfirmed=true;}});
        addRenderableWidget(commandDraftField);


		int buttonX = panelX + panelW - PAD - 300;
		statusWidth = Math.max(60, buttonX - contentX - 10);

		addRenderableWidget(UiButton.builder(Component.literal("Выдать"), button -> sendPunish())
				.bounds(buttonX, footerY, 120, 20).build());
		addRenderableWidget(UiButton.builder(Component.literal("Разбан"), button -> sendUnban())
				.bounds(buttonX + 126, footerY, 84, 20).build());
		addRenderableWidget(UiButton.builder(Component.literal("Размут"), button -> sendUnmute())
				.bounds(buttonX + 216, footerY, 84, 20).build());
	}

	private void initMacros() {
		int leftW = Math.max(280, Math.min(400, contentW * 45 / 100));
		int rightX = contentX + leftW + 10;
		int rightW = contentW - leftW - 10;

		bindListX = contentX;
		bindListY = contentY + 28;
		bindListW = leftW;
		bindListH = Math.max(MACRO_ROW, contentH - 28);

        addRenderableWidget(UiButton.builder(Component.literal("Импорт"),b->minecraft.setScreen(new PresetExchangeScreen(this,null,selectedBind))).bounds(contentX+leftW-156,contentY,76,20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Экспорт"),b->exportPack(null,selectedBind)).bounds(contentX+leftW-76,contentY,76,20).build());
		macroTriggerButton = UiButton.builder(Component.literal("Триггер: " + triggerLabel(macroTrigger)), button -> {
			macroTrigger = nextMacroTrigger(macroTrigger);
            if(macroTrigger.equals("radius") && macroRadius.equals("0")) macroRadius="5";
			this.init(width, height);
		}).bounds(rightX, contentY + 10, rightW, 20).build();
		addRenderableWidget(macroTriggerButton);

		itemBoxX = rightX;
		itemBoxY = contentY + 44;
		itemBoxW = rightW - 178;
		itemBoxH = 18;

		addRenderableWidget(UiButton.builder(Component.literal("В руке"), button -> {
			macroItem = heldItemId();
			macroCustomItem = "";
			macroCustomModel = 0;
			if (this.minecraft != null && this.minecraft.player != null) {
				CustomItemPreset heldPreset = CustomItems.findPreset(this.minecraft.player.getMainHandItem());
				if (heldPreset != null) {
					macroItem = heldPreset.baseItem;
					macroCustomItem = heldPreset.id;
					macroCustomModel = heldPreset.model;
					setStatus("Свой предмет из руки: " + heldPreset.name, GREEN);
					return;
				}
			}
			setStatus("Предмет из руки: " + macroItem, TEXT);
		}).bounds(rightX + rightW - 174, contentY + 44, 54, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Стереть"),b->{macroItem="any";macroCustomItem="";macroCustomModel=0;init(width,height);}).bounds(rightX+rightW-114,contentY+44,54,20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Выдать"),b->{try{var p=CustomItems.byId(macroCustomItem);var stack=p!=null?CustomItems.build(p,1):new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(macroItem)));setStatus(CustomItems.give(minecraft,stack),TEXT);}catch(Exception e){setStatus("Выберите конкретный предмет",RED);}}).bounds(rightX+rightW-54,contentY+44,54,20).build());

		addRenderableWidget(UiButton.builder(Component.literal("Каталог предметов"), button -> switchTab(2)).bounds(rightX,contentY+66,(rightW-6)/2,20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Описание / команды"),button->minecraft.setScreen(new MacroDetailsScreen(this,macroName,macroDescription,macroCommand,macroCommandDelays,(name,description,commands,delays)->{macroName=name;macroDescription=description;macroCommand=commands;macroCommandDelays=new java.util.ArrayList<>(delays);}))).bounds(rightX+(rightW+6)/2,contentY+66,(rightW-6)/2,20).build());

		macroRadiusField = new UiEditBox(this.font, rightX, contentY + 100, 60, 20, Component.literal("Радиус"));
		macroRadiusField.setMaxLength(5);
		macroRadiusField.setValue(macroTrigger.equals("key") ? macroLookDistance : macroRadius);
		macroRadiusField.setResponder(value -> { if(macroTrigger.equals("key")) macroLookDistance=value; else macroRadius=value; });
		macroRadiusField.setHint(Component.literal("0 = один раз"));
		addRenderableWidget(macroRadiusField);

		macroCommandField = new UiEditBox(this.font, rightX, contentY + 134, rightW-94, 20, Component.literal("Команда"));
		macroCommandField.setMaxLength(256);
		macroCommandField.setValue(macroCommand.contains("\n")?"Набор: "+macroCommand.split("\n").length+" команд":macroCommand);
        macroCommandField.setEditable(!macroCommand.contains("\n"));
		macroCommandField.setResponder(value -> macroCommand = value);
		macroCommandField.setHint(Component.literal("Например: /spawn"));
		addRenderableWidget(macroCommandField);
        addRenderableWidget(UiButton.builder(Component.literal("Ник цели"),b->{if(macroCommand.contains("\n"))return;macroCommandField.insertText("%target%");setFocused(macroCommandField);macroCommandField.setFocused(true);}).bounds(rightX+rightW-88,contentY+134,88,20).build());
        int modeW = Math.max(72, rightW * 2 / 5);
        int repeatW = (rightW - modeW - 12) / 2;
        Button mode = addRenderableWidget(UiButton.builder(Component.literal(macroTrigger.equals("key") || macroTrigger.equals("radius") || macroTrigger.equals("command") ? macroModeLabel() : "Вкл / выкл"), button -> {
            macroKeyMode = switch(macroKeyMode){case "PRESS"->"HOLD";case "HOLD"->"TOGGLE";default->"PRESS";};
            if (!macroKeyMode.equals("PRESS") && macroRepeats.equals("1")) macroRepeats = "0";
            this.init(width, height);
        }).bounds(rightX, contentY + 170, modeW, 20).build());
        mode.active = macroTrigger.equals("key") || macroTrigger.equals("radius") || macroTrigger.equals("command");
        EditBox repeat = new UiEditBox(font, rightX + modeW + 6, contentY + 170, repeatW, 20, Component.literal("0 = без лимита"));
        repeat.setValue(macroRepeats); repeat.setMaxLength(5); repeat.setResponder(value -> macroRepeats = value); addRenderableWidget(repeat);
        EditBox delay = new UiEditBox(font, rightX + modeW + repeatW + 12, contentY + 170, repeatW, 20, Component.literal("Задержка, мс"));
        delay.setValue(macroDelay); delay.setMaxLength(7); delay.setResponder(value -> macroDelay = value); addRenderableWidget(delay);
        repeat.setEditable(mode.active); delay.setEditable(mode.active);

        addRenderableWidget(UiButton.builder(Component.literal("Клавиша: " + macroKeyLabel()),button -> {
            minecraft.setScreen(new KeyCaptureScreen(this,(key,mods)->{macroKeyCode=key;macroKeyModifiers=mods;}));
        }).bounds(rightX,contentY+198,rightW,22).build());
		previewX = rightX;
		previewY = contentY + 232;
		previewW = rightW;
		previewH = Math.max(24, contentH - 232);

		int buttonX = panelX + panelW - PAD - 280;
		statusWidth = Math.max(60, buttonX - contentX - 10);

		addRenderableWidget(UiButton.builder(Component.literal("Новый"), button -> newMacro())
				.bounds(buttonX, footerY, 88, 20).build());
		addRenderableWidget(UiButton.builder(Component.literal("Сохранить"), button -> createMacro())
				.bounds(buttonX + 96, footerY, 88, 20).build());
		addRenderableWidget(UiButton.builder(Component.literal("Удалить"), button -> deleteMacro())
				.bounds(buttonX + 192, footerY, 88, 20).build());
	}

    private void exportPack(CustomItemPreset item,Binding macro){try{var pack=com.envyworld.items.PresetExchange.selection(EnvyWorld.config(),item,macro);if(pack.items.isEmpty()&&pack.macros.isEmpty())pack=com.envyworld.items.PresetExchange.all(EnvyWorld.config());if(pack.items.isEmpty()&&pack.macros.isEmpty()){setStatus("Нет предметов или макросов для экспорта",MUTED);return;}var dir=EnvyWorld.dataDir().resolve("exports");java.nio.file.Files.createDirectories(dir);var file=dir.resolve("presets-"+System.currentTimeMillis()+".json");java.nio.file.Files.writeString(file,com.envyworld.items.PresetExchange.encode(pack));com.envyworld.client.ClipboardFiles.copy(file);setStatus("JSON-файл скопирован  |  можно вставить в Импорт или отправить",GREEN);}catch(Exception e){setStatus("Не удалось экспортировать пакет",RED);}}
    private List<CustomItemPreset> savedItems() {
        String query = itemQuery.toLowerCase(Locale.ROOT);
        return EnvyWorld.config().customItems.stream().filter(preset -> preset != null && (query.isBlank()
                || (preset.name + " " + preset.baseItem + " " + preset.id + " " + preset.author).toLowerCase(Locale.ROOT).contains(query))).toList();
    }

    private void openItemEditor(CustomItemPreset preset) {
        minecraft.setScreen(new ItemEditorScreen(this, preset, stored -> { selectedCustomItem = stored; setStatus("Сохранено: " + stored.name, GREEN); }));
    }
    void refreshSavedSelection() { if(selectedCustomItem!=null)selectedCustomItem=CustomItems.byId(selectedCustomItem.id);customDraft=null; }

    private void initItems() {
        itemCustomMode = false;
        EditBox search = new UiEditBox(font, contentX, contentY, Math.max(80, contentW - 368), 20, Component.literal("Поиск сохранённых предметов"));
        search.setValue(itemQuery); search.setResponder(value -> { itemQuery = value; customScroll = 0; }); addRenderableWidget(search);
        addRenderableWidget(UiButton.builder(Component.literal("Добавить предмет"), button -> openItemEditor(null))
                .bounds(contentX + contentW - 138, contentY, 138, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Импорт"), b -> minecraft.setScreen(new PresetExchangeScreen(this,selectedCustomItem,null))).bounds(contentX+contentW-248,contentY,102,20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Экспорт"),b->exportPack(selectedCustomItem,null)).bounds(contentX+contentW-358,contentY,102,20).build());
        itemGridX = contentX; itemGridY = contentY + 36; itemGridW = contentW; itemGridH = Math.max(CELL, footerY - 26 - itemGridY);
        customListX = itemGridX; customListY = itemGridY; customListW = itemGridW; customListH = itemGridH;
        int actionWidth = (contentW - 24) / 4;
        addRenderableWidget(UiButton.builder(Component.literal("Редактировать"), button -> {
            if (selectedCustomItem == null) setStatus("Выберите предмет", MUTED); else openItemEditor(selectedCustomItem);
        }).bounds(contentX, footerY, actionWidth, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Выдать"), button -> {
            if (selectedCustomItem == null) setStatus("Выберите предмет", MUTED);
            else try { setStatus(CustomItems.give(minecraft, CustomItems.build(selectedCustomItem, shiftDown() ? 64 : selectedCustomItem.count)), TEXT); }
            catch (RuntimeException invalid) { setStatus(invalid.getMessage(), RED); }
        }).bounds(contentX + actionWidth + 8, footerY, actionWidth, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Использовать в макросе"), button -> {
            if (selectedCustomItem == null) { setStatus("Выберите предмет", MUTED); return; }
            macroItem = selectedCustomItem.baseItem; macroCustomItem = selectedCustomItem.id; macroCustomModel = selectedCustomItem.model; switchTab(1);
        }).bounds(contentX + (actionWidth + 8) * 2, footerY, actionWidth, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Удалить"),b->{
            if(selectedCustomItem==null){setStatus("Выберите предмет",MUTED);return;}
            if(System.currentTimeMillis()-deleteItemArmed>3000){deleteItemArmed=System.currentTimeMillis();setStatus("Повторите нажатие «Удалить» для подтверждения",GOLD);return;}
            String id=selectedCustomItem.id;EnvyWorld.config().customItems.removeIf(p->p.id.equals(id));selectedCustomItem=null;customDraft=null;CustomItems.invalidate();EnvyWorld.saveConfig();init(width,height);
        }).bounds(contentX+(actionWidth+8)*3,footerY,actionWidth,20).build());
        statusWidth = contentW; statusY = footerY - 18; previewX = previewY = previewW = previewH = 0;
    }

	private void initCustomItems() {
		// Left: full catalogue of Minecraft items for picking a base.
		// Right: editor fields + preview.
		int searchW = Math.max(120, contentW / 2 - 10);
		itemSearchField = new UiEditBox(this.font, contentX, contentY, searchW, 20,
				Component.literal("Поиск базы"));
		itemSearchField.setMaxLength(48);
		itemSearchField.setValue(itemQuery);
		itemSearchField.setResponder(value -> {
			itemQuery = value;
			rebuildItems();
		});
		itemSearchField.setHint(Component.literal("Поиск предмета: алмаз / diamond"));
		addRenderableWidget(itemSearchField);
		rebuildItems();

		int gridW = Math.max(CELL * 8, contentW / 2);
		itemGridX = contentX;
		itemGridY = contentY + 24;
		itemGridW = gridW;
		itemGridH = Math.max(CELL, ((contentH - 24) / CELL) * CELL);

		customEditorX = itemGridX + itemGridW + 10;
		customEditorY = contentY + 24;
		customEditorW = Math.max(180, contentW - itemGridW - 10);

		CustomItemPreset preset = customDraft != null ? customDraft : selectedCustomItem;
		if (customBaseItem == null || customBaseItem.isBlank()) {
			customBaseItem = selectedItem;
		}

		String name = preset != null ? preset.name : defaultCustomName();
		String lore = preset != null ? preset.lore : "";
		String enchantments = preset != null ? preset.enchantments : "";
		String model = String.valueOf(preset != null ? preset.model : defaultCustomModel());
		String count = String.valueOf(preset != null ? preset.count : 1);
		customColor = preset != null ? preset.color : "LIGHT_PURPLE";
		customUnbreakable = preset != null && preset.unbreakable;
		customGlint = preset == null || preset.glint;

		customNameField = new UiEditBox(this.font, customEditorX, customEditorY + 10, customEditorW, 20,
				Component.literal("Название предмета"));
		customNameField.setMaxLength(64);
		customNameField.setValue(name == null ? "" : name);
		customNameField.setHint(Component.literal("Например: Меч стража"));
		addRenderableWidget(customNameField);

		customLoreField = new UiEditBox(this.font, customEditorX, customEditorY + 44, customEditorW, 20,
				Component.literal("Лор, разделяйте строки |"));
		customLoreField.setMaxLength(256);
		customLoreField.setValue(lore == null ? "" : lore);
		customLoreField.setHint(Component.literal("Первая строка | Вторая строка"));
		addRenderableWidget(customLoreField);

		customEnchantField = new UiEditBox(this.font, customEditorX, customEditorY + 78, customEditorW, 20,
				Component.literal("sharpness:5, unbreaking:3"));
		customEnchantField.setMaxLength(256);
		customEnchantField.setValue(enchantments == null ? "" : enchantments);
		customEnchantField.setHint(Component.literal("sharpness:5, unbreaking:3"));
		addRenderableWidget(customEnchantField);

		// Quick enchantment picker: click a name to add/refresh it in the field.
		int pickerH = 50;
		enchantPickerX = customEditorX;
		enchantPickerY = customEditorY + 98;
		enchantPickerW = customEditorW;
		enchantPickerH = pickerH;
		customEnchantLevelField = new UiEditBox(this.font,
				customEditorX + customEditorW - 60, customEditorY + 150, 60, 14,
				Component.literal("уровень"));
		customEnchantLevelField.setMaxLength(3);
		customEnchantLevelField.setValue("5");
		addRenderableWidget(customEnchantLevelField);

		customModelField = new UiEditBox(this.font, customEditorX, customEditorY + 168, 54, 20,
				Component.literal("Модель"));
		customModelField.setMaxLength(1);
		customModelField.setValue(model);
		customModelField.setHint(Component.literal("0 - 9"));
		addRenderableWidget(customModelField);

		customCountField = new UiEditBox(this.font, customEditorX + 62, customEditorY + 168, 54, 20,
				Component.literal("Кол-во"));
		customCountField.setMaxLength(3);
		customCountField.setValue(count);
		customCountField.setHint(Component.literal("1 - 64"));
		addRenderableWidget(customCountField);

		customColorButton = UiButton.builder(Component.literal("Цвет: " + customColor.toLowerCase(Locale.ROOT)),
				button -> {
					customColor = nextCustomColor(customColor);
					button.setMessage(Component.literal("Цвет: " + customColor.toLowerCase(Locale.ROOT)));
				}).bounds(customEditorX + 124, customEditorY + 168, Math.max(60, customEditorW - 124), 20).build();
		addRenderableWidget(customColorButton);

		int half = Math.max(70, (customEditorW - 8) / 2);
		customUnbreakableButton = UiButton.builder(
				Component.literal("Неразрушимый: " + (customUnbreakable ? "да" : "нет")),
				button -> {
					customUnbreakable = !customUnbreakable;
					button.setMessage(Component.literal("Неразрушимый: " + (customUnbreakable ? "да" : "нет")));
				}).bounds(customEditorX, customEditorY + 190, half, 20).build();
		addRenderableWidget(customUnbreakableButton);

		customGlintButton = UiButton.builder(
				Component.literal("Блеск: " + (customGlint ? "да" : "нет")),
				button -> {
					customGlint = !customGlint;
					button.setMessage(Component.literal("Блеск: " + (customGlint ? "да" : "нет")));
				}).bounds(customEditorX + half + 8, customEditorY + 190,
						Math.max(60, customEditorW - half - 8), 18).build();
		addRenderableWidget(customGlintButton);

		// Base is chosen by clicking the catalogue on the left — show it as a
		// read-only label instead of a button.
        Button baseInfo = UiButton.builder(Component.literal("База: " + shortItemId(customBaseItem)), button -> {})
                .bounds(customEditorX, customEditorY + 214, customEditorW, 20).build();
        baseInfo.active = false;
        addRenderableWidget(baseInfo);

		customPreviewX = customEditorX;
		customPreviewY = customEditorY + 236;
		customPreviewW = customEditorW;
		customPreviewH = Math.max(24, contentH - 272);

		int buttonX = panelX + panelW - PAD - 448;
		statusWidth = Math.max(60, buttonX - contentX - 10);
		statusY = footerY + 9;

		addRenderableWidget(UiButton.builder(Component.literal("Новый"), button -> newCustomItem())
				.bounds(buttonX, footerY, 80, 20).build());
		addRenderableWidget(UiButton.builder(Component.literal("Сохранить"), button -> saveCustomItem())
				.bounds(buttonX + 88, footerY, 88, 20).build());
		addRenderableWidget(UiButton.builder(Component.literal("Выдать"), button -> giveCustomItem())
				.bounds(buttonX + 184, footerY, 80, 20).build());
		addRenderableWidget(UiButton.builder(Component.literal("В макрос"), button -> bindCustomItemToMacro())
				.bounds(buttonX + 272, footerY, 88, 20).build());
		addRenderableWidget(UiButton.builder(Component.literal("Удалить"), button -> deleteCustomItem())
				.bounds(buttonX + 368, footerY, 80, 20).build());
	}

	private void initRules() {
		rulesListX = contentX;
		rulesListY = contentY;
		rulesListW = contentW;
		rulesListH = Math.max(16, (contentH / 16) * 16);
		statusWidth = contentW;
		statusY = footerY + 9;
		previewX = 0;
		previewY = 0;
		previewW = 0;
		previewH = 0;
	}

	private void initPunishLog() {
		// Controls: search | server | refresh  (one row at top)
		int controlY = contentY;
		int searchW = Math.max(140, contentW / 3);
		logSearchField = new UiEditBox(this.font, contentX, controlY, searchW, 20,
				Component.literal("Поиск"));
		logSearchField.setValue(logSearch);
		logSearchField.setHint(Component.literal("Ник или причина…"));
		logSearchField.setResponder(value -> {
            logSearch = value;
            statusText = "";
            logScroll = 0;
            logSelected = null;
            PunishLogClient.request(value, logServer);
        });
		addRenderableWidget(logSearchField);

		logServerX = contentX + searchW + 6;
		logServerY = controlY;
		logServerW = Math.min(160, contentW - searchW - 130);
		int refreshX = logServerX + logServerW + 6;
		int refreshW = Math.max(50, contentX + contentW - refreshX);

		addRenderableWidget(UiButton.builder(Component.literal("Обновить"), button -> {
			PunishLogClient.refreshAll(logSearch, logServer);
		}).bounds(refreshX, controlY, refreshW, 20).build());

		logListX = contentX;
		logListY = controlY + 48;
		logListW = contentW;
		logListH = Math.max(16, footerY - 6 - logListY);
		statusWidth = contentW;
		statusY = footerY + 9;
		previewX = 0;
		previewY = 0;
		previewW = 0;
		previewH = 0;

		PunishLogClient.request(logSearch, logServer);
	}

	private void initOnline() {
		int searchW = Math.max(140, contentW / 3);
		onlineSearchField = new UiEditBox(this.font, contentX, contentY, searchW, 20,
				Component.literal("Поиск игроков"));
		onlineSearchField.setValue(onlineSearch);
		onlineSearchField.setHint(Component.literal("Фильтр ника…"));
		onlineSearchField.setResponder(value -> { onlineSearch = value; onlineScroll = 0; });
		addRenderableWidget(onlineSearchField);
		addRenderableWidget(UiButton.builder(Component.literal("Сканировать"), button -> {
			com.envyworld.logs.OnlineScanner.rescanNow();
		}).bounds(contentX + searchW + 6, contentY, 100, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Отслеживать ник"), button -> {
            String nick = onlineSearch.trim();
            if (!nick.matches("[A-Za-z0-9_]{1,16}")) { setStatus("Введите полный ник для отслеживания", RED); return; }
            boolean watched = com.envyworld.logs.OnlineWatch.toggle(nick);
            setStatus(nick + (watched ? " добавлен в отслеживаемые" : " больше не отслеживается"), TEXT);
            HudNotices.add(nick, watched ? "Добавлен в отслеживаемые" : "Отслеживание отключено", 0xFFB5D5FA);
        }).bounds(contentX + searchW + 112, contentY, Math.min(118, contentW - searchW - 112), 20).build());
		statusWidth = contentW;
		statusY = footerY + 9;
		previewX = previewY = previewW = previewH = 0;
	}

    private void initSettings() {
        addRenderableWidget(UiButton.builder(Component.literal("Чат и команды"),b->minecraft.setScreen(new SessionChatScreen(this))).bounds(contentX+contentW-216,contentY,110,20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Telegram"),b->minecraft.setScreen(new TelegramScreen(this))).bounds(contentX+contentW-100,contentY,100,20).build());
        settingsPanel = new SettingsPanel(contentX, contentY+28, contentW, contentH-28,
                widget -> addRenderableWidget(widget), () -> minecraft.setScreen(new HudEditorScreen(this)));
        previewX = previewY = previewW = previewH = 0;
    }

	private void initHistory() {
        int searchW = Math.max(80, Math.min(260, contentW - 188));
        historySearchField = new UiEditBox(this.font, contentX, contentY, searchW, 20,
                Component.literal("Ник игрока, например Bodyaha"));
        historySearchField.setValue(historySearch);
        historySearchField.setMaxLength(32);
        historySearchField.setResponder(value -> historySearch = value);
        addRenderableWidget(historySearchField);
        addRenderableWidget(UiButton.builder(Component.literal("Открыть профиль"), button -> fetchHistory())
                .bounds(contentX + searchW + 6, contentY, 102, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("К логам"), button -> switchTab(5))
                .bounds(contentX + contentW - 68, contentY, 68, 20).build());
        profileCardX = contentX;
        profileCardY = contentY + 28;
        profileCardW = clamp(contentW / 4, 132, 210);
        profileCardH = Math.max(100, footerY - profileCardY - 8);
        historyListX = profileCardX + profileCardW + 12;
        historyListY = profileCardY + 30;
        historyListW = contentW - profileCardW - 12;
        historyListH = Math.max(32, footerY - historyListY - 8);
        if (!profileName.isBlank()) {
            if (profileView == null || !profileView.nick().equalsIgnoreCase(profileName)) profileView = PlayerProfileView.cached(profileName);
            addRenderableWidget(profileView.createWidget(profileCardX + 8, profileCardY + 37,
                    profileCardW - 16, Math.max(40, profileCardH - 80)));
        }
        if (historyMode == 2) {
            int buttonWidth = (historyListW - 30) / 2;
            dupeIpButton = addRenderableWidget(UiButton.builder(Component.literal("Аккаунты с IP  |  /dupeip"), button -> runProfileCheck("dupeip"))
                    .bounds(historyListX + 10, historyListY + 10, buttonWidth, 20)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Доступно персоналу и донату Президент"))).build());
            ipHistoryButton = addRenderableWidget(UiButton.builder(Component.literal("История IP  |  /iphistory"), button -> runProfileCheck("iphistory"))
                    .bounds(historyListX + 20 + buttonWidth, historyListY + 10, buttonWidth, 20)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Доступно персоналу; права проверяет сервер"))).build());
            dupeIpButton.active = minecraft.getConnection() != null && !profileName.isBlank() && RankDetector.canProfileCheck("dupeip");
            ipHistoryButton.active = minecraft.getConnection() != null && !profileName.isBlank() && RankDetector.canProfileCheck("iphistory");
        }
        statusWidth = contentW;
        statusY = footerY + 9;
        previewX = previewY = previewW = previewH = 0;
    }

    private void runProfileCheck(String command) {
        profileCheckCommand = command;
        historyScroll = 0;
        setStatus(ProfileChecks.request(profileName, command), TEXT);
    }
	private void fetchHistory() {
        String nick = historySearch == null ? "" : historySearch.trim();
        if (!nick.matches("[A-Za-z0-9_]{1,32}")) {
            setStatus("Введите ник игрока без пробелов", RED);
            return;
        }
        statusText = "";
        int request = ++historyRequestId;
        profileName = nick;
        profileView = PlayerProfileView.cached(nick);
        historyScroll = 0;
        historyState = "loading";
        historyError = "";
        historyRows = PunishLogClient.snapshot(nick, "all").rows();
        historyLimited = false;
        if (tab == 7) init(this.width, this.height);
        PunishLogClient.request(nick, "all").thenAccept(result -> this.minecraft.execute(() -> {
            if (request != historyRequestId) return;
            historyRows = result.rows();
            historyState = result.state();
            historyError = result.error();
            historyLimited = result.limited();
        }));
    }
	private void initRanks() {
		// Current rank banner: top strip
		// Left: rank list, Right: rank commands grouped + server commands
		int BANNER_H = 4;

		rankListX = contentX;
		rankListY = contentY + BANNER_H + 8;
		rankListW = Math.max(210, Math.min(340, contentW * 2 / 7));
		rankListH = Math.max(RANK_ROW * 2, contentH - BANNER_H - 8);

		int rightX = rankListX + rankListW + 8;
		int rightW = Math.max(200, contentW - rankListW - 8);

		rankCommandsX = rightX;
		rankCommandsY = rankListY;
		rankCommandsW = rightW;
		// Split right side: top 55% commands, bottom 45% server
		int availableH = contentH - BANNER_H - 8;
		rankCommandsH = Math.max(RANK_COMMAND_ROW * 3, (availableH - 22) * 55 / 100);

		serverCommandsX = rightX;
		serverCommandsY = rankCommandsY + rankCommandsH + 22;
		serverCommandsW = rightW;
		serverCommandsH = Math.max(RANK_COMMAND_ROW * 2, contentH - (serverCommandsY - contentY));

		statusWidth = Math.max(40,contentW-240);
		statusY = footerY + 9;

		// Rank banner buttons (top-right of banner)
		int btnY = footerY;
		addRenderableWidget(UiButton.builder(Component.literal("⟳ Авто"), button -> {
			RankDetector.clearOverride();
			RankDetector.refresh(this.minecraft);
			selectedRank = RankDetector.current();
			if (selectedRank == null && !RankCatalog.all().isEmpty()) {
				selectedRank = RankCatalog.all().get(0);
			}
			setStatus("Автоопределение ранга обновлено: " + RankDetector.displayName(), GREEN);
			this.init(this.width, this.height);
		}).bounds(contentX + contentW - 226, btnY, 106, 20).build());

		addRenderableWidget(UiButton.builder(Component.literal("✔ Закрепить"), button -> {
			if (selectedRank == null) {
				setStatus("Выберите ранг в списке слева", RED);
				return;
			}
			RankDetector.setOverride(selectedRank.id);
			selectedRank = RankDetector.current();
			setStatus("Ранг закреплён вручную: " + (selectedRank != null ? selectedRank.name : "?"), GREEN);
			this.init(this.width, this.height);
		}).bounds(contentX + contentW - 116, btnY, 116, 20).build());
	}

	// ------------------------------------------------------------------ render

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Screen renders a vanilla menu texture before render() unless we replace it.
		// Capture the unobscured world for Kawase blur; draw one dark tint in render().
		this.minecraft.gui.renderDeferredSubtitles();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		UiRenderer.beginFrame();
        scrollBars.beginFrame();
        frameMouseX = mouseX; frameMouseY = mouseY;
		graphics = new UiGraphics(graphics, mouseX, mouseY);
        float transition=menuMotion.update(isClosing?0:1);
        if(isClosing&&transition<=.005f){closeReady=true;transition=0;}
        UiRenderer.menuTransition(transition,isClosing);
        // Internal layers stay opaque relative to each other; the completed menu fades once.
        fadeProgress=1;

        HudSurface.fullscreen(this.width,this.height);

		// Navigation uses the same Envyworld render queue as all other controls.
		drawTopbar(graphics, mouseX, mouseY);

		switch (tab) {
			case 0 -> renderPunish(graphics, mouseX, mouseY);
			case 1 -> renderMacros(graphics, mouseX, mouseY);
			case 2 -> renderItems(graphics, mouseX, mouseY);
			case 3 -> renderRanks(graphics, mouseX, mouseY);
			case 4 -> renderRules(graphics, mouseX, mouseY);
			case 5 -> renderPunishLog(graphics, mouseX, mouseY);
			case 6 -> renderOnline(graphics, mouseX, mouseY);
			case 7 -> renderHistory(graphics, mouseX, mouseY);
            default -> settingsPanel.draw(graphics, mouseX, mouseY);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
		if (tab == 0 && punishTypeOpen) renderPunishDropdown(graphics, mouseX, mouseY);

        if (tab == 5 && logServerOpen) renderLogServerDropdown(graphics, mouseX, mouseY);
        if (tab != 6) {
		    drawStatus(graphics);
        }
	}

	private static int applyFade(int color, float fade) {
		int alpha = (color >> 24) & 0xFF;
		return (Math.round(alpha * fade) << 24) | (color & 0xFFFFFF);
	}

	// Navigation layout; Envyworld handles all rendering.

	private final int[] navHitX = new int[TABS.length];
	private final int[] navHitW = new int[TABS.length];
	private final int[] navHitY = new int[TABS.length];
	private final int[] navHitH = new int[TABS.length];

	private void drawTopbar(GuiGraphics graphics, int mouseX, int mouseY) {
		int barY = panelY;
		int barH = TOPBAR_H;

		// Translucent bar over the background blur.
		graphics.fill(panelX, barY, panelX + panelW, barY + barH, applyFade(TOPBAR, fadeProgress));

		// Navigation geometry in GUI coordinates.
		int linkH = barH;
		int linkY = barY;
		// MSDF cap height ~7px; center that inside the 20px link.
        float fontY = UiFont.centeredY(linkY, linkH, UiFont.SIZE);
        long now = System.nanoTime();
        float dt = navLastFrame == 0 ? 1f / 60 : Math.min(.05f, (now - navLastFrame) / 1_000_000_000f);
        navLastFrame = now;
        float easing = 1 - (float) Math.exp(-dt * 18);
		int[] widths = new int[TABS.length];
		int totalWidth = 0;
		for (int i = 0; i < TABS.length; i++) {
			widths[i] = UiFont.width(TABS[i]) + 16;
			totalWidth += widths[i];
		}
		totalWidth += (TABS.length - 1) * 2;
		String ownNick=minecraft.getUser().getName();ownProfileW=Math.min(112,UiFont.width(ownNick)+34);ownProfileX=panelX+panelW-ownProfileW-8;
        int x=panelX+Math.max(4,(panelW-ownProfileW-20-totalWidth)/2);

		for (int i = 0; i < TABS.length; i++) {
			String label = TABS[i];
			int linkW = widths[i];
			navHitX[i] = x;
			navHitW[i] = linkW;
			navHitY[i] = linkY;
			navHitH[i] = linkH;

			boolean active = i == tab;
			boolean hover = mouseX >= x && mouseX < x + linkW
					&& mouseY >= linkY && mouseY < linkY + linkH;

            navHover[i] += ((hover ? 1 : 0) - navHover[i]) * easing;
            if(active)UiGraphics.rounded(x,barY+4,linkW,barH-8,6,0xC0242424);
            if(navHover[i]>.01f)UiGraphics.rounded(x,barY+4,linkW,barH-8,6,applyFade(0xE0303030,navHover[i]));

			int textColor = active || hover ? 0xFFF1F2F4 : 0xFFD0D2D6;
			UiFont.draw(graphics, label, x + 8, fontY, applyFade(textColor, fadeProgress));
			x += linkW + 2;
		}
        boolean profileHover=inRect(mouseX,mouseY,ownProfileX,linkY,ownProfileW,linkH);
        if(profileHover)UiGraphics.rounded(ownProfileX,barY,ownProfileW,barH,0,0x80181818);
        PlayerFaceRenderer.draw(graphics,PlayerProfileView.cached(ownNick).skin(),ownProfileX+5,barY+(barH-14)/2,14);
        UiFont.draw(graphics,truncate(ownNick,ownProfileW-26),ownProfileX+23,fontY,TEXT);
        UiGraphics.rounded(panelX,barY+barH-.5f,panelW,.5f,0,0x20FFFFFF);
    }

	private void renderPunish(GuiGraphics graphics, int mouseX, int mouseY) {
		label(graphics, "Игроки", playerListX, playerListY - 12);
		label(graphics, "Правила", ruleListX, ruleListY - 12);

		drawPlayerList(graphics, mouseX, mouseY);
		drawRuleList(graphics, mouseX, mouseY);

		drawToggle(graphics, ruleToggleX, ruleToggleY, ruleToggleW, ruleToggleH, "По правилу", ruleMode, mouseX, mouseY);
		drawToggle(graphics, manualToggleX, ruleToggleY, manualToggleW, ruleToggleH, "Вручную", !ruleMode, mouseX, mouseY);

		int rightX = previewX;
		int rightW = previewW;

		String rankText = "Ранг: " + RankDetector.displayName();
		if (rightW - 220 > UiFont.width(rankText) + 12) {
			UiFont.draw(graphics, rankText, rightX + rightW - UiFont.width(rankText), contentY + 4, GREEN);
		}

		label(graphics, "Цель", rightX, contentY + 22);
		label(graphics, "Срок", rightX, contentY + 57);
		label(graphics, "Тип", rightX + 72, contentY + 57);
		label(graphics, "Причина", rightX, contentY + 91);
		label(graphics, "Добавлять текст в конце причины", rightX, contentY + 129);
		boolean instant = ruleMode && selectedRule != null
				? selectedRule.action == RuleAction.WARN || selectedRule.action == RuleAction.KICK
				: !PunishmentTypes.get(punishType).timed();
		durationField.setEditable(!instant);
		durationField.active = !instant;

		String headline;
		if (ruleMode && selectedRule != null) {
			headline = "Команда  |  " + ruleActionName(selectedRule.action) + "  |  [" + selectedRule.code + "]";
		} else if (ruleMode) {
			headline = "Правило не выбрано";
		} else {
			headline = "Команда  |  " + punishType.toUpperCase(Locale.ROOT);
		}

		graphics.fill(previewX,previewY,previewX+previewW,previewY+previewH,PANEL);
        graphics.renderOutline(previewX,previewY,previewW,previewH,BORDER);
        UiFont.draw(graphics,headline,previewX+8,previewY+7,0xFFD6E0ED);
        String generated=generatedPunishCommand();
        if(!generated.equals(commandBase)){commandBase=generated;commandOverride=null;syncingCommand=true;try{commandDraftField.setValue(generated);}finally{syncingCommand=false;}}
        UiFont.draw(graphics,"Редактируемая команда  |  флаги можно дописать в конце: -s",previewX+8,previewY+48,6,MUTED);

	}

	private void renderMacros(GuiGraphics graphics, int mouseX, int mouseY) {
        int rightX = itemBoxX, rightW = previewW, modeW = Math.max(72, rightW * 2 / 5), repeatW = (rightW - modeW - 12) / 2;
        label(graphics, "Режим запуска", rightX, contentY + 158);
        label(graphics, "Повторы", rightX + modeW + 6, contentY + 158);
        label(graphics, "Пауза, мс", rightX + modeW + repeatW + 12, contentY + 158);
		label(graphics, "Макросы (локально)", bindListX, contentY);
		drawBindList(graphics, mouseX, mouseY);

		label(graphics, "Триггер", rightX, contentY);
		label(graphics, "Предмет", rightX, contentY + 34);

		graphics.fill(itemBoxX, itemBoxY, itemBoxX + itemBoxW, itemBoxY + itemBoxH, applyFade(PANEL, fadeProgress));
		graphics.renderOutline(itemBoxX, itemBoxY, itemBoxW, itemBoxH, applyFade(BORDER, fadeProgress));
		UiFont.draw(graphics, truncate(macroItemLabel(), itemBoxW - 8), itemBoxX + 5, itemBoxY + 5, macroCustomItem.isBlank() ? TEXT : GOLD);

		label(graphics, macroTrigger.equals("key") ? "Дальность" : "Радиус", rightX, contentY + 90);
        macroRadiusField.active=!macroTrigger.equals("command");macroRadiusField.setEditable(!macroTrigger.equals("command"));
		UiFont.draw(graphics, truncate(macroTrigger.equals("key") ? "%target% — игрок под прицелом" : macroTrigger.equals("projectile") ? "Свой снаряд  |  %target% — попадание" : macroTrigger.equals("radius") ? "Все игроки в радиусе" : macroTrigger.equals("command") ? "Команда без наведения" : "%target% — ник при ударе",rightW-70), rightX + 68, contentY + 104, MUTED);
		label(graphics, "Команда", rightX, contentY + 124);

		drawPreview(graphics, previewX, previewY, previewW, previewH,
				triggerLabel(macroTrigger) + ": " + macroKeyLabel()+(selectedBind!=null && selectedBind.author!=null && !selectedBind.author.isBlank()?"  Автор: "+selectedBind.author:""), macroPreview());
	}

	private void renderItems(GuiGraphics graphics, int mouseX, int mouseY) {
		if (itemCustomMode) {
			renderCustomItems(graphics, mouseX, mouseY);
			return;
		}

		// Default tab — the user's saved custom items as an icon grid, just
		// like the catalogue. Click a cell to select it.
		List<CustomItemPreset> presets = savedItems();
        int columns = Math.max(1, (itemGridW - 14) / CELL);
		int rows = Math.max(1, (itemGridH + CELL - 1) / CELL + 1);
        int total = presets.size();
        int totalHeight = (total + columns - 1) / columns * CELL;
        int maxScroll = Math.max(0, totalHeight - itemGridH);
		customScroll = clamp(customScroll, 0, maxScroll);

		label(graphics, "Мои предметы (" + total + ")", itemGridX, itemGridY - 12);
		graphics.fill(itemGridX, itemGridY, itemGridX + itemGridW, itemGridY + itemGridH,
				applyFade(PANEL, fadeProgress));

		graphics.enableScissor(itemGridX, itemGridY, itemGridX + itemGridW - 6, itemGridY + itemGridH);

		if (total == 0) {
			String emptyText = "Пока пусто. Нажмите «Добавить предмет».";
			UiFont.draw(graphics, emptyText,
					itemGridX + (itemGridW - UiFont.width(emptyText)) / 2.0f,
					itemGridY + itemGridH / 2.0f - 4.0f, MUTED);
		}

		int startIndex = customScroll / CELL * columns;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < columns; col++) {
				int index = startIndex + row * columns + col;
				if (index >= total) break;
				CustomItemPreset preset = presets.get(index);
				if (preset == null) continue;

				int cellX = itemGridX + col * CELL;
				int cellY = itemGridY + row * CELL - (itemCustomMode ? 0 : customScroll % CELL);
				boolean hover = mouseX >= cellX && mouseX < cellX + CELL
                        && mouseY >= cellY && mouseY < cellY + CELL && mouseY >= itemGridY && mouseY < itemGridY + itemGridH;

				if (preset == selectedCustomItem) {
					graphics.fill(cellX + 1, cellY + 1, cellX + CELL - 1, cellY + CELL - 1,
							applyFade(SELECTED, fadeProgress));
					graphics.renderOutline(cellX + 1, cellY + 1, CELL - 2, CELL - 2,
							applyFade(ACCENT, fadeProgress));
				} else if (hover) {
					graphics.fill(cellX + 1, cellY + 1, cellX + CELL - 1, cellY + CELL - 1,
							0x16FFFFFF);
				} else {
					graphics.renderOutline(cellX + 1, cellY + 1, CELL - 2, CELL - 2,
							applyFade(BORDER, fadeProgress));
				}

				ItemStack stack;
                try { stack = CustomItems.build(preset, 1); }
                catch (RuntimeException invalid) { stack = new ItemStack(net.minecraft.world.item.Items.BARRIER); }
				graphics.renderItem(stack, cellX + 2, cellY + 2);
				if (hover) {
					graphics.setTooltipForNextFrame(this.font, CustomItems.describe(preset, stack), Optional.empty(), mouseX, mouseY);
				}
			}
		}

		graphics.disableScissor();
		drawScrollbar(graphics, itemGridX, itemGridY, itemGridW, itemGridH,
				customScroll, itemGridH, totalHeight);
	}

	private void renderCustomItems(GuiGraphics graphics, int mouseX, int mouseY) {
		// Left: catalogue grid. Right: editor.
		drawItemGrid(graphics, mouseX, mouseY);
		label(graphics, "Название", customEditorX, customEditorY);
		label(graphics, "Лор (| = новая строка)", customEditorX, customEditorY + 34);
		label(graphics, "Зачарования (например sharpness:5, unbreaking:3)", customEditorX, customEditorY + 68);
		label(graphics, "Быстрый выбор: клик = добавить с уровнем из поля →", customEditorX, customEditorY + 92);
		label(graphics, "Модель / Количество / Цвет", customEditorX, customEditorY + 156);
		label(graphics, "Флаги", customEditorX, customEditorY + 180);
		label(graphics, "База предмета", customEditorX, customEditorY + 204);

		// Enchantment picker panel
		graphics.fill(enchantPickerX, enchantPickerY, enchantPickerX + enchantPickerW,
				enchantPickerY + enchantPickerH, applyFade(PANEL, fadeProgress));
		graphics.renderOutline(enchantPickerX, enchantPickerY, enchantPickerW, enchantPickerH,
				applyFade(BORDER, fadeProgress));
		graphics.enableScissor(enchantPickerX, enchantPickerY,
				enchantPickerX + enchantPickerW, enchantPickerY + enchantPickerH);
		int rowH = 10;
		int visiblePicker = enchantPickerH / rowH;
		enchantPickScroll = clamp(enchantPickScroll, 0, Math.max(0, enchantPickerIds.size() - visiblePicker));
		for (int i = 0; i < visiblePicker; i++) {
			int idx = enchantPickScroll + i;
			if (idx >= enchantPickerIds.size()) break;
			String id = enchantPickerIds.get(idx);
			int rowY = enchantPickerY + 2 + i * rowH;
			boolean hover = mouseX >= enchantPickerX && mouseX < enchantPickerX + enchantPickerW
					&& mouseY >= rowY && mouseY < rowY + rowH;
			if (id.equals(enchantPickSelected)) {
				graphics.fill(enchantPickerX + 1, rowY, enchantPickerX + enchantPickerW - 1, rowY + rowH,
						0x22B07CFF);
			} else if (hover) {
				graphics.fill(enchantPickerX + 1, rowY, enchantPickerX + enchantPickerW - 1, rowY + rowH,
						0x14FFFFFF);
			}
			int vanilla = vanillaMax(id);
			String labelTxt = id + "  (" + vanilla + (vanilla < 255 ? " - 255" : "") + ")";
			UiFont.draw(graphics, labelTxt, enchantPickerX + 6, rowY + 1, vanilla > 1 ? TEXT : MUTED);
		}
		graphics.disableScissor();

		graphics.fill(customPreviewX, customPreviewY, customPreviewX + customPreviewW,
				customPreviewY + customPreviewH, applyFade(PANEL, fadeProgress));
		graphics.renderOutline(customPreviewX, customPreviewY, customPreviewW, customPreviewH, applyFade(BORDER, fadeProgress));
		graphics.fill(customPreviewX + 1, customPreviewY + 1, customPreviewX + 3,
				customPreviewY + customPreviewH - 1, applyFade(ACCENT_DARK, fadeProgress));

		CustomItemPreset preview = draftFromFields();
		ItemStack stack = CustomItems.build(preview, 1);
		graphics.renderItem(stack, customPreviewX + 8, customPreviewY + 8);
		UiFont.draw(graphics, truncate(preview.name, customPreviewW - 34), customPreviewX + 30, customPreviewY + 9, previewColor());

		int textY = customPreviewY + 28;
		if (preview.lore != null && !preview.lore.isBlank()) {
			for (String line : preview.lore.split("\\|")) {
				if (textY > customPreviewY + customPreviewH - 11) {
					break;
				}
				UiFont.draw(graphics, truncate(line.trim(), customPreviewW - 16), customPreviewX + 8, textY, MUTED);
				textY += 10;
			}
		}

		// Enchantment summary: colour-coded vs vanilla max.
		if (preview.enchantments != null && !preview.enchantments.isBlank()
				&& textY < customPreviewY + customPreviewH - 12) {
			String summary = enchantmentSummary(preview.enchantments);
			if (!summary.isBlank()) {
				UiFont.draw(graphics, truncate(summary, customPreviewW - 16), customPreviewX + 8, textY, GOLD);
				textY += 10;
			}
		}

		if (textY < customPreviewY + customPreviewH - 12) {
			UiFont.draw(graphics, "Модели 1..9 используют встроенные текстуры палок", customPreviewX + 8, customPreviewY + customPreviewH - 12, GOLD);;
		}
	}

	private void renderRanks(GuiGraphics graphics, int mouseX, int mouseY) {
		int BANNER_H = 4;

		// --------------------------------------------------------- top banner


		// --------------------------------------------------------- rank list (left)
		List<Rank> ranks = RankCatalog.all();
		int rows = Math.max(1, rankListH / RANK_ROW);
		rankScroll = clamp(rankScroll, 0, Math.max(0, ranks.size() - rows));

		label(graphics, "Ранги сервера", rankListX, rankListY - 12);
		graphics.fill(rankListX, rankListY, rankListX + rankListW, rankListY + rankListH, applyFade(PANEL, fadeProgress));

		graphics.enableScissor(rankListX, rankListY, rankListX + rankListW - 6, rankListY + rankListH);

		Rank current = RankDetector.current();
		if (ranks.isEmpty()) {
			UiFont.draw(graphics, "Каталог рангов не загружен", rankListX + 8, rankListY + 8, RED);
		}

		for (int row = 0; row < rows; row++) {
			int index = rankScroll + row;
			if (index >= ranks.size()) {
				break;
			}

			Rank rank = ranks.get(index);
			int rowY = rankListY + row * RANK_ROW;
			boolean hover = mouseX >= rankListX && mouseX < rankListX + rankListW
					&& mouseY >= rowY && mouseY < rowY + RANK_ROW;

			if (rank == selectedRank) {
				drawListRow(rankListX, rankListY, rankListW, rankListH, rowY, RANK_ROW, applyFade(SELECTED, fadeProgress));
				graphics.fill(rankListX + 1, rowY, rankListX + 3, rowY + RANK_ROW - 1, applyFade(ACCENT, fadeProgress));
			} else if (current != null && current.id.equalsIgnoreCase(rank.id)) {
				// Tinted green for current rank
				int banner = rank.argb() & 0x00FFFFFF | 0x28000000;
				drawListRow(rankListX, rankListY, rankListW, rankListH, rowY, RANK_ROW, banner);
			} else if (hover) {
				drawListRow(rankListX, rankListY, rankListW, rankListH, rowY, RANK_ROW, 0x16FFFFFF);
			} else if ((index & 1) == 1) {
				drawListRow(rankListX, rankListY, rankListW, rankListH, rowY, RANK_ROW, 0x08FFFFFF);
			}

			// Colour bar on left edge matching rank color
			int rankColor = rank.argb();
			graphics.fill(rankListX + 1, rowY + 2, rankListX + 4, rowY + RANK_ROW - 2, rankColor);

			UiFont.draw(graphics, truncate(rank.name, rankListW - 72), rankListX + 9, rowY + 4, rankColor);

			String kindBadge = rank.isStaff() ? "ПЕРСОНАЛ" : (rank.isDonation() ? "ДОНАТ" : "ИГРОК");
			int kindColor = rank.isStaff() ? RED : (rank.isDonation() ? GOLD : MUTED);
			UiFont.draw(graphics, kindBadge, rankListX + 9, rowY + 14, kindColor);

			if (current != null && current.id.equalsIgnoreCase(rank.id)) {
				UiFont.draw(graphics, "ВЫ", rankListX + rankListW - 10 - UiFont.width("ВЫ"), UiFont.centeredY(rowY,RANK_ROW,7), GREEN);
			}
		}

		graphics.disableScissor();
		drawScrollbar(graphics, rankListX, rankListY, rankListW, rankListH, rankScroll, rows, ranks.size());

		// --------------------------------------------------------- right side
		if (selectedRank == null) {
			UiFont.draw(graphics, "Выберите ранг слева", rankCommandsX + 8, contentY + BANNER_H + 16, MUTED);
			return;
		}

		label(graphics, "Команды ранга  |  " + selectedRank.name + " (клик — копировать в буфер)",
				rankCommandsX, rankCommandsY - 12);
		drawRankCommands(graphics, mouseX, mouseY);

		label(graphics, "Доступные команды на сервере (" + RankDetector.availableCount() + " шт.)",
				serverCommandsX, serverCommandsY - 12);
		drawServerCommands(graphics, mouseX, mouseY);
	}

	/** Big banner at the top of the Ranks tab showing the detected rank. */
	private void drawRankBanner(GuiGraphics graphics, int bx, int by, int bw, int bh) {
		Rank current = RankDetector.current();
		int rankColor = current != null ? current.argb() : 0xFF6060A0;

		// Background with faint rank colour tint
		graphics.fill(bx, by, bx + bw, by + bh, applyFade(PANEL, fadeProgress));
		graphics.renderOutline(bx, by, bw, bh, applyFade(BORDER, fadeProgress));

		// Left colour bar (thick)
		graphics.fill(bx + 1, by + 1, bx + 6, by + bh - 1, rankColor);

		// Right side subtle gradient strip using rank color with low alpha
		int tint = rankColor & 0x00FFFFFF | 0x22000000;
		graphics.fill(bx + bw - 80, by + 1, bx + bw - 1, by + bh - 1, tint);

		// Main rank name (large)
		String rankName = current != null ? current.name : "Ранг не определён";
		UiFont.draw(graphics, rankName, bx + 14, by + 8, rankColor);

		// Kind badge
		if (current != null) {
			String kind = current.kindName().toUpperCase(Locale.ROOT);
			int kindBg = current.isStaff() ? 0xFF3A0000 : (current.isDonation() ? 0xFF3A2800 : 0xFF1A1A2A);
			int kindFg = current.isStaff() ? RED : (current.isDonation() ? GOLD : MUTED);
			int kw = UiFont.width(kind) + 10;
			graphics.fill(bx + 14, by + 20, bx + 14 + kw, by + 32, kindBg);
			graphics.renderOutline(bx + 14, by + 20, kw, 12, current.isStaff() ? 0xFFAA0000 : (current.isDonation() ? 0xFF806020 : BORDER));
			UiFont.draw(graphics, kind, bx + 18, by + 22, kindFg);
		}

		// Server / game mode detected from the local tab header/footer and sidebar.
		String modeStr = RankDetector.detectedMode();
		UiFont.draw(graphics, "Режим по табу: " + modeStr, bx + 14, by + 44, GOLD);

		// Perks (1 line)
		if (current != null && current.perks != null && !current.perks.isEmpty()) {
			String perks = String.join("  |  ", current.perks);
			UiFont.draw(graphics, truncate(perks, bw - 280), bx + 14, by + 58, MUTED);
		}

		// Detection info (right side of banner)
		int infoX = bx + bw - 264;
		String sourceStr = RankDetector.source().isBlank() ? "—" : RankDetector.source();
		String confStr = RankDetector.confidence() + "%";

		UiFont.draw(graphics, "Определение", infoX, by + 8, MUTED);
		UiFont.draw(graphics, "Источник: " + sourceStr, infoX, by + 22, TEXT);
		UiFont.draw(graphics, "Уверенность: " + confStr, infoX, by + 34, RankDetector.confidence() >= 70 ? GREEN : (RankDetector.confidence() >= 40 ? GOLD : RED));
		String evidence = RankDetector.evidence();
		UiFont.draw(graphics, truncate(evidence, 248), infoX, by + 46, MUTED);
		// Commands available count
		UiFont.draw(graphics, "Команд сервера: " + RankDetector.availableCount(), infoX, by + 58, MUTED);;
	}

	private void drawRankCommands(GuiGraphics graphics, int mouseX, int mouseY) {
		List<RankCommand> commands = RankCatalog.commandsForMode(selectedRank,RankDetector.detectedMode());

		// Build display items: group headers + commands
		List<Object> displayItems = new ArrayList<>(); // String = group header, RankCommand = command
		String lastGroup = null;
		for (RankCommand cmd : commands) {
			String group = cmd.group == null || cmd.group.isBlank() ? "Прочее" : cmd.group;
			if (!group.equals(lastGroup)) {
				displayItems.add(group);
				lastGroup = group;
			}
			displayItems.add(cmd);
		}

		int total = displayItems.size();
		int rows = Math.max(1, rankCommandsH / RANK_COMMAND_ROW);
		rankCommandScroll = clamp(rankCommandScroll, 0, Math.max(0, total - rows));

		graphics.fill(rankCommandsX, rankCommandsY, rankCommandsX + rankCommandsW,
				rankCommandsY + rankCommandsH, applyFade(PANEL, fadeProgress));

		graphics.enableScissor(rankCommandsX, rankCommandsY, rankCommandsX + rankCommandsW - 6,
				rankCommandsY + rankCommandsH);

		if (commands.isEmpty()) {
			UiFont.draw(graphics, "Нет данных о командах для этого ранга", rankCommandsX + 8, rankCommandsY + 6, MUTED);;
		}

		int commandWidth = Math.max(150, rankCommandsW * 45 / 100);
		int cmdIndex = 0; // for alternating row color (only commands, not headers)
		for (int row = 0; row < rows; row++) {
			int index = rankCommandScroll + row;
			if (index >= total) {
				break;
			}

			Object item = displayItems.get(index);
			int rowY = rankCommandsY + row * RANK_COMMAND_ROW;

			if (item instanceof String groupName) {
				// Group header
				graphics.fill(rankCommandsX + 1, rowY, rankCommandsX + rankCommandsW - 1,
						rowY + RANK_COMMAND_ROW - 1, 0xFF1A1A26);
				graphics.fill(rankCommandsX + 1, rowY, rankCommandsX + 3,
						rowY + RANK_COMMAND_ROW - 1, applyFade(ACCENT_DARK, fadeProgress));
				UiIcons.draw("chevron-right",rankCommandsX+9,rowY+RANK_COMMAND_ROW/2f,8,ACCENT);
                UiFont.draw(graphics, groupName, rankCommandsX + 18, UiFont.centeredY(rowY, RANK_COMMAND_ROW - 1, UiFont.SIZE), ACCENT);
			} else if (item instanceof RankCommand command) {
				boolean available = RankDetector.isAvailable(command.name());
				boolean hover = mouseX >= rankCommandsX && mouseX < rankCommandsX + rankCommandsW
						&& mouseY >= rowY && mouseY < rowY + RANK_COMMAND_ROW;

				if (hover) {
					graphics.fill(rankCommandsX + 1, rowY, rankCommandsX + rankCommandsW - 1,
							rowY + RANK_COMMAND_ROW - 1, 0x20FFFFFF);
				} else if ((cmdIndex & 1) == 1) {
					graphics.fill(rankCommandsX + 1, rowY, rankCommandsX + rankCommandsW - 1,
							rowY + RANK_COMMAND_ROW - 1, 0x08FFFFFF);
				}

				// Availability indicator
				int indicatorColor = available ? GREEN : MUTED;
				if(available)UiIcons.check(rankCommandsX+9,rowY+RANK_COMMAND_ROW/2f,indicatorColor);

				String cmdText = command.full();
				UiFont.draw(graphics, truncate(cmdText, commandWidth - 20), rankCommandsX + 18, UiFont.centeredY(rowY, RANK_COMMAND_ROW - 1, UiFont.SIZE), available ? TEXT : 0xFF8C94A2);
				if (command.description != null && !command.description.isBlank()) {
					UiFont.draw(graphics, truncate(command.description, rankCommandsW - commandWidth - 12), rankCommandsX + commandWidth, UiFont.centeredY(rowY, RANK_COMMAND_ROW - 1, UiFont.SIZE), available ? 0xFFB5BDC9 : 0xFF737E8F);
				}
				cmdIndex++;
			}
		}

		graphics.disableScissor();
		drawScrollbar(graphics, rankCommandsX, rankCommandsY, rankCommandsW, rankCommandsH,
				rankCommandScroll, rows, total);
	}

	private void drawServerCommands(GuiGraphics graphics, int mouseX, int mouseY) {
		List<String> commands = RankDetector.serverCommands();
		// How many columns fit given roughly 140px per column
		int columns = Math.max(1, serverCommandsW / 140);
		int colW = serverCommandsW / columns;
		int totalRows = (commands.size() + columns - 1) / columns;
		int rows = Math.max(1, serverCommandsH / RANK_COMMAND_ROW);
		serverCommandScroll = clamp(serverCommandScroll, 0, Math.max(0, totalRows - rows));

		graphics.fill(serverCommandsX, serverCommandsY, serverCommandsX + serverCommandsW,
				serverCommandsY + serverCommandsH, applyFade(PANEL, fadeProgress));

		graphics.enableScissor(serverCommandsX, serverCommandsY, serverCommandsX + serverCommandsW - 6,
				serverCommandsY + serverCommandsH);

		if (commands.isEmpty()) {
			UiFont.draw(graphics, "Сервер не прислал дерево команд", serverCommandsX + 8, serverCommandsY + 6, MUTED);;
			UiFont.draw(graphics, "Зайдите на сервер — права определятся автоматически", serverCommandsX + 8, serverCommandsY + 18, MUTED);;
		}

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < columns; col++) {
				int index = (serverCommandScroll + row) * columns + col;
				if (index >= commands.size()) {
					break;
				}

				String command = commands.get(index);
				int cellX = serverCommandsX + serverCommandsW * col / columns;
                int cellRight = serverCommandsX + serverCommandsW * (col + 1) / columns;
				int cellY = serverCommandsY + row * RANK_COMMAND_ROW;
				boolean hover = mouseX >= cellX && mouseX < cellRight
						&& mouseY >= cellY && mouseY < cellY + RANK_COMMAND_ROW;

				// Check if command belongs to the selected rank
				boolean inRank = selectedRank != null && RankCatalog.commandsForMode(selectedRank,RankDetector.detectedMode()).stream()
						.anyMatch(entry -> entry.name().equalsIgnoreCase(command));
				boolean inCurrentRank = RankDetector.current() != null
						&& RankCatalog.commandsForMode(RankDetector.current(),RankDetector.detectedMode()).stream()
								.anyMatch(entry -> entry.name().equalsIgnoreCase(command));

				if (hover) {
					UiGraphics.rounded(cellX, cellY, cellRight - cellX, RANK_COMMAND_ROW, 0, 0x20FFFFFF);
				} else if (inCurrentRank) {
					UiGraphics.rounded(cellX, cellY, cellRight - cellX, RANK_COMMAND_ROW, 0, 0x14308830);
				}

				int cmdColor;
				if (inCurrentRank) {
					cmdColor = GREEN;
				} else if (inRank) {
					cmdColor = selectedRank.argb();
				} else {
					cmdColor = TEXT;
				}

				UiFont.draw(graphics, truncate("/" + command, colW - 10), cellX + 5, UiFont.centeredY(cellY, RANK_COMMAND_ROW - 1, UiFont.SIZE), cmdColor);
			}
		}

		graphics.disableScissor();
		drawScrollbar(graphics, serverCommandsX, serverCommandsY, serverCommandsW, serverCommandsH,
				serverCommandScroll, rows, totalRows);
	}

	private void renderRules(GuiGraphics graphics, int mouseX, int mouseY) {
		List<RulePreset> presets = RuleBook.all();
		int rows = Math.max(1, rulesListH / 16);
		rulesScroll = clamp(rulesScroll, 0, Math.max(0, presets.size() - rows));

		graphics.fill(rulesListX, rulesListY, rulesListX + rulesListW, rulesListY + rulesListH, applyFade(PANEL, fadeProgress));

		graphics.enableScissor(rulesListX, rulesListY, rulesListX + rulesListW - 6, rulesListY + rulesListH);

		for (int row = 0; row < rows; row++) {
			int index = rulesScroll + row;
			if (index >= presets.size()) {
				break;
			}

			RulePreset preset = presets.get(index);
			int rowY = rulesListY + row * 16;
			boolean hover = mouseX >= rulesListX && mouseX < rulesListX + rulesListW
					&& mouseY >= rowY && mouseY < rowY + 16;

			if (preset == selectedRule) {
				drawListRow(rulesListX, rulesListY, rulesListW, rulesListH, rowY, 16, applyFade(SELECTED, fadeProgress));
				graphics.fill(rulesListX + 1, rowY, rulesListX + 3, rowY + 15, applyFade(ACCENT, fadeProgress));
			} else if (hover) {
				drawListRow(rulesListX, rulesListY, rulesListW, rulesListH, rowY, 16, 0x16FFFFFF);
			} else if ((index & 1) == 1) {
				drawListRow(rulesListX, rulesListY, rulesListW, rulesListH, rowY, 16, 0x08FFFFFF);
			}

			// Layout: [code] ACTION duration-range  |  title
			// Action is colour coded; duration shows "base" or "min - max (base)".
			int actionColor = switch (preset.action) {
				case MUTE -> 0xFFAAC7E5;
				case BAN -> 0xFFFF6B6B;
				case BANIP -> 0xFFD94F4F;
				case WARN -> 0xFFFFC94F;
				case KICK -> 0xFFB07CFF;
				case NOTE -> 0xFF9AA2AE;
			};
			String durationLabel = durationRangeLabel(preset);
            drawRuleColumns(graphics,preset,rulesListX,rowY,rulesListW-6,16);
		}

		graphics.disableScissor();
		drawScrollbar(graphics, rulesListX, rulesListY, rulesListW, rulesListH, rulesScroll, rows, presets.size());
	}

	private void renderOnline(GuiGraphics graphics, int mouseX, int mouseY) {
        List<OnlineEntry> names = onlineEntries();
		int listY = contentY + 70;
		int listH = onlineListHeight();
		int rows = Math.max(1, (listH + ONLINE_ROW - 1) / ONLINE_ROW + 1);
		onlineScroll = clamp(onlineScroll, 0, Math.max(0, names.size() * ONLINE_ROW - listH));
        int first = onlineScroll / ONLINE_ROW, offset = onlineScroll % ONLINE_ROW;
		String state = com.envyworld.logs.OnlineScanner.state();
		String status = switch (state) {
			case "scanning" -> "сканирование: " + com.envyworld.logs.OnlineScanner.collectedCount();
			case "done" -> "готово  |  следующая проверка через ~30 с";
			default -> "ожидание…";
		};
        long total = names.stream().filter(entry -> !entry.heading()).count();
		String header = "Онлайн сети: " + onlineTotal + "  |  С вами: " + onlineNearby + "  |  Показано: " + total + "  |  " + status;
		UiFont.draw(graphics, truncate(statusText.isBlank() ? header : statusText, contentW), contentX, contentY + 27, statusText.isBlank() ? MUTED : statusColor);
        String[] filters = {"Все игроки", "С вами на сервере", "Другие игроки сети", "Отслеживаемые"};
        int stride = onlineChipStride();
        for (int i = 0; i < filters.length; i++) drawToggle(graphics, contentX + i * stride, contentY + 43,
                stride - 5, 18, truncate(filters[i], stride - 16), onlineFilter == i, mouseX, mouseY);
		graphics.fill(contentX, listY, contentX + contentW, listY + listH, applyFade(PANEL, fadeProgress));

		graphics.enableScissor(contentX, listY, contentX + contentW - 6, listY + listH);
        if (names.isEmpty()) UiFont.draw(graphics, minecraft.getConnection() == null
                ? "Подключитесь к серверу, чтобы увидеть онлайн" : "В этой группе игроков нет", contentX + 12, listY + 14, MUTED);
		for (int row = 0; row < rows; row++) {
			int index = first + row;
			if (index >= names.size()) break;
            OnlineEntry entry = names.get(index);
			int y = listY + row * ONLINE_ROW - offset;
            if (entry.heading()) {
                graphics.fill(contentX + 1, y, contentX + contentW - 1, y + 25, 0x0FFFFFFF);
                UiFont.draw(graphics, entry.name(), contentX + 10, y + 9, UiFont.SIZE_SMALL, entry.near() ? 0xFF87D9B0 : MUTED);
                continue;
            }
            boolean hover = inRect(mouseX, mouseY, contentX, y, contentW, ONLINE_ROW) && mouseY >= listY && mouseY < listY + listH;
			if (hover || (index & 1) == 1) graphics.fill(contentX + 1, y, contentX + contentW - 1, y + 25, hover ? 0x16FFFFFF : 0x06FFFFFF);
            UiIcons.watch(contentX + contentW - 22, y + 13, com.envyworld.logs.OnlineWatch.watched(entry.name()),
                    com.envyworld.logs.OnlineWatch.watched(entry.name()) ? 0xFFB4D3F8 : 0xFF6F7C8F);
            PlayerFaceRenderer.draw(graphics, PlayerProfileView.cached(entry.name()).skin(), contentX + 9, y + 5, 16);
			UiFont.draw(graphics, entry.name(), contentX + 33, y + 5, TEXT);
            UiFont.draw(graphics, entry.near() ? nearbyMode() : onlineFilter == 3 ? watchLabel(entry.name()) : unknownOnlineLabel(entry.name()),
                    contentX + 33, y + 16, UiFont.SIZE_SMALL, entry.near() ? 0xFF87D9B0 : MUTED);
            if (hover) graphics.setTooltipForNextFrame(this.font, List.of(Component.literal(mouseX >= contentX + contentW - 38 ? "Нажмите значок: отслеживать / отключить" : "Двойной клик — профиль " + entry.name()),
                    Component.literal(entry.near() ? nearbyMode() : onlineFilter == 3 ? watchLabel(entry.name()) : unknownOnlineLabel(entry.name()))), Optional.empty(), mouseX, mouseY);
		}
		graphics.disableScissor();
		drawScrollbar(graphics, contentX, listY, contentW, listH, onlineScroll, listH, names.size() * ONLINE_ROW);

	}

    private record OnlineEntry(String name, boolean near, boolean heading) {}
    private String watchLabel(String nick) {
        var state = com.envyworld.logs.OnlineWatch.presence(nick);
        return !state.known() ? "Проверяем присутствие" : state.online() ? "В сети  |  сервер неизвестен" : "Не в сети";
    }
    private int onlineListHeight() { return Math.max(16, panelY + panelH - PAD - (contentY + 70)); }
    private String nearbyMode() {
        String mode = RankDetector.detectedMode();
        return "С вами  |  " + (mode.equals("—") ? "режим не определён" : mode);
    }
    private String unknownOnlineLabel(String nick) {
        if (RankDetector.isStaffNick(nick)) return "Персонал  |  сервер неизвестен";
        return "Сервер неизвестен";
    }
    private int onlineChipStride() { return Math.min(144, contentW / 4); }
    private List<OnlineEntry> onlineEntries() {
        java.util.Map<String, String> visible = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (minecraft.getConnection() != null) for (PlayerInfo info : minecraft.getConnection().getListedOnlinePlayers()) {
            String name = info.getProfile().name(); visible.put(name, name);
        }
        java.util.Map<String, String> network = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : com.envyworld.logs.OnlineScanner.online()) network.put(name, name);
        network.putAll(visible);
        onlineTotal = network.size();
        onlineNearby = visible.size();
        if (onlineFilter == 3) {
            return EnvyWorld.config().client.watchedPlayers.stream()
                    .filter(n -> n.toLowerCase(Locale.ROOT).contains(onlineSearch.toLowerCase(Locale.ROOT)))
                    .sorted(String.CASE_INSENSITIVE_ORDER).map(n -> new OnlineEntry(n, visible.containsKey(n), false)).toList();
        }
        List<String> near = new ArrayList<>(), unknown = new ArrayList<>();
        for (String name : network.values()) {
            if (!onlineSearch.isBlank() && !name.toLowerCase(Locale.ROOT).contains(onlineSearch.toLowerCase(Locale.ROOT))) continue;
            if (visible.containsKey(name)) near.add(name); else unknown.add(name);
        }
        List<OnlineEntry> result = new ArrayList<>();
        if (onlineFilter != 2 && !near.isEmpty()) {
            result.add(new OnlineEntry(nearbyMode().toUpperCase(Locale.ROOT) + "  |  " + near.size(), true, true));
            for (String name : near) result.add(new OnlineEntry(name, true, false));
        }
        if (onlineFilter != 1 && !unknown.isEmpty()) {
            result.add(new OnlineEntry("ДРУГИЕ ИГРОКИ СЕТИ  |  " + unknown.size(), false, true));
            for (String name : unknown) result.add(new OnlineEntry(name, false, false));
        }
        return result;
    }

	private void renderHistory(GuiGraphics graphics, int mouseX, int mouseY) {
        List<PunishLogClient.Row> all = profileRows();
        long received = all.stream().filter(row -> row.punished().equalsIgnoreCase(profileName)).count();
        long issued = all.stream().filter(row -> row.punisher().equalsIgnoreCase(profileName)).count();
        List<PunishLogClient.Row> rows = all.stream().filter(row -> historyMode == 0
                ? row.punished().equalsIgnoreCase(profileName) : row.punisher().equalsIgnoreCase(profileName)).toList();
        graphics.fill(profileCardX, profileCardY, profileCardX + profileCardW, profileCardY + profileCardH, PANEL);
        graphics.renderOutline(profileCardX, profileCardY, profileCardW, profileCardH, BORDER);
        UiFont.draw(graphics, "ПРОФИЛЬ ИГРОКА", profileCardX + 12, profileCardY + 10, UiFont.SIZE_SMALL, MUTED);
        UiFont.draw(graphics, profileName.isBlank() ? "Выберите игрока" : truncate(profileName, profileCardW - 24),
                profileCardX + 12, profileCardY + 23, 9, TEXT);
        if (profileView != null) {
            int bottom = profileCardY + profileCardH;


            UiFont.draw(graphics, "Получено: " + received, profileCardX + 10, bottom - 25, TEXT);
            UiFont.draw(graphics, "Выдано: " + issued, profileCardX + 10, bottom - 13, MUTED);
        }
        int chipWidth = historyChipWidth();
        drawToggle(graphics, historyListX, profileCardY, chipWidth, 20, "Получил | " + received, historyMode == 0, mouseX, mouseY);
        drawToggle(graphics, historyListX + chipWidth + 6, profileCardY, chipWidth, 20, "Выдал | " + issued, historyMode == 1, mouseX, mouseY);
        drawToggle(graphics, historyListX + (chipWidth + 6) * 2, profileCardY, chipWidth, 20, "Проверки IP", historyMode == 2, mouseX, mouseY);
        if (historyMode == 2) { renderProfileChecks(graphics); return; }
        int visible = Math.max(1, (historyListH + LOG_ROW - 1) / LOG_ROW + 1);
        historyScroll = clamp(historyScroll, 0, Math.max(0, logTotal(rows,historyListW) - historyListH));
        int first = historyScroll / LOG_ROW, offset = historyScroll % LOG_ROW;
        graphics.fill(historyListX, historyListY, historyListX + historyListW, historyListY + historyListH, PANEL);

        graphics.enableScissor(historyListX + 1, historyListY + 1, historyListX + historyListW - 6, historyListY + historyListH - 1);
        if (rows.isEmpty()) {
            String empty = profileName.isBlank() ? "Введите ник или откройте профиль двойным кликом в логах"
                    : historyState.equals("loading") ? "Загружаем историю..."
                    : historyState.equals("error") ? "Не удалось загрузить историю: " + historyError
                    : "В этой категории наказаний нет";
            int y = historyListY + 14;
            for (String line : UiFont.wrap(empty, historyListW - 28)) {
                UiFont.draw(graphics, line, historyListX + 14, y, MUTED);
                y += 12;
            }
        }
        renderLogRows(graphics,rows,historyListX,historyListY,historyListW,historyListH,historyScroll,mouseX,mouseY);
        graphics.disableScissor();
        drawScrollbar(graphics, historyListX, historyListY, historyListW, historyListH, historyScroll, historyListH, logTotal(rows,historyListW));
    }

    private void renderProfileChecks(GuiGraphics graphics) {
        boolean connected = minecraft.getConnection() != null && !profileName.isBlank();
        dupeIpButton.active = connected && RankDetector.canProfileCheck("dupeip");
        ipHistoryButton.active = connected && RankDetector.canProfileCheck("iphistory");
        graphics.fill(historyListX, historyListY, historyListX + historyListW, historyListY + historyListH, PANEL);

        int textY = historyListY + 42;
        List<String> lines = new ArrayList<>();
        lines.add("/" + profileCheckCommand + " " + profileName + "  |  ответ сервера");
        var result = ProfileChecks.snapshot(profileName, profileCheckCommand);
        if (result.lines().isEmpty()) {
            String help = switch (result.state()) {
                case "loading" -> "Проверка выполняется";
                case "timeout" -> "Ответ не получен. Проверьте доступность команды и сообщения в чате.";
                case "disconnected" -> "Подключение прервано.";
                default -> "Выберите проверку кнопкой выше. /dupeip показывает аккаунты с общим IP, /iphistory — историю IP игрока.";
            };
            lines.addAll(UiFont.wrap(help, historyListW - 26));
        } else for (String line : result.lines()) lines.addAll(UiFont.wrap(line, historyListW - 26));
        lines.add("");
        lines.addAll(UiFont.wrap("Доступ: /dupeip — Президент и персонал; /iphistory — персонал. Фактические права проверяет сервер.", historyListW - 26));
        int textHeight = Math.max(12, historyListH - 52);
        int visible = (textHeight + 11) / 12 + 1;
        historyScroll = clamp(historyScroll, 0, Math.max(0, lines.size() * 12 - textHeight));
        int first = historyScroll / 12, offset = historyScroll % 12;
        graphics.enableScissor(historyListX + 1, textY, historyListX + historyListW - 6, historyListY + historyListH - 1);
        for (int i = 0; i < visible && first + i < lines.size(); i++) {
            int index = first + i;
            String line=lines.get(index);int color=index==0?0xFFAFCFF1:TEXT;
            for(var account:result.accounts())if(line.startsWith(account.nick()+" |")){color=account.color();break;}
            UiFont.draw(graphics, line, historyListX + 12, textY + i * 12 - offset, color);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, historyListX, textY, historyListW, textHeight, historyScroll, textHeight, lines.size() * 12,false);
        UiGraphics.outline(historyListX,historyListY,historyListW,historyListH,9,BORDER);
    }
	private void renderPunishLog(GuiGraphics graphics, int mouseX, int mouseY) {
        PunishLogClient.tickAll(logSearch, logServer);
        String serverLabel = logServer.equals("all") ? "Все серверы" : com.envyworld.logs.ServerModes.label(logServer);
        drawToggle(graphics, logServerX, logServerY, logServerW, 18, truncate(serverLabel, logServerW - 26), logServerOpen, mouseX, mouseY);
        UiIcons.chevron(logServerX + logServerW - 11, logServerY + 9, logServerOpen, MUTED);
        String[] filters = {"all", "bans", "mutes", "warnings"};
        String[] labels = {"Все", "Баны", "Муты", "Варны"};
        for (int i = 0; i < filters.length; i++) {
            drawToggle(graphics, contentX + i * logChipStride(), contentY + 24, logChipStride() - 4, 18,
                    labels[i], logTypeFilter.equals(filters[i]), mouseX, mouseY);
        }
        var snapshot = PunishLogClient.snapshot(logSearch, logServer);
        List<PunishLogClient.Row> rows = filteredLogRows();
        int visible = Math.max(1, (logListH + LOG_ROW - 1) / LOG_ROW + 1);
        logScroll = clamp(logScroll, 0, Math.max(0, logTotal(rows,logListW) - logListH));
        int first = logScroll / LOG_ROW, offset = logScroll % LOG_ROW;
        graphics.fill(logListX, logListY, logListX + logListW, logListY + logListH, PANEL);

        graphics.enableScissor(logListX + 1, logListY + 1, logListX + logListW - 6, logListY + logListH - 1);
        if (rows.isEmpty()) {
            String empty = snapshot.state().equals("loading") ? "Ищем записи..."
                    : snapshot.state().equals("error") ? "Ошибка: " + snapshot.error() : "Ничего не найдено";
            UiFont.draw(graphics, truncate(empty, logListW - 24), logListX + 12, logListY + 12, MUTED);
        }
        renderLogRows(graphics,rows,logListX,logListY,logListW,logListH,logScroll,mouseX,mouseY);
        graphics.disableScissor();
        drawScrollbar(graphics, logListX, logListY, logListW, logListH, logScroll, logListH, logTotal(rows,logListW));
    }

    private final java.util.Set<PunishLogClient.Row> expandedLogs=new java.util.HashSet<>();
    private record WrappedLog(PunishLogClient.Row row,int width) {}
    private final java.util.Map<WrappedLog,List<String>> wrappedLogs=new java.util.LinkedHashMap<>();
    private List<String> expandedLines(PunishLogClient.Row row,int width){
        var key=new WrappedLog(row,width);var cached=wrappedLogs.get(key);if(cached!=null)return cached;
        var lines=new ArrayList<String>();
        for(String part:row.reason().split("(?iu)\\s*(?: - (?=Предупрежд)|\\n+)\\s*")){
            if(!part.isBlank())lines.addAll(UiFont.wrap(part.strip(),Math.max(40,width-32)));
        }
        if(wrappedLogs.size()>=128)wrappedLogs.remove(wrappedLogs.keySet().iterator().next());
        wrappedLogs.put(key,List.copyOf(lines));return lines;
    }
    private int logHeight(PunishLogClient.Row row,int width){return LOG_ROW+(expandedLogs.contains(row)?8+expandedLines(row,width).size()*12:0);}
    private int logTotal(List<PunishLogClient.Row> rows,int width){return rows.stream().mapToInt(r->logHeight(r,width)).sum();}
    private int logIndex(List<PunishLogClient.Row> rows,int width,double offset){for(int i=0;i<rows.size();i++){offset-=logHeight(rows.get(i),width);if(offset<0)return i;}return -1;}
    private void renderLogRows(GuiGraphics g,List<PunishLogClient.Row> rows,int x,int top,int w,int h,int scroll,int mx,int my){
        int y=top-scroll;
        for(int i=0;i<rows.size();i++){
            var row=rows.get(i);int rh=logHeight(row,w);
            if(y+rh>top && y<top+h){boolean hover=inRect(mx,my,x,y,w-5,rh)&&my>=top&&my<top+h;
                if(hover || row.equals(logSelected) || (i&1)==1)g.fill(x+1,y,x+w-4,y+rh-1,row.equals(logSelected)?SELECTED:hover?0x14FFFFFF:0x06FFFFFF);
                drawLogEntry(g,row,x,y,w-6,hover,mx,my);
                if(expandedLogs.contains(row)){int yy=y+LOG_ROW+3;for(String line:expandedLines(row,w)){if(yy+12>top && yy<top+h)UiFont.draw(g,line,x+14,yy,7,TEXT);yy+=12;}}
            }
            y+=rh;if(y>=top+h)break;
        }
    }
    private void drawLogEntry(GuiGraphics graphics, PunishLogClient.Row entry, int x, int y, int width,
                              boolean hover, int mouseX, int mouseY) {
        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + LOG_ROW - 1);
        int color = logTypeColor(entry.type());
        graphics.fill(x + 2, y + 4, x + 4, y + LOG_ROW - 5, color);
        String tag = entry.typeLabel();
        float titleY = UiFont.centeredY(y + 3, 12, UiFont.SIZE);
        UiFont.draw(graphics, tag, x + 10, titleY, color);
        int headX = x + 50;
        int nameWidth = (x + width - headX - 34) / 2;
        drawLogHead(graphics, entry.punished(), headX, y + 3);
        drawLogHead(graphics, entry.punisher(), headX + nameWidth, y + 3);
        UiFont.draw(graphics, truncate("Получил: " + entry.punished(), nameWidth - 24), headX + 17, titleY, 0xFFEAB5C0);
        UiFont.draw(graphics, truncate("Выдал: " + entry.punisher(), nameWidth - 17), headX + nameWidth + 17, titleY, 0xFFAFCFF1);
        String reason=entry.reason().replaceAll("\\s+", " ");
        graphics.enableScissor(x+10,y+15,headX+nameWidth-12,y+26);
        UiFont.drawFaded(graphics,reason,x+10,y+16,headX+nameWidth-x-22,TEXT);
        graphics.disableScissor();
        var timer = com.envyworld.logs.PunishmentClock.status(entry);
        String timeLabel = truncate(timer.label(), width / 2);
        int timeWidth = UiFont.width(timeLabel);
        String meta = entry.date() + (entry.server().isBlank() ? "" : "  |  " + com.envyworld.logs.ServerModes.label(entry.server()));
        UiFont.draw(graphics, truncate(meta, width - timeWidth - 48), x + 10, y + 27, UiFont.SIZE_SMALL, MUTED);
        UiFont.draw(graphics, timeLabel, x + width - timeWidth - 28, y + 27, timer.color());
        UiIcons.chevron(x+width-15,y+21,expandedLogs.contains(entry),MUTED);
        graphics.disableScissor();

    }

    private static String logSkinNick(String name) {
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "консоль", "console", "[console]", "[консоль]", "server", "сервер" -> "Secularizer";
            default -> name;
        };
    }

    private void drawLogHead(GuiGraphics graphics, String name, int x, int y) {
        // Native images are drawn after the Wex geometry. Suppress an image covered
        // by the open popup so it cannot paint over the popup's options.
        if (tab == 5 && logServerOpen && x < logServerMenuX() + logServerMenuWidth() && x + 12 > logServerMenuX()
                && y < logServerMenuY() + logServerMenuHeight() && y + 12 > logServerMenuY()) return;
        String nick = logSkinNick(name);
        var skin = nick.matches("[A-Za-z0-9_]{1,16}") ? PlayerProfileView.cached(nick).skin()
                : net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin();
        PlayerFaceRenderer.draw(graphics, skin, x, y, 12);
    }
	// ------------------------------------------------------------------ lists

	private void drawPlayerList(GuiGraphics graphics, int mouseX, int mouseY) {
		List<ModerationPlayer> players = filteredPlayers();
		int rows = Math.max(1, playerListH / PLAYER_ROW);
		playerScroll = clamp(playerScroll, 0, Math.max(0, players.size() - rows));

		graphics.fill(playerListX, playerListY, playerListX + playerListW, playerListY + playerListH,
				applyFade(PANEL, fadeProgress));

		graphics.enableScissor(playerListX, playerListY, playerListX + playerListW, playerListY + playerListH);

		for (int row = 0; row < rows + (playerListH % PLAYER_ROW > 0 ? 1 : 0); row++) {
			int index = playerScroll + row;
			if (index >= players.size()) {
				break;
			}

			ModerationPlayer info = players.get(index);
			String name = info.name();
			int rowY = playerListY + row * PLAYER_ROW;
			boolean hover = mouseX >= playerListX && mouseX < playerListX + playerListW
					&& mouseY >= rowY && mouseY < rowY + PLAYER_ROW;

			if (name.equals(targetName)) {
				drawListRow(playerListX,playerListY,playerListW,playerListH,rowY,PLAYER_ROW,
						applyFade(SELECTED, fadeProgress));
			} else if (hover) {
				drawListRow(playerListX,playerListY,playerListW,playerListH,rowY,PLAYER_ROW,
						applyFade(0x16FFFFFF, fadeProgress));
			}

			PlayerFaceRenderer.draw(graphics, PlayerProfileView.cached(name).skin(), playerListX + 6, rowY + (PLAYER_ROW-10)/2, 10);
			UiFont.draw(graphics, truncate(name, Math.max(30,playerListW - 103)), playerListX + 21, UiFont.centeredY(rowY, PLAYER_ROW - 1, UiFont.SIZE), applyFade(TEXT, fadeProgress));
            UiFont.draw(graphics, info.near()?"С вами":"Другой сервер", playerListX+playerListW-86, UiFont.centeredY(rowY,PLAYER_ROW-1,6),6,info.near()?0xFF9DDAC2:MUTED);
		}

		graphics.disableScissor();
		drawScrollbar(graphics, playerListX, playerListY, playerListW, playerListH, playerScroll, rows, players.size());
	}

	private void drawRuleList(GuiGraphics graphics, int mouseX, int mouseY) {
		List<RulePreset> presets = RuleBook.all();
		int rows = Math.max(1, ruleListH / RULE_ROW);
		ruleScroll = clamp(ruleScroll, 0, Math.max(0, presets.size() - rows));

		graphics.fill(ruleListX, ruleListY, ruleListX + ruleListW, ruleListY + ruleListH, applyFade(PANEL, fadeProgress));

		graphics.enableScissor(ruleListX, ruleListY, ruleListX + ruleListW, ruleListY + ruleListH);

		for (int row = 0; row < rows + (ruleListH % RULE_ROW > 0 ? 1 : 0); row++) {
			int index = ruleScroll + row;
			if (index >= presets.size()) {
				break;
			}

			RulePreset preset = presets.get(index);
			int rowY = ruleListY + row * RULE_ROW;
			boolean hover = mouseX >= ruleListX && mouseX < ruleListX + ruleListW
					&& mouseY >= rowY && mouseY < rowY + RULE_ROW;

			if (preset == selectedRule) {
				drawListRow(ruleListX,ruleListY,ruleListW,ruleListH,rowY,RULE_ROW, applyFade(SELECTED, fadeProgress));
			} else if (hover) {
				drawListRow(ruleListX,ruleListY,ruleListW,ruleListH,rowY,RULE_ROW, 0x16FFFFFF);
			} else if ((index & 1) == 1) {
				drawListRow(ruleListX,ruleListY,ruleListW,ruleListH,rowY,RULE_ROW, 0x08FFFFFF);
			}

			int actionColor = switch (preset.action) {
				case MUTE -> 0xFFAAC7E5;
				case BAN -> 0xFFFF6B6B;
				case BANIP -> 0xFFD94F4F;
				case WARN -> 0xFFFFC94F;
				case KICK -> 0xFFB07CFF;
				case NOTE -> 0xFF9AA2AE;
			};
			String durationLabel = durationRangeLabel(preset);
            drawRuleColumns(graphics,preset,ruleListX,rowY,ruleListW-6,RULE_ROW);

		}

		graphics.disableScissor();
		drawScrollbar(graphics, ruleListX, ruleListY, ruleListW, ruleListH, ruleScroll, rows, presets.size());
	}

    private void drawBindList(GuiGraphics g,int mouseX,int mouseY){
        var binds=ClientBinds.all();int rows=Math.max(1,bindListH/MACRO_ROW);bindScroll=clamp(bindScroll,0,Math.max(0,binds.size()-rows));
        g.fill(bindListX,bindListY,bindListX+bindListW,bindListY+bindListH,PANEL);

        int right=bindListX+bindListW-6, available=right-bindListX-12, split=bindListX+8+available*54/100;
        g.enableScissor(bindListX,bindListY,right,bindListY+bindListH);
        if(binds.isEmpty())UiFont.draw(g,"Пока нет макросов",bindListX+8,bindListY+10,MUTED);
        for(int row=0;row<=rows;row++){
            int index=bindScroll+row;if(index>=binds.size())break;var bind=binds.get(index);int yy=bindListY+row*MACRO_ROW;
            boolean hover=inRect(mouseX,mouseY,bindListX,yy,right-bindListX,Math.min(MACRO_ROW,bindListY+bindListH-yy));
            drawListRow(bindListX,bindListY,bindListW,bindListH,yy,MACRO_ROW,bind==selectedBind?SELECTED:hover?0x16FFFFFF:index%2==0?0x06FFFFFF:0x0CFFFFFF);
            int nameX=bindListX+25;
            try{var p=CustomItems.byId(bind.customItem);var stack=p!=null?CustomItems.build(p,1):new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(bind.item.equals("any")?"minecraft:air":bind.item)));if(!stack.isEmpty()){g.pose().pushMatrix();g.pose().translate(bindListX+6,yy+4);g.pose().scale(.75f,.75f);g.renderItem(stack,0,0);g.pose().popMatrix();}else UiIcons.draw("terminal",bindListX+15,yy+10,12,MUTED);}catch(RuntimeException ignored){}
            UiFont.draw(g,truncate(bind.id,split-nameX-8),nameX,yy+5,bind.enabled?TEXT:MUTED);
            String key=bind.keyCode==-1?"Общая: "+com.envyworld.client.EnvyWorldClient.macroKeyName():com.envyworld.client.MacroKeys.label(bind.keyCode,bind.keyModifiers);
            UiFont.draw(g,truncate("Клавиша: "+key,right-split-8),split,yy+5,0xFFBDD4EF);
            String description=bind.description==null||bind.description.isBlank()?"Нет описания":bind.description;
            UiFont.draw(g,truncate(description,split-bindListX-16),bindListX+8,yy+18,6,MUTED);
            String mode=switch(bind.keyMode==null?"PRESS":bind.keyMode){case "HOLD"->"Зажатие";case "TOGGLE"->"Переключение";default->"Одно нажатие";};
            if(bind.trigger==BindTrigger.ATTACK_ENTITY||bind.trigger==BindTrigger.USE_ITEM)mode="Вкл / выкл";
            UiFont.draw(g,truncate(triggerLabel(bind.trigger.id())+": "+mode,right-split-8),split,yy+18,6,MUTED);
            String[] commands=bind.command.split("\\n");String commandLabel=commands.length>1?"Команды ("+commands.length+"): ":"Команда: ";
            UiFont.draw(g,truncate(commandLabel+bind.command.replace('\n',' ').replace(';',' '),available),bindListX+8,yy+31,6,0xFFB1C1D4);
            if(hover&&(UiFont.width(bind.id)>split-nameX-8||UiFont.width(description,6)>split-bindListX-16||UiFont.width(commandLabel+bind.command,6)>available))g.setTooltipForNextFrame(font,List.of(Component.literal(bind.id),Component.literal("Клавиша: "+key),Component.literal(description),Component.literal("Режим: "+mode),Component.literal(bind.command)),Optional.empty(),mouseX,mouseY);
        }
        g.disableScissor();drawScrollbar(g,bindListX,bindListY,bindListW,bindListH,bindScroll,rows,binds.size());
    }

	private void drawItemGrid(GuiGraphics graphics, int mouseX, int mouseY) {
		int columns = Math.max(1, (itemGridW - 14) / CELL);
		int rows = Math.max(1, itemGridH / CELL);
		int totalRows = (filteredItems.size() + columns - 1) / columns;
		itemScroll = clamp(itemScroll, 0, Math.max(0, totalRows - rows));

		graphics.fill(itemGridX, itemGridY, itemGridX + itemGridW, itemGridY + itemGridH, applyFade(PANEL, fadeProgress));

		graphics.enableScissor(itemGridX, itemGridY, itemGridX + itemGridW - 6, itemGridY + itemGridH);

		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				int index = (itemScroll + row) * columns + column;
				if (index >= filteredItems.size()) {
					break;
				}

				Item item = filteredItems.get(index);
				Identifier id = BuiltInRegistries.ITEM.getKey(item);
				if (id == null) {
					continue;
				}

				int cellX = itemGridX + column * CELL;
				int cellY = itemGridY + row * CELL - (itemCustomMode ? 0 : customScroll % CELL);
				boolean hover = mouseX >= cellX && mouseX < cellX + CELL && mouseY >= cellY && mouseY < cellY + CELL;

				if (id.toString().equals(selectedItem)) {
					graphics.fill(cellX + 1, cellY + 1, cellX + CELL - 1, cellY + CELL - 1, applyFade(SELECTED, fadeProgress));
					graphics.renderOutline(cellX + 1, cellY + 1, CELL - 2, CELL - 2, applyFade(ACCENT, fadeProgress));
				} else if (hover) {
					graphics.fill(cellX + 1, cellY + 1, cellX + CELL - 1, cellY + CELL - 1, applyFade(PANEL_LIGHT, fadeProgress));
				}

				ItemStack stack = new ItemStack(item);
				graphics.renderItem(stack, cellX + 2, cellY + 2);

				if (hover) {
					graphics.setTooltipForNextFrame(this.font,
							List.of(stack.getHoverName(),
									Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY)),
							Optional.empty(), mouseX, mouseY);
				}
			}
		}

		graphics.disableScissor();
		drawScrollbar(graphics, itemGridX, itemGridY, itemGridW, itemGridH, itemScroll, rows, totalRows);
	}

	// ------------------------------------------------------------------ primitives

	private void label(GuiGraphics graphics, String text, int x, int y) {
		UiFont.draw(graphics, text, x, y, applyFade(MUTED, fadeProgress));
	}

	private void drawPreview(GuiGraphics graphics, int x, int y, int w, int h, String headline, String command) {
		if (w <= 0 || h <= 0) {
			return;
		}

		graphics.fill(x, y, x + w, y + h, applyFade(PANEL, fadeProgress));
		graphics.renderOutline(x, y, w, h, applyFade(BORDER, fadeProgress));

		int textY = y + 6;
		UiFont.draw(graphics, truncate(headline, w - 14), x + 8, textY, applyFade(GOLD, fadeProgress));
		textY += 12;

		for (String line : UiFont.wrap(command, w - 16)) {
			if (textY > y + h - 11) {
				break;
			}
			UiFont.draw(graphics, line, x + 8, textY, applyFade(TEXT, fadeProgress));
			textY += UiFont.lineHeight() + 2;
		}
	}

	private void drawToggle(GuiGraphics graphics, int x, int y, int w, int h, String label, boolean active,
			int mouseX, int mouseY) {
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		int bg = active ? 0xC12A2A2A : hover ? 0xB3303030 : PANEL;
        float radius = Math.min(8, h / 2f);
        UiGraphics.rounded(x, y, w, h, radius, applyFade(bg, fadeProgress));
        UiGraphics.outline(x, y, w, h, radius, applyFade(active ? 0x58909090 : hover ? 0x44909090 : BORDER, fadeProgress));
		UiFont.draw(graphics, label, x + 7, UiFont.centeredY(y, h, UiFont.SIZE), applyFade(active ? TEXT : MUTED, fadeProgress));
	}

    private void drawListRow(int x,int y,int w,int h,int rowY,int rowHeight,int color){
        UiRenderer.submit((draw,matrix)->{
            float scale=matrix.m00();
            draw.pushScreenScissor(x*scale,rowY*scale,(w-6)*scale,rowHeight*scale);
            draw.drawRoundedRectangle(matrix,x,y,w,h,9,color);
            draw.popScissor();
        });
    }
    private void drawScrollbar(GuiGraphics graphics,int x,int y,int w,int h,int scroll,int visible,int total){
        drawScrollbar(graphics,x,y,w,h,scroll,visible,total,true);
    }
    private void drawScrollbar(GuiGraphics graphics,int x,int y,int w,int h,int scroll,int visible,int total,boolean border){
        var setter=scrollSetter(x,y);var bar=new ScrollBars.Bar(x+w-6,y+1,h-2,scroll,visible,total,setter);
        scrollBars.add(bar.x(),bar.y(),bar.height(),scroll,visible,total,setter);
        ScrollBars.drawInside(bar,x,y,w,h,9);
        if(border)UiGraphics.outline(x,y,w,h,9,BORDER);
    }

    private java.util.function.IntConsumer scrollSetter(int x, int y) {
        if (tab == 0) return y == playerListY ? value -> playerScroll = value : value -> ruleScroll = value;
        if (tab == 1) return value -> bindScroll = value;
        if (tab == 2) return itemCustomMode ? value -> itemScroll = value : value -> customScroll = value;
        if (tab == 3) {
            if (x == rankListX) return value -> rankScroll = value;
            return y == rankCommandsY ? value -> rankCommandScroll = value : value -> serverCommandScroll = value;
        }
        if (tab == 4) return value -> rulesScroll = value;
        if (tab == 5) return logServerOpen && y == logServerMenuY() + 4 ? value -> logServerMenuScroll = value : value -> logScroll = value;
        if (tab == 6) return value -> onlineScroll = value;
        return value -> historyScroll = value;
    }

	private void drawStatus(GuiGraphics graphics) {
        String text = statusText.isBlank() ? defaultHint() : statusText;
        int color = statusText.isBlank() ? MUTED : statusColor;
        if(statusText.isBlank() && tab==8 && !HudNotices.current().isEmpty()) {var notice=HudNotices.current().getFirst();text=notice.title()+"  |  "+notice.body();color=notice.color();}
        if (statusText.isBlank() && tab == 5) {
            var snapshot = PunishLogClient.snapshot(logSearch, logServer);
            text = filteredLogRows().size() + " записей | двойной клик — открыть профиль"
                    + (snapshot.state().equals("loading") ? " | обновляем..." : "")
                    + (snapshot.state().equals("partial") ? " | часть данных не загрузилась" : "")
                    + (snapshot.state().equals("error") ? " | обновление не удалось" : "")
                    + (snapshot.limited() ? " | достигнут лимит выдачи" : "");
        } else if (statusText.isBlank() && tab == 7) {
            text = profileName.isBlank() ? "Найдите игрока по нику или откройте его из логов"
                    : profileName + " | " + (historyMode == 2 ? "проверки аккаунтов / IP" : (historyMode == 0 ? "полученные" : "выданные") + " наказания")
                    + (historyState.equals("loading") ? " | обновляем..." : "")
                    + (historyState.equals("error") ? " | не удалось обновить историю" : "")
                    + (historyState.equals("partial") ? " | загружены не все категории" : "")
                    + (historyLimited ? " | достигнут лимит выдачи" : "");
        }
        UiFont.draw(graphics, truncate(text, statusWidth), statusX, statusY, applyFade(color, fadeProgress));
    }
    private List<PunishLogClient.Row> filteredLogRows() {
        List<PunishLogClient.Row> rows = PunishLogClient.snapshot(logSearch, logServer).rows();
        return logTypeFilter.equals("all") ? rows : rows.stream().filter(row -> row.type().equals(logTypeFilter)).toList();
    }

    private int logServerMenuWidth() { return Math.min(210, Math.max(166, logServerW)); }
    private int logServerMenuX() { return Math.min(logServerX, panelX + panelW - PAD - logServerMenuWidth()); }
    private int logServerMenuY() { return logServerY + 23; }
    private int logServerMenuRows() { return Math.max(1, Math.min(9, (panelH - PAD - logServerMenuY() - 8) / 18)); }
    private int logServerMenuHeight() { return logServerMenuRows() * 18 + 8; }

    private void renderLogServerDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = logServerMenuX(), y = logServerMenuY(), w = logServerMenuWidth(), h = logServerMenuHeight();
        UiGraphics.rounded(x - 2, y - 2, w + 4, h + 5, 9, 0x64000000);
        UiGraphics.rounded(x, y, w, h, 8, 0xF51E1E1E);
        UiGraphics.outline(x, y, w, h, 8, 0x488893A3);
        int visible = logServerMenuRows();
        logServerMenuScroll = clamp(logServerMenuScroll, 0, Math.max(0, PunishLogClient.SERVERS.length - visible));
        for (int i = 0; i < visible; i++) {
            int index = logServerMenuScroll + i;
            if (index >= PunishLogClient.SERVERS.length) break;
            String server = PunishLogClient.SERVERS[index];
            int rowY = y + 4 + i * 18;
            boolean selected = server.equals(logServer), hover = inRect(mouseX, mouseY, x + 4, rowY, w - 8, 18);
            if (hover) UiGraphics.rounded(x + 4, rowY, w - 8, 18, 6, 0xB3393939);
            if (selected) UiGraphics.rounded(x + 7, rowY + 6, 2, 6, 1, 0xFFC3CEDD);
            UiFont.draw(graphics, server.equals("all") ? "Все серверы" : com.envyworld.logs.ServerModes.label(server), x + 15,
                    UiFont.centeredY(rowY, 18, UiFont.SIZE), selected || hover ? TEXT : MUTED);
        }
        drawScrollbar(graphics, x, y + 4, w - 3, h - 8, logServerMenuScroll, visible, PunishLogClient.SERVERS.length,false);
    }

    private boolean clickLogServerDropdown(double mouseX, double mouseY) {
        int x = logServerMenuX(), y = logServerMenuY();
        if (inRect(mouseX, mouseY, x, y, logServerMenuWidth(), logServerMenuHeight())) {
            double offset = mouseY - y - 4;
            int row = (int) (offset / 18);
            int index = logServerMenuScroll + row;
            if (offset >= 0 && row < logServerMenuRows() && index >= 0 && index < PunishLogClient.SERVERS.length) selectLogServer(PunishLogClient.SERVERS[index]);
            return true;
        }
        logServerOpen = false;
        return inRect(mouseX, mouseY, logServerX, logServerY, logServerW, 18);
    }

    private void selectLogServer(String server) {
        if (!java.util.Arrays.asList(PunishLogClient.SERVERS).contains(server)) return;
        logServer = server;
        logServerOpen = false;
        logScroll = 0;
        logSelected = null;
        lastClickedLogRow = null;
        lastLogClickAt = 0;
        statusText = "";
        PunishLogClient.request(logSearch, logServer);
    }

    private List<PunishLogClient.Row> profileRows() {
        List<PunishLogClient.Row> rows = historyState.equals("loading")
                ? PunishLogClient.snapshot(profileName, "all").rows() : historyRows;
        return rows.stream().filter(row -> row.punished().equalsIgnoreCase(profileName)
                || row.punisher().equalsIgnoreCase(profileName)).toList();
    }

    private static int logTypeColor(String type) {
        return switch (type) { case "bans" -> 0xFFF195A5; case "mutes" -> 0xFF9BC6F4; default -> 0xFFDAC18C; };
    }

    private int logChipStride() { return Math.min(78, (contentW - 12) / 4) + 4; }
    private int historyChipWidth() { return Math.min(108, (historyListW - 12) / 3); }
    private int punishMenuWidth() { return Math.max(160, Math.min(300, punishTypeW)); }
    private int punishMenuX() { return Math.min(punishTypeX, panelX + panelW - PAD - punishMenuWidth()); }
    private int punishMenuY() { return Math.max(contentY, Math.min(punishTypeY + 22, footerY - 14 - PUNISH_TYPES.length * 17)); }

    private void renderPunishDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = punishMenuX(), y = punishMenuY(), width = punishMenuWidth();
        graphics.fill(x, y, x + width, y + PUNISH_TYPES.length * 17 + 8, 0xF21F1F1F);
        graphics.renderOutline(x, y, width, PUNISH_TYPES.length * 17 + 8, 0x38FFFFFF);
        for (int i = 0; i < PUNISH_TYPES.length; i++) {
            var type = PunishmentTypes.get(PUNISH_TYPES[i]);
            int rowY = y + 4 + i * 17;
            boolean hover = inRect(mouseX, mouseY, x + 3, rowY, width - 6, 17);
            if (hover || type.command().equals(punishType)) graphics.fill(x + 3, rowY, x + width - 3, rowY + 17,
                    hover ? 0x22FFFFFF : 0x12FFFFFF);
            int commandWidth = UiFont.width("/" + type.command(), UiFont.SIZE_SMALL);
            UiFont.draw(graphics, truncate(type.label(), width - commandWidth - 26), x + 8, UiFont.centeredY(rowY, 17, UiFont.SIZE), TEXT);
            UiFont.draw(graphics, "/" + type.command(), x + width - commandWidth - 8, UiFont.centeredY(rowY, 17, UiFont.SIZE_SMALL), UiFont.SIZE_SMALL, MUTED);
        }
    }

    private boolean clickPunishDropdown(double mouseX, double mouseY) {
        int x = punishMenuX(), y = punishMenuY();
        if (inRect(mouseX, mouseY, x, y, punishMenuWidth(), PUNISH_TYPES.length * 17 + 8)) {
            double offset = mouseY - y - 4;
            int index = (int) (offset / 17);
            if (offset >= 0 && index < PUNISH_TYPES.length) {
                punishType = PUNISH_TYPES[index];
                ruleMode = false;
                punishTypeOpen = false;
                typeButton.setMessage(Component.literal(PunishmentTypes.buttonLabel(punishType)));
            }
            return true;
        }
        punishTypeOpen = false;
        typeButton.setMessage(Component.literal(PunishmentTypes.buttonLabel(punishType)));
        return inRect(mouseX, mouseY, punishTypeX, punishTypeY, punishTypeW, 18);
    }

	// ------------------------------------------------------------------ interactions

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
        if (tab == 8 && settingsPanel.press(event.x(), event.y())) return true;
        if (scrollBars.press(event.x(), event.y())) return true;
        if(inRect(event.x(),event.y(),ownProfileX,panelY+5,ownProfileW,20)){minecraft.setScreen(new OwnProfileScreen(this));return true;}
		if (tab == 0 && punishTypeOpen && clickPunishDropdown(event.x(), event.y())) return true;
        if (tab == 5 && logServerOpen && clickLogServerDropdown(event.x(), event.y())) return true;
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		double mouseX = event.x();
		double mouseY = event.y();

		int clickedTab = tabAt(mouseX, mouseY);
		if (clickedTab >= 0) {
			switchTab(clickedTab);
			return true;
		}

        if (tab == 7) {
            int chip = historyChipWidth();
            for (int i = 0; i < 3; i++) if (inRect(mouseX, mouseY, historyListX + i * (chip + 6), profileCardY, chip, 20)) {
                historyMode = i;
                historyScroll = 0;
                statusText = "";
                init(this.width, this.height);
                return true;
            }
        }
        if (tab == 6) {
            if (inRect(mouseX, mouseY, contentX, contentY + 43, onlineChipStride() * 4, 18)) {
                onlineFilter = clamp((int) ((mouseX - contentX) / onlineChipStride()), 0, 3);
                onlineScroll = 0;
                return true;
            }
            int listY = contentY + 70;
            if (inRect(mouseX, mouseY, contentX, listY, contentW, onlineListHeight())) {
                List<OnlineEntry> entries = onlineEntries();
                int index = (int) ((onlineScroll + mouseY - listY) / ONLINE_ROW);
                if (index < entries.size() && !entries.get(index).heading()) {
                    String nick = entries.get(index).name();
                    if (mouseX >= contentX + contentW - 38) { com.envyworld.logs.OnlineWatch.toggle(nick); return true; }
                    long now = net.minecraft.util.Util.getMillis();
                    if (doubleClick || nick.equals(lastOnlineNick) && now - lastOnlineClick < 450) {
                        historySearch = nick; historyMode = 0; switchTab(7); fetchHistory();
                    } else { targetName = nick; setStatus("Выбран " + nick + "  |  двойной клик — профиль", TEXT); }
                    lastOnlineNick = nick; lastOnlineClick = now;
                }
                return true;
            }
        }
        if((tab==5 || tab==7) && mouseX >= (tab==5?logListX+logListW:historyListX+historyListW)-30 && mouseX < (tab==5?logListX+logListW:historyListX+historyListW)-5) {
            int top=tab==5?logListY:historyListY,h=tab==5?logListH:historyListH;
            if(mouseY>=top && mouseY<top+h && (tab==5 || historyMode!=2)) {
                var list=tab==5?filteredLogRows():profileRows().stream().filter(r->historyMode==0?r.punished().equalsIgnoreCase(profileName):r.punisher().equalsIgnoreCase(profileName)).toList();
                int index=logIndex(list,tab==5?logListW:historyListW,mouseY-top+(tab==5?logScroll:historyScroll));
                if(index>=0 && index<list.size()){var entry=list.get(index);if(!expandedLogs.remove(entry))expandedLogs.add(entry);return true;}
            }
        }
        if (tab == 5 && inRect(mouseX, mouseY, contentX, contentY + 24, contentW, 20)) {
            int index = (int) ((mouseX - contentX) / logChipStride());
            String[] filters = {"all", "bans", "mutes", "warnings"};
            if (index >= 0 && index < filters.length) {
                logTypeFilter = filters[index];
                statusText = "";
                logScroll = 0;
                return true;
            }
        }
		if (tab == 0) {

			if (inRect(mouseX, mouseY, ruleToggleX, ruleToggleY, ruleToggleW, ruleToggleH)) {
				ruleMode = true;
				return true;
			}
			if (inRect(mouseX, mouseY, manualToggleX, ruleToggleY, manualToggleW, ruleToggleH)) {
				ruleMode = false;
				return true;
			}
			if (inRect(mouseX, mouseY, playerListX, playerListY, playerListW, playerListH)) {
				return clickPlayerList(mouseY);
			}
			if (inRect(mouseX, mouseY, ruleListX, ruleListY, ruleListW, ruleListH)) {
				return clickRuleList(mouseY);
			}
		} else if (tab == 1) {
			if (inRect(mouseX, mouseY, bindListX, bindListY, bindListW, bindListH)) {
				return clickBindList(mouseY);
			}
		} else if (tab == 2) {
			if (itemCustomMode) {
				if (inRect(mouseX, mouseY, enchantPickerX, enchantPickerY,
						enchantPickerW, enchantPickerH)) {
					return clickEnchantPicker(mouseY);
				}
				if (inRect(mouseX, mouseY, itemGridX, itemGridY, itemGridW, itemGridH)) {
					return clickItemGrid(mouseX, mouseY);
				}
			} else if (inRect(mouseX, mouseY, itemGridX, itemGridY, itemGridW, itemGridH)) {
				return clickCustomList(mouseX, mouseY, doubleClick, event.hasShiftDown());
			}
		} else if (tab == 3) {
			if (inRect(mouseX, mouseY, rankListX, rankListY, rankListW, rankListH)) {
				return clickRankList(mouseY);
			}
			if (inRect(mouseX, mouseY, rankCommandsX, rankCommandsY, rankCommandsW, rankCommandsH)) {
				return clickRankCommand(mouseY);
			}
			if (inRect(mouseX, mouseY, serverCommandsX, serverCommandsY, serverCommandsW, serverCommandsH)) {
				return clickServerCommand(mouseX, mouseY);
			}
		} else if (tab == 4) {
			if (inRect(mouseX, mouseY, rulesListX, rulesListY, rulesListW, rulesListH)) {
				return clickRulesList(mouseY);
			}
		} else if (tab == 5) {
			if (inRect(mouseX, mouseY, logServerX, logServerY, logServerW, 18)) {
                logServerOpen = true;
				logServerMenuScroll = Math.max(0, java.util.Arrays.asList(PunishLogClient.SERVERS).indexOf(logServer) - 3);
				return true;
			}
			if (inRect(mouseX, mouseY, logListX, logListY, logListW, logListH)) {
				return clickLogList(mouseY, doubleClick);
			}
		}

		return false;
	}

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return (tab == 8 && settingsPanel.drag(event.y())) || scrollBars.drag(event.y()) || super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return (tab == 8 && settingsPanel.release()) || scrollBars.release() || super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (tab == 7 && historySearchField != null && historySearchField.isFocused()) {
                fetchHistory();
                return true;
            }
            if (tab == 5 && logSearchField != null && logSearchField.isFocused()) {
                PunishLogClient.request(logSearch, logServer);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private static String resolveServerAction(String action) {
        String[] aliases = switch (action) {
            case "banip" -> new String[]{"banip", "ipban"};
            case "muteip" -> new String[]{"muteip", "ipmute"};
            case "tempbanip" -> new String[]{"tempbanip", "tempipban"};
            case "tempmuteip" -> new String[]{"tempmuteip", "tempipmute"};
            default -> new String[]{action};
        };
        List<String> commands = RankDetector.serverCommands();
        for (String alias : aliases) if (commands.stream().anyMatch(command -> command.equalsIgnoreCase(alias))) return alias;
        return action;
    }

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (UiGraphics.scrollTooltip(scrollY)) return true;
        if (tab == 5 && logServerOpen && inRect(mouseX, mouseY, logServerMenuX(), logServerMenuY(), logServerMenuWidth(), logServerMenuHeight())) {
            logServerMenuScroll = clamp(logServerMenuScroll - (int) Math.signum(scrollY), 0,
                    Math.max(0, PunishLogClient.SERVERS.length - logServerMenuRows()));
            return true;
        }
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}

		if (tab == 8) { settingsPanel.wheel(scrollY); return true; }
        int direction = (int) Math.signum(scrollY);
		if (direction == 0) {
			return false;
		}

		if (tab == 0 && inRect(mouseX, mouseY, playerListX, playerListY, playerListW, playerListH)) {
			playerScroll = Math.max(0, playerScroll - direction);
			return true;
		}
		if (tab == 0 && inRect(mouseX, mouseY, ruleListX, ruleListY, ruleListW, ruleListH)) {
			ruleScroll = Math.max(0, ruleScroll - direction);
			return true;
		}
		if (tab == 1 && inRect(mouseX, mouseY, bindListX, bindListY, bindListW, bindListH)) {
			bindScroll = Math.max(0, bindScroll - direction);
			return true;
		}
		if (tab == 2 && itemCustomMode
				&& inRect(mouseX, mouseY, enchantPickerX, enchantPickerY, enchantPickerW, enchantPickerH)) {
			enchantPickScroll = Math.max(0, enchantPickScroll - direction);
			return true;
		}
		if (tab == 2 && itemCustomMode
				&& inRect(mouseX, mouseY, itemGridX, itemGridY, itemGridW, itemGridH)) {
			int columns = Math.max(1, (itemGridW - 14) / CELL);
			itemScroll = Math.max(0, itemScroll - direction);
			return true;
		}
		if (tab == 2 && !itemCustomMode
				&& inRect(mouseX, mouseY, itemGridX, itemGridY, itemGridW, itemGridH)) {
			customScroll = Math.max(0, customScroll - (int) Math.round(scrollY * CELL));
			return true;
		}
		if (tab == 3 && inRect(mouseX, mouseY, rankListX, rankListY, rankListW, rankListH)) {
			rankScroll = Math.max(0, rankScroll - direction);
			return true;
		}
		if (tab == 3 && inRect(mouseX, mouseY, rankCommandsX, rankCommandsY, rankCommandsW, rankCommandsH)) {
			rankCommandScroll = Math.max(0, rankCommandScroll - direction);
			return true;
		}
		if (tab == 3 && inRect(mouseX, mouseY, serverCommandsX, serverCommandsY, serverCommandsW, serverCommandsH)) {
			serverCommandScroll = Math.max(0, serverCommandScroll - direction);
			return true;
		}
		if (tab == 4 && inRect(mouseX, mouseY, rulesListX, rulesListY, rulesListW, rulesListH)) {
			rulesScroll = Math.max(0, rulesScroll - direction);
			return true;
		}
		if (tab == 6) {
			onlineScroll = Math.max(0, onlineScroll - (int) Math.round(scrollY * ONLINE_ROW));
			return true;
		}
		if (tab == 7) {
			historyScroll = Math.max(0, historyScroll - (int) Math.round(scrollY * (historyMode == 2 ? 12 : LOG_ROW)));
			return true;
		}
		if (tab == 5 && inRect(mouseX, mouseY, logListX, logListY, logListW, logListH)) {
			logScroll = Math.max(0, logScroll - (int) Math.round(scrollY * LOG_ROW));
			return true;
		}

		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

    @Override public void tick(){super.tick();if(closeReady&&minecraft.screen==this)minecraft.setScreen(null);}
    @Override public void onClose(){isClosing=true;}

	@Override
	public boolean shouldCloseOnEsc() {
        if (logServerOpen) { logServerOpen = false; return false; }
		if (punishTypeOpen) { punishTypeOpen = false; return false; }
		return true;
	}

	@Override
	public void removed() {
		// Deliberately no disposeInstance() — closing the PostChain here crashes
		// the render thread ("Buffer already closed"). The chain is re-acquired
		// lazily next open and shared buffers live with MC anyway.
		super.removed();
	}

	public void onDataChanged() {
		// The bind list is read directly from ClientBinds while rendering.
	}

	private boolean clickPlayerList(double mouseY) {
		List<ModerationPlayer> players = filteredPlayers();
		int index = playerScroll + (int) ((mouseY - playerListY) / PLAYER_ROW);
		if (index < 0 || index >= players.size()) {
			return true;
		}

		targetName = players.get(index).name();
		if (targetField != null) {
			targetField.setValue(targetName);
		}
		setStatus("Цель: " + targetName, TEXT);
		return true;
	}

	private boolean clickRuleList(double mouseY) {
		List<RulePreset> presets = RuleBook.all();
		int index = ruleScroll + (int) ((mouseY - ruleListY) / RULE_ROW);
		if (index < 0 || index >= presets.size()) {
			return true;
		}

		selectedRule = presets.get(index);
        guidedReasonConfirmed=false;
        punishReason="["+selectedRule.code+"] "+selectedRule.title;
        fillingRuleReason=true;
        try{if(reasonField!=null)reasonField.setValue(punishReason);}finally{fillingRuleReason=false;}
		ruleMode = true;
		// Selecting a rule also picks the matching punishment type so the
		// manual-type dropdown stays in sync.
		punishType = switch (selectedRule.action) {
			case MUTE -> "mute";
			case BAN -> "ban";
			case WARN -> "warn";
			case KICK -> "kick";
			case BANIP -> "banip";
			default -> punishType;
		};
		// And the duration default from the rule.
		if (!selectedRule.duration.isBlank() && durationField != null) {
			punishDuration = selectedRule.duration;
			durationField.setValue(punishDuration);
		}
		if (typeButton != null) {
			typeButton.setMessage(Component.literal(PunishmentTypes.buttonLabel(punishType)));
		}
		setStatus("Правило [" + selectedRule.code + "] выбрано", TEXT);
		return true;
	}

	private boolean clickBindList(double mouseY) {
		List<Binding> binds = ClientBinds.all();
		int index = bindScroll + (int) ((mouseY - bindListY) / MACRO_ROW);
		if (index < 0 || index >= binds.size()) {
			return true;
		}

		selectedBind = binds.get(index);
		macroTrigger = selectedBind.trigger == BindTrigger.PROJECTILE_HIT ? "use" : selectedBind.trigger == BindTrigger.KEY && selectedBind.radius > 0 ? "radius" : selectedBind.trigger.id();
		macroItem = selectedBind.item;
		macroCustomItem = selectedBind.customItem == null ? "" : selectedBind.customItem;
		macroCustomModel = selectedBind.customModelData;
		if (!macroCustomItem.isBlank()) {
			CustomItemPreset preset = CustomItems.byId(macroCustomItem);
			if (preset != null) {
				macroItem = preset.baseItem;
				macroCustomModel = preset.model;
			}
		}
		macroRadius = String.valueOf(selectedBind.radius);
        macroKeyCode = selectedBind.keyCode; macroKeyModifiers = selectedBind.keyModifiers; macroLookDistance=Double.toString(selectedBind.lookDistance);
        macroKeyMode = selectedBind.keyMode == null ? "PRESS" : selectedBind.keyMode;
        macroRepeats = Integer.toString(selectedBind.repeatCount);
        macroDelay = Integer.toString(selectedBind.repeatDelayMs);
		macroCommand = selectedBind.command;
        macroCommandDelays=selectedBind.commandDelaysMs==null?new java.util.ArrayList<>():new java.util.ArrayList<>(selectedBind.commandDelaysMs);
        macroName=selectedBind.id;macroDescription=selectedBind.description==null?"":selectedBind.description;
		setStatus("Загружен " + selectedBind.id + " — измените и нажмите «Сохранить»", TEXT);
		this.init(this.width, this.height);
		return true;
	}

	private boolean clickItemGrid(double mouseX, double mouseY) {
		int columns = Math.max(1, (itemGridW - 14) / CELL);
		int column = (int) ((mouseX - itemGridX) / CELL);
		int row = (int) ((mouseY - itemGridY) / CELL);
		int index = (itemScroll + row) * columns + column;
		if (index < 0 || index >= filteredItems.size()) {
			return true;
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(filteredItems.get(index));
		if (id != null) {
			selectedItem = id.toString();
			// In the "Add item" screen clicking a catalogue entry switches the
			// custom item's base immediately.
			if (itemCustomMode) {
				customBaseItem = selectedItem;
				setStatus("База: " + customBaseItem, GREEN);
			} else {
				setStatus("Выбран предмет: " + selectedItem, TEXT);
			}
		}
		return true;
	}

	private boolean clickEnchantPicker(double mouseY) {
		int rowH = 10;
		int index = enchantPickScroll + (int) ((mouseY - enchantPickerY - 2) / rowH);
		if (index < 0 || index >= enchantPickerIds.size()) {
			return true;
		}
		enchantPickSelected = enchantPickerIds.get(index);
		int level = 1;
		try {
			level = Math.max(1, Math.min(255, Integer.parseInt(
					customEnchantLevelField == null ? "1" : customEnchantLevelField.getValue().trim())));
		} catch (NumberFormatException ignored) {
		}
		// Replace or append "id:level" in the enchantments field.
		String current = customEnchantField == null ? "" : customEnchantField.getValue();
		java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
		for (String part : current.split("[,;]")) {
			String e = part.trim();
			if (e.isEmpty()) continue;
			String[] bits = e.split(":", 2);
			map.put(bits[0].trim().toLowerCase(java.util.Locale.ROOT),
					bits.length > 1 ? bits[1].trim() : "1");
		}
		map.put(enchantPickSelected, String.valueOf(level));
		StringBuilder sb = new StringBuilder();
		for (java.util.Map.Entry<String, String> e : map.entrySet()) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(e.getKey()).append(':').append(e.getValue());
		}
		if (customEnchantField != null) {
			customEnchantField.setValue(sb.toString());
		}
		int vanilla = vanillaMax(enchantPickSelected);
		setStatus(enchantPickSelected + ":" + level
				+ (level > vanilla ? " — выше ванильного максимума " + vanilla + " (креатив)" : ""), GOLD);
		return true;
	}

	private boolean clickCustomList(double mouseX, double mouseY, boolean doubleClick, boolean stack) {
		List<CustomItemPreset> presets = savedItems();
		int columns = Math.max(1, (itemGridW - 14) / CELL);
		int column = (int) ((mouseX - itemGridX) / CELL);
		int row = (int) ((mouseY - itemGridY) / CELL);
		int index = ((int) (mouseY - itemGridY) + customScroll) / CELL * columns + column;
		if (index < 0 || index >= presets.size()) {
			return true;
		}

		if(column>=columns)return true;
        if(selectedCustomItem==null || !selectedCustomItem.id.equals(presets.get(index).id))deleteItemArmed=0;
		selectedCustomItem = presets.get(index);
        long now=System.currentTimeMillis();
        boolean give=doubleClick || selectedCustomItem.id.equals(lastItemClick) && now-lastItemClickAt<450;
        lastItemClick=selectedCustomItem.id;lastItemClickAt=now;
        if(give){try{setStatus(CustomItems.give(minecraft,CustomItems.build(selectedCustomItem,stack?64:1)),TEXT);}catch(RuntimeException invalid){setStatus(invalid.getMessage(),RED);}return true;}
		if (selectedCustomItem != null) {
			customDraft = selectedCustomItem.copy();
			customBaseItem = selectedCustomItem.baseItem;
			setStatus("Загружен свой предмет: " + selectedCustomItem.name, TEXT);
			this.init(this.width, this.height);
		}
		return true;
	}

	private boolean clickRankList(double mouseY) {
		List<Rank> ranks = RankCatalog.all();
		int index = rankScroll + (int) ((mouseY - rankListY) / RANK_ROW);
		if (index < 0 || index >= ranks.size()) {
			return true;
		}

		selectedRank = ranks.get(index);
		rankCommandScroll = 0;
		serverCommandScroll = 0;
		setStatus("Просмотр ранга: " + selectedRank.name, TEXT);
		return true;
	}

	private boolean clickRankCommand(double mouseY) {
		if (selectedRank == null) {
			return true;
		}

		// Rebuild display items to find what was clicked
		List<Object> displayItems = new ArrayList<>();
		String lastGroup = null;
		for (RankCommand cmd : selectedRank.commands) {
			String group = cmd.group == null || cmd.group.isBlank() ? "Прочее" : cmd.group;
			if (!group.equals(lastGroup)) {
				displayItems.add(group);
				lastGroup = group;
			}
			displayItems.add(cmd);
		}

		int index = rankCommandScroll + (int) ((mouseY - rankCommandsY) / RANK_COMMAND_ROW);
		if (index < 0 || index >= displayItems.size()) {
			return true;
		}

		Object item = displayItems.get(index);
		if (item instanceof RankCommand command) {
			copyToClipboard(command.full());
		}
		return true;
	}

	private boolean clickServerCommand(double mouseX, double mouseY) {
		List<String> commands = RankDetector.serverCommands();
		int columns = Math.max(1, serverCommandsW / 140);
		int colW = serverCommandsW / columns;
		int col = Math.min(columns - 1, (int) ((mouseX - serverCommandsX) * columns / Math.max(1, serverCommandsW)));
		int row = (int) ((mouseY - serverCommandsY) / RANK_COMMAND_ROW);
		int index = (serverCommandScroll + row) * columns + col;
		if (index < 0 || index >= commands.size()) {
			return true;
		}

		copyToClipboard("/" + commands.get(index));
		return true;
	}

	private boolean clickRulesList(double mouseY) {
		List<RulePreset> presets = RuleBook.all();
		int index = rulesScroll + (int) ((mouseY - rulesListY) / 16);
		if (index < 0 || index >= presets.size()) {
			return true;
		}

		selectedRule = presets.get(index);
        guidedReasonConfirmed=false;
        punishReason="["+selectedRule.code+"] "+selectedRule.title;
        fillingRuleReason=true;
        try{if(reasonField!=null)reasonField.setValue(punishReason);}finally{fillingRuleReason=false;}
		ruleMode = true;
		setStatus("Правило [" + selectedRule.code + "] выбрано", TEXT);
		switchTab(0);
		return true;
	}

	private boolean clickLogList(double mouseY, boolean doubleClick) {
        List<PunishLogClient.Row> rows = filteredLogRows();
        int index = logIndex(rows,logListW,logScroll+mouseY-logListY);
        if (index < 0 || index >= rows.size()) return true;
        PunishLogClient.Row entry = rows.get(index);
        long now = net.minecraft.util.Util.getMillis();
        boolean repeated = entry.equals(lastClickedLogRow) && now - lastLogClickAt < 450;
        logSelected = entry;
        targetName = entry.punished();
        lastClickedLogRow = entry;
        lastLogClickAt = now;
        if (doubleClick || repeated) {
            historySearch = entry.punished();
            historyMode = 0;
            switchTab(7);
            fetchHistory();
        } else setStatus("Выбран " + entry.punished() + " | двойной клик — профиль", TEXT);
        return true;
    }

	// ------------------------------------------------------------------ actions

	private void sendPunish() {
		String target = targetName.trim();
		if (target.isEmpty()) {
			setStatus("Укажите ник игрока", RED);
			return;
		}
		if (ruleMode && selectedRule == null) {
			setStatus("Выберите пункт правил слева", RED);
			return;
		}
		if (ruleMode && selectedRule.action == RuleAction.NOTE) {
			setStatus("Этот пункт выдаётся вручную: " + selectedRule.note, RED);
			return;
		}
		if (!ruleMode && punishReason.trim().isEmpty()) {
			setStatus("Укажите причину — по правилу 3.10 она обязательна", RED);
			return;
		}

        if(ruleMode && EnvyWorld.config().client.guidedReasons && !guidedReasonConfirmed) {
            minecraft.setScreen(new ReasonChoiceScreen(this,selectedRule,target,reason->{punishReason=reason;guidedReasonConfirmed=true;if(reasonField!=null)reasonField.setValue(reason);})); return;
        }
		sendChatCommand(punishCommand());
	}

	private void sendUnban() {
		sendRemoval("unban", "Разбан отправлен в чат");
	}

	private void sendUnmute() {
		sendRemoval("unmute", "Снятие мута отправлено в чат");
	}

	private void sendRemoval(String action, String okText) {
		String target = targetName.trim();
		if (target.isEmpty()) {
			setStatus("Укажите ник игрока", RED);
			return;
		}

		String reason = appendSuffix(punishReason.trim());
		sendChatCommand(commandLine(action, target, "", reason), okText);
	}

	private String appendSuffix(String reason) {
		if (punishReasonSuffix.isBlank()) return reason;
		return reason.isBlank() ? punishReasonSuffix.trim() : reason + " " + punishReasonSuffix.trim();
	}

	/**
	 * Types a command into chat exactly like the player would. The leading slash is
	 * stripped because {@code ClientPacketListener#sendCommand} adds it itself.
	 */
	private void sendChatCommand(String command) {
		sendChatCommand(command, "Команда отправлена в чат");
	}

	private void sendChatCommand(String command, String okText) {
		if (this.minecraft == null || this.minecraft.getConnection() == null) {
			setStatus("Нет соединения с сервером", RED);
			return;
		}
		if (command == null || command.isBlank()) {
			setStatus("Команда не сформирована", RED);
			return;
		}

		String value = command.startsWith("/") ? command.substring(1) : command;
		try {
			this.minecraft.getConnection().sendCommand(value);
			setStatus(okText + ": /" + value, GREEN);
		} catch (RuntimeException exception) {
			setStatus("Не удалось отправить команду в чат", RED);
		}
	}

	private String commandBase="",commandOverride;
    private boolean syncingCommand;
    private EditBox commandDraftField;
    private String punishCommand(){String generated=generatedPunishCommand();return commandOverride!=null && generated.equals(commandBase)?commandOverride:generated;}
    private String generatedPunishCommand() {
		String target = targetName.isBlank() ? "<ник>" : targetName.trim();
		String reason = punishReason.trim();

		if (ruleMode) {
			if (selectedRule == null) {
				return "Выберите пункт правил слева";
			}
			if (selectedRule.action == RuleAction.NOTE) {
				return "Правило требует ручного решения"
						+ (selectedRule.note.isBlank() ? "" : ": " + selectedRule.note);
			}

			String action = ruleActionCommand(selectedRule.action);
			boolean instant = selectedRule.action == RuleAction.WARN || selectedRule.action == RuleAction.KICK;
			String duration = instant ? "" : (punishDuration.isBlank() ? selectedRule.duration : punishDuration.trim());
			String defaultReason = "[" + selectedRule.code + "] " + selectedRule.title;
			String finalReason = appendSuffix(reason.isEmpty() ? defaultReason : reason);
			return commandLine(action, target, duration, finalReason);
		}

		boolean instant = !PunishmentTypes.get(punishType).timed();
		return commandLine(punishType, target,
				instant ? "" : (punishDuration.isBlank() ? "1h" : punishDuration.trim()), appendSuffix(reason));
	}

	private static String commandLine(String action, String target, String duration, String reason) {
		StringBuilder command = new StringBuilder("/").append(resolveServerAction(action)).append(' ').append(target);

		if (duration != null && !duration.isBlank() && !duration.equals("-")) {
			command.append(' ').append(duration);
		}
		if (reason != null && !reason.isBlank()) {
			command.append(' ').append(reason);
		}
		return command.toString();
	}

	private static String ruleActionCommand(RuleAction action) {
		return switch (action) {
			case MUTE -> "mute";
			case BAN -> "ban";
			// Keep the requested IP punishment type.
			case BANIP -> "banip";
			case WARN -> "warn";
			case KICK -> "kick";
			case NOTE -> "note";
		};
	}

	private static String ruleActionName(RuleAction action) {
		return switch (action) {
			case MUTE -> "MUTE";
			case BAN -> "BAN";
			case BANIP -> "BANIP";
			case WARN -> "WARN";
			case KICK -> "KICK";
			case NOTE -> "ВРУЧНУЮ";
		};
	}

	private void newCustomItem() {
		selectedCustomItem = null;
		customDraft = null;
		customBaseItem = selectedItem;
		customColor = "LIGHT_PURPLE";
		customUnbreakable = false;
		customGlint = true;
		setStatus("Новый свой предмет — заполните поля и нажмите «Сохранить»", TEXT);
		this.init(this.width, this.height);
	}

	private CustomItemPreset draftFromFields() {
		CustomItemPreset preset = customDraft != null ? customDraft.copy()
				: (selectedCustomItem == null ? new CustomItemPreset() : selectedCustomItem.copy());
		preset.id = selectedCustomItem == null ? CustomItems.nextId() : selectedCustomItem.id;
		String name = customNameField == null ? "" : customNameField.getValue().trim();
		preset.name = name.isBlank() ? defaultCustomName() : name;
		preset.baseItem = customBaseItem == null || customBaseItem.isBlank() ? selectedItem : customBaseItem;
		preset.lore = customLoreField == null ? "" : customLoreField.getValue().trim();
		preset.enchantments = customEnchantField == null ? "" : customEnchantField.getValue().trim();
		preset.model = clamp(parseInt(customModelField == null ? "" : customModelField.getValue(),
				defaultCustomModel()), 0, 9);
		preset.count = clamp(parseInt(customCountField == null ? "" : customCountField.getValue(), 1), 1, 64);
		preset.color = customColor;
		preset.unbreakable = customUnbreakable;
		preset.glint = customGlint;
		return preset;
	}

	private void captureCustomDraft() {
		if (customNameField != null) {
			customDraft = draftFromFields();
		}
	}

	private void saveCustomItem() {
		CustomItemPreset draft = draftFromFields();
		if (draft.name == null || draft.name.isBlank()) {
			setStatus("Введите название предмета", RED);
			return;
		}

		if (selectedCustomItem == null) {
			draft.id = CustomItems.nextId();
			EnvyWorld.config().customItems.add(draft);
		} else {
			draft.id = selectedCustomItem.id;
			int index = EnvyWorld.config().customItems.indexOf(selectedCustomItem);
			if (index >= 0) {
				EnvyWorld.config().customItems.set(index, draft);
			} else {
				EnvyWorld.config().customItems.add(draft);
			}
		}

		EnvyWorld.saveConfig();
		selectedCustomItem = draft;
		customDraft = null;
		customBaseItem = draft.baseItem;
		setStatus("Сохранено: " + draft.name + "  |  " + shortItemId(draft.baseItem)
				+ "  |  model " + draft.model, GREEN);
		this.init(this.width, this.height);
	}

	private void giveCustomItem() {
		CustomItemPreset draft = draftFromFields();
		if (draft.name == null || draft.name.isBlank()) {
			setStatus("Введите название предмета", RED);
			return;
		}

		int count = shiftDown() ? 64 : draft.count;
		giveStack(CustomItems.build(draft, count));
	}

	private void deleteCustomItem() {
		if (selectedCustomItem == null && customDraft == null) {
			setStatus("Выберите свой предмет в списке слева", RED);
			return;
		}

		String name = selectedCustomItem != null ? selectedCustomItem.name : customDraft.name;
		if (selectedCustomItem != null) {
			EnvyWorld.config().customItems.remove(selectedCustomItem);
		}
		EnvyWorld.saveConfig();
		selectedCustomItem = null;
		customDraft = null;
		setStatus("Удалён свой предмет: " + name, GREEN);
		this.init(this.width, this.height);
	}

	private void bindCustomItemToMacro() {
		CustomItemPreset draft = draftFromFields();
		if (draft.name == null || draft.name.isBlank()) {
			setStatus("Введите название предмета", RED);
			return;
		}

		if (selectedCustomItem == null) {
			draft.id = CustomItems.nextId();
			EnvyWorld.config().customItems.add(draft);
		} else {
			draft.id = selectedCustomItem.id;
			int index = EnvyWorld.config().customItems.indexOf(selectedCustomItem);
			if (index >= 0) {
				EnvyWorld.config().customItems.set(index, draft);
			} else {
				EnvyWorld.config().customItems.add(draft);
			}
		}

		EnvyWorld.saveConfig();
		selectedCustomItem = draft;
		customDraft = null;
		customBaseItem = draft.baseItem;
		macroItem = draft.baseItem;
		macroCustomItem = draft.id;
		macroCustomModel = draft.model;
		switchTab(1);
		setStatus("Свой предмет привязан к макросу: " + draft.name, GREEN);
	}

	private void giveStack(ItemStack stack) {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.getConnection() == null) {
			setStatus("Нет соединения с сервером", RED);
			return;
		}
		if (!this.minecraft.player.isCreative()) {
			setStatus("Выдача предметов доступна только в креативе", RED);
			return;
		}
		if (stack == null || stack.isEmpty()) {
			setStatus("Предмет не сформирован", RED);
			return;
		}

		Inventory inventory = this.minecraft.player.getInventory();
		int inventorySlot = firstEmptySlot(inventory);
		int menuSlot = inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;

		inventory.setItem(inventorySlot, stack.copy());
		this.minecraft.gameMode.handleCreativeModeItemAdd(stack, menuSlot);
		setStatus("Выдано в инвентарь: " + stack.getHoverName().getString()
				+ " ×" + stack.getCount(), GREEN);
	}

	private void copyToClipboard(String command) {
		if (this.minecraft == null || command == null || command.isBlank()) {
			return;
		}
		this.minecraft.keyboardHandler.setClipboard(command);
		setStatus("Скопировано: " + command, GREEN);
	}

	private String defaultCustomName() {
		Identifier id = Identifier.tryParse(selectedItem);
		if (id == null) {
			return "Мой предмет";
		}
		Item item = BuiltInRegistries.ITEM.getValue(id);
		if (item == null || item == Items.AIR) {
			return "Мой предмет";
		}
		return new ItemStack(item).getHoverName().getString() + " +";
	}

	private int defaultCustomModel() {
		return isStickLike(selectedItem) ? 1 : 0;
	}

	private static String nextCustomColor(String current) {
		List<ChatFormatting> colours = CustomItems.colours();
		for (int index = 0; index < colours.size(); index++) {
			if (colours.get(index).name().equalsIgnoreCase(current)) {
				return colours.get((index + 1) % colours.size()).name();
			}
		}
		return colours.get(0).name();
	}

	private int previewColor() {
		try {
			Integer colour = ChatFormatting.valueOf(customColor.toUpperCase(Locale.ROOT)).getColor();
			if (colour != null) {
				return 0xFF000000 | colour;
			}
		} catch (RuntimeException ignored) {
			// Fall through.
		}
		return TEXT;
	}

	/**
	 * Summary like "sharpness 5/255, unbreaking 3/3" for the item preview.
	 * Levels above the vanilla cap are flagged "(креатив)".
	 */
	private static String enchantmentSummary(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (String part : raw.split("[,;]")) {
			String entry = part.trim();
			if (entry.isEmpty()) {
				continue;
			}
			String[] bits = entry.split(":", 2);
			String id = bits[0].trim();
			int level = 1;
			if (bits.length > 1) {
				try {
					level = Integer.parseInt(bits[1].trim());
				} catch (NumberFormatException ignored) {
				}
			}
			int vanilla = vanillaMax(id);
			if (sb.length() > 0) {
				sb.append("  |  ");
			}
			sb.append(id).append(' ').append(level).append('/').append(vanilla);
			if (level > vanilla) {
				sb.append(" (креатив)");
			}
		}
		return sb.toString();
	}

	private static int vanillaMax(String enchantmentId) {
		return switch (enchantmentId.toLowerCase(java.util.Locale.ROOT)
				.replace("minecraft:", "")) {
			case "sharpness" -> 5;
			case "smite" -> 5;
			case "bane_of_arthropods" -> 5;
			case "knockback" -> 2;
			case "fire_aspect" -> 2;
			case "looting" -> 3;
			case "sweeping", "sweeping_edge" -> 3;
			case "efficiency" -> 5;
			case "silk_touch" -> 1;
			case "unbreaking" -> 3;
			case "fortune" -> 3;
			case "power" -> 5;
			case "punch" -> 2;
			case "flame" -> 1;
			case "infinity" -> 1;
			case "luck_of_the_sea" -> 3;
			case "lure" -> 3;
			case "depth_strider" -> 3;
			case "frost_walker" -> 2;
			case "mending" -> 1;
			case "binding_curse", "vanishing_curse" -> 1;
			case "protection" -> 4;
			case "fire_protection" -> 4;
			case "blast_protection" -> 4;
			case "projectile_protection" -> 4;
			case "respiration" -> 3;
			case "aqua_affinity" -> 1;
			case "thorns" -> 3;
			case "feather_falling" -> 4;
			case "soul_speed" -> 3;
			case "swift_sneak" -> 3;
			case "loyalty" -> 3;
			case "impaling" -> 5;
			case "riptide" -> 3;
			case "channeling" -> 1;
			case "multishot" -> 1;
			case "quick_charge" -> 3;
			case "piercing" -> 4;
			case "density" -> 5;
			case "breach" -> 4;
			case "wind_burst" -> 3;
			default -> 1;
		};
	}

	private static String shortItemId(String id) {
		if (id == null) {
			return "—";
		}
		return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
	}

	private static final java.util.Set<String> STICK_LIKE = java.util.Set.of(
			"minecraft:stick", "minecraft:debug_stick",
			"minecraft:carrot_on_a_stick", "minecraft:warped_fungus_on_a_stick",
			"minecraft:blaze_rod", "minecraft:breeze_rod", "minecraft:fishing_rod",
			"minecraft:bamboo", "minecraft:bone", "minecraft:mace", "minecraft:trident");

	private static boolean isStickLike(String itemId) {
		return itemId != null && STICK_LIKE.contains(itemId.toLowerCase(Locale.ROOT));
	}

	private void sendGiveItem(int count) {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.getConnection() == null) {
			setStatus("Нет соединения с сервером", RED);
			return;
		}
		if (!this.minecraft.player.isCreative()) {
			setStatus("Выдача предметов доступна только в креативе", RED);
			return;
		}

		Identifier id = Identifier.tryParse(selectedItem);
		if (id == null) {
			setStatus("Неизвестный предмет: " + selectedItem, RED);
			return;
		}

		Item item = BuiltInRegistries.ITEM.getValue(id);
		if (item == null || item == Items.AIR) {
			setStatus("Неизвестный предмет: " + selectedItem, RED);
			return;
		}

		int amount = Math.max(1, Math.min(count, item.getDefaultMaxStackSize()));
		ItemStack stack = new ItemStack(item, amount);
		Inventory inventory = this.minecraft.player.getInventory();

		int inventorySlot = firstEmptySlot(inventory);
		int menuSlot = inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;

		// Update the local copy for instant feedback; the server confirms via the
		// creative-mode slot packet. No /give command is used.
		inventory.setItem(inventorySlot, stack.copy());
		this.minecraft.gameMode.handleCreativeModeItemAdd(stack, menuSlot);

		setStatus("Выдано в инвентарь: " + stack.getHoverName().getString() + " ×" + amount, GREEN);
	}

	private static int firstEmptySlot(Inventory inventory) {
		for (int slot = 0; slot < 36; slot++) {
			if (inventory.getItem(slot).isEmpty()) {
				return slot;
			}
		}
		return inventory.getSelectedSlot();
	}

	private void newMacro() {
        macroName="";macroDescription="";macroCommandDelays=new java.util.ArrayList<>();
		selectedBind = null;
		macroTrigger = "attack";
		macroItem = heldItemId();
		macroCustomItem = "";
		macroCustomModel = 0;
		macroRadius = "0";
		macroCommand = "";
		setStatus("Новый макрос", TEXT);
        macroKeyMode = "PRESS"; macroRepeats = "1"; macroDelay = "1000"; macroKeyCode=0; macroKeyModifiers=0; macroLookDistance="64";
		this.init(this.width, this.height);
	}

	private void createMacro() {
		if (macroItem.isBlank()) {
			setStatus("Выберите предмет (каталог или «В руке»)", RED);
			return;
		}
		if (macroCommand.isBlank()) {
			setStatus("Введите команду макроса", RED);
			return;
		}

		BindTrigger trigger = BindTrigger.byId(macroTrigger);
		if (trigger == null) {
			trigger = BindTrigger.ATTACK_ENTITY;
		}

		String command = macroCommand.trim();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}

		if(!macroName.isBlank()&&EnvyWorld.config().binds.stream().anyMatch(b->b!=selectedBind&&b.id.equals(macroName))){setStatus("Макрос с таким названием уже существует",RED);return;}
        int radius = trigger == BindTrigger.RADIUS ? Math.max(0, parseInt(macroRadius, 0)) : 0;

		if (selectedBind == null) {
			Binding binding = new Binding(macroName.isBlank()?nextBindId():macroName, trigger, macroItem, command);
			binding.radius = radius;
            binding.author=minecraft.getUser().getName();
            binding.keyMode = macroKeyMode;
            binding.repeatCount = clamp(parseInt(macroRepeats, 1), 0, 10_000);
            binding.repeatDelayMs = clamp(parseInt(macroDelay, 1000), 50, 3_600_000);
			binding.customItem = macroCustomItem;
			binding.customModelData = macroCustomModel;
			EnvyWorld.config().binds.add(binding);
			selectedBind = binding;
			EnvyWorld.saveConfig();
			setStatus("Макрос " + binding.id + " сохранён локально", GREEN);
		} else {
			selectedBind.trigger = trigger;
			selectedBind.item = macroItem;
			selectedBind.command = command;
			selectedBind.radius = radius;
            selectedBind.keyMode = macroKeyMode;
            selectedBind.repeatCount = clamp(parseInt(macroRepeats, 1), 0, 10_000);
            selectedBind.repeatDelayMs = clamp(parseInt(macroDelay, 1000), 50, 3_600_000);
			selectedBind.customItem = macroCustomItem;
			selectedBind.customModelData = macroCustomModel;
			EnvyWorld.saveConfig();
			setStatus("Макрос " + selectedBind.id + " обновлён локально", GREEN);
		}

        if(!macroName.isBlank())selectedBind.id=macroName;selectedBind.description=macroDescription;selectedBind.commandDelaysMs=new java.util.ArrayList<>(macroCommandDelays);
        selectedBind.keyCode=macroKeyCode; selectedBind.keyModifiers=macroKeyModifiers;
        try { selectedBind.lookDistance=Math.max(1,Math.min(128,Double.parseDouble(macroLookDistance))); } catch(NumberFormatException ignored) { selectedBind.lookDistance=64; }
        EnvyWorld.saveConfig();

	}

	private void deleteMacro() {
		if (selectedBind == null) {
			setStatus("Выберите макрос в списке слева", RED);
			return;
		}

		String id = selectedBind.id;
		EnvyWorld.config().binds.remove(selectedBind);
		EnvyWorld.saveConfig();
		setStatus("Макрос " + id + " удалён локально", GREEN);
		selectedBind = null;
	}

    private String macroKeyLabel() { return macroKeyCode==-1 && !macroTrigger.equals("key") && !macroTrigger.equals("radius") && !macroTrigger.equals("command") ? "Не назначена" : com.envyworld.client.MacroKeys.label(macroKeyCode,macroKeyModifiers); }
    private String macroModeLabel() { return switch (macroKeyMode) { case "HOLD" -> "Удерживать"; case "TOGGLE" -> "Переключение"; default -> "Нажатие"; }; }

	private String nextBindId() {
		int next = 1;

		for (Binding binding : EnvyWorld.config().binds) {
			if (binding.id.startsWith("bind")) {
				try {
					next = Math.max(next, Integer.parseInt(binding.id.substring(4)) + 1);
				} catch (NumberFormatException ignored) {
					// Custom ids are allowed.
				}
			}
		}

		return "bind" + next;
	}

	private String macroPreview() {
		String item = macroItemLabel();
		String command = macroCommand == null || macroCommand.isBlank() ? "<команда>" : macroCommand.trim();
		String radius = macroTrigger.equals("radius") ? " Радиус: " + parseInt(macroRadius, 0) : macroTrigger.equals("key") ? " Дальность: " + macroLookDistance : "";

		return "Триггер: " + triggerLabel(macroTrigger) + radius + "  Предмет: " + item + "  Команда: " + command;
	}

	private String macroItemLabel() {
		if (macroCustomItem != null && !macroCustomItem.isBlank()) {
			CustomItemPreset preset = CustomItems.byId(macroCustomItem);
			if (preset != null) {
				return com.envyworld.items.ItemText.parse(preset.name,preset.color).getString() + "  |  " + shortItemId(preset.baseItem) + "  |  model " + preset.model;
			}
			return macroCustomItem + "  |  " + shortItemId(macroItem);
		}
		return macroItem == null || macroItem.isBlank() ? "<предмет>" : macroItem.equals("any") ? "Любой предмет" : macroItem;
	}

	private String customItemName(String id) {
		CustomItemPreset preset = CustomItems.byId(id);
		return preset == null ? id : com.envyworld.items.ItemText.parse(preset.name,preset.color).getString();
	}

	private void switchTab(int newTab) {
		if (this.tab == newTab) {
			return;
		}

		if (this.tab == 2 && itemCustomMode) {
			captureCustomDraft();
		}
		this.tab = newTab;
        this.logServerOpen = false;
		this.statusText = "";
		this.init(this.width, this.height);
	}

	private void setStatus(String text, int color) {
		this.statusText = text;
		this.statusColor = color;
	}

	// ------------------------------------------------------------------ helpers

	private record ModerationPlayer(String name, boolean near) {}
    private List<ModerationPlayer> filteredPlayers() {
        if(minecraft==null || minecraft.getConnection()==null)return List.of();
        var local=new java.util.TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for(var info:minecraft.getConnection().getListedOnlinePlayers())local.add(info.getProfile().name());
        var names=new java.util.TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(com.envyworld.logs.OnlineScanner.online());names.addAll(local);
        String query=playerQuery.trim().toLowerCase(Locale.ROOT);
        return names.stream().filter(n->n.toLowerCase(Locale.ROOT).contains(query))
            .map(n->new ModerationPlayer(n,local.contains(n)))
            .sorted(java.util.Comparator.comparing(ModerationPlayer::near).reversed().thenComparing(ModerationPlayer::name,String.CASE_INSENSITIVE_ORDER)).toList();
    }

	private void rebuildItems() {
		String query = itemQuery.trim().toLowerCase(Locale.ROOT);
		List<Item> found = new ArrayList<>();

		for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id == null) {
				continue;
			}
			// Full catalogue — no filtering by item type.
			if (!query.isEmpty()) {
				String hover = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
				if (!id.toString().contains(query) && !hover.contains(query)) {
					continue;
				}
			}
			found.add(item);
		}

		found.sort(Comparator.comparing(item -> String.valueOf(BuiltInRegistries.ITEM.getKey(item))));
		this.filteredItems = found;
		this.itemScroll = 0;
	}

	private String heldItemId() {
		if (this.minecraft == null || this.minecraft.player == null) {
			return "minecraft:diamond_sword";
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(this.minecraft.player.getMainHandItem().getItem());
		return id == null ? "minecraft:diamond_sword" : id.toString();
	}

	private int tabAt(double mouseX, double mouseY) {
		if (mouseY < panelY || mouseY > panelY + TOPBAR_H) {
			return -1;
		}
		for (int i = 0; i < TABS.length; i++) {
			if (navHitW[i] <= 0) continue;
			if (mouseX >= navHitX[i] && mouseX < navHitX[i] + navHitW[i]
					&& mouseY >= navHitY[i] && mouseY < navHitY[i] + navHitH[i]) {
				return i;
			}
		}
		return -1;
	}

	private static boolean inRect(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private boolean shiftDown() {
		if (this.minecraft == null) {
			return false;
		}
		long window = this.minecraft.getWindow().handle();
		return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
				|| GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
	}

    private static String triggerLabel(String id){return switch(id){case "attack"->"Атака";case "projectile"->"Попадание";case "radius"->"Радиус";case "key"->"Взгляд";case "command"->"Клавиша";case "break"->"Разрушение";case "use"->"Использование";default->id;};}
    private void drawRuleColumns(GuiGraphics g,RulePreset p,int x,int y,int width,int height){
        int code=38,type=44,duration=90;float yy=UiFont.centeredY(y,height-1,UiFont.SIZE);
        UiFont.draw(g,truncate(p.code,code-5),x+7,yy,TEXT);
        int color=switch(p.action){case MUTE->0xFFAAC7E5;case BAN,BANIP->0xFFF195A5;case WARN->0xFFDAC18C;case NOTE->MUTED;default->TEXT;};
        UiFont.draw(g,p.action.name(),x+code,yy,color);UiFont.draw(g,truncate(durationRangeLabel(p),duration-8),x+code+type,yy,GOLD);
        UiFont.draw(g,truncate(p.title,width-code-type-duration-7),x+code+type+duration,yy,p.action==RuleAction.NOTE?MUTED:TEXT);
    }

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static String nextPunishType(String current) {
		for (int index = 0; index < PUNISH_TYPES.length; index++) {
			if (PUNISH_TYPES[index].equals(current)) {
				return PUNISH_TYPES[(index + 1) % PUNISH_TYPES.length];
			}
		}
		return PUNISH_TYPES[0];
	}

	private static String nextMacroTrigger(String current) {
		String[] triggers = {"attack", "radius", "key", "command", "use"};
		for (int index = 0; index < triggers.length; index++) {
			if (triggers[index].equals(current)) {
				return triggers[(index + 1) % triggers.length];
			}
		}
		return triggers[0];
	}

	/** Short "1h - 5h" / "perm" label for the rules list. */
	private static String durationRangeLabel(RulePreset preset) {
		String base = preset.duration == null ? "" : preset.duration.trim();
		String min = preset.min == null ? "" : preset.min.trim();
		String max = preset.max == null ? "" : preset.max.trim();
		if (!min.isBlank() && !max.isBlank() && !min.equalsIgnoreCase(max)) {
			return min + " - " + max + (base.isBlank() ? "" : " (" + base + ")");
		}
		if (!base.isBlank()) {
			return base;
		}
		return "";
	}

	private String truncate(String text, int maxWidth) {
		if (text == null) {
			return "";
		}
		if (maxWidth <= 0) {
			return "";
		}
		if (UiFont.width(text) <= maxWidth) {
			return text;
		}

		String ellipsis = "...";
		StringBuilder builder = new StringBuilder();
		for (char character : text.toCharArray()) {
			if (UiFont.width(builder.toString() + character + ellipsis) > maxWidth) {
				break;
			}
			builder.append(character);
		}
		return builder + ellipsis;
	}

	private String defaultHint() {
		return switch (tab) {
			case 0 -> "Ранг: " + RankDetector.displayName() + "  |  команда вводится в чат от вашего имени";
			case 1 -> "Макросы хранятся локально  |  V — запуск  |  у своих предметов отдельная модель";
			case 2 -> "Только креатив  |  Shift по кнопке — 64 шт.  |  палки, трезубец и жезлы сверху  |  модели 1 - 9";
			case 3 -> "Сервер: " + RankDetector.detectedMode() + "  |  ранг читается из таба/бара и дерева команд";
			case 4 -> "Кликните пункт правил, чтобы перейти к наказанию";
			case 5 -> "Все наказания  |  обновление каждые ~10 с  |  клик по строке — выбрать игрока";
			case 6 -> "";
			case 7 -> "Двойной клик в логах или онлайне — открыть профиль";
            default -> "Настройки сохраняются автоматически  |  ПКМ по блоку в редакторе HUD — оформление";
		};
	}
}
