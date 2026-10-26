package com.envyworld.items;

import com.envyworld.EnvyWorld;
import com.google.gson.Gson;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds client-side custom ItemStacks from saved presets. The stack is put into
 * the creative inventory with the normal creative slot packet, never with /give.
 */
public final class CustomItems {
	private static final Gson GSON = new Gson();
    private static final java.util.Map<String, ItemStack> BUILT_CACHE = new java.util.LinkedHashMap<>();
    private static Object registryIdentity;

	private CustomItems() {
	}

	/**
	 * Removes untouched bundled examples once, preserving edited and user-created presets.
	 */
	public static void seedDefaults(Logger logger) {
        // Starter examples were removed from the catalog. Keep user-authored presets.
        if (!EnvyWorld.config().starterItemsRemoved) {
            try {
                java.nio.file.Path config = EnvyWorld.dataDir().resolve("config.json");
                if (java.nio.file.Files.exists(config)) java.nio.file.Files.copy(config,
                        EnvyWorld.dataDir().resolve("config-before-starter-removal-" + System.currentTimeMillis() + ".json"));
                var bundled = new java.util.HashMap<String, CustomItemPreset>();
                try (InputStream stream = CustomItems.class.getResourceAsStream("/envyworld/items.json")) {
                    if (stream != null) for (var preset : GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), CustomItemPreset[].class)) bundled.put(preset.id, preset);
                }
                EnvyWorld.config().customItems.removeIf(preset -> preset != null && bundled.containsKey(preset.id)
                        && GSON.toJson(preset).equals(GSON.toJson(bundled.get(preset.id))));
                EnvyWorld.config().starterItemsRemoved = true;
                EnvyWorld.config().customItemsSeeded = true;
                EnvyWorld.saveConfig();
            } catch (Exception exception) { logger.error("Failed to migrate starter presets", exception); }
        }
	}

	public static ItemStack build(CustomItemPreset preset, int requestedCount) {
		if (preset == null) {
			return new ItemStack(Items.STICK);
		}

		Identifier id = Identifier.tryParse(preset.baseItem == null ? "" : preset.baseItem.trim());
		Item item = id == null ? Items.STICK : BuiltInRegistries.ITEM.getValue(id);
		if (item == null || item == Items.AIR) {
			item = Items.STICK;
		}

		int max = Math.max(1, item.getDefaultMaxStackSize());
		int count = Math.max(1, Math.min(requestedCount <= 0 ? preset.count : requestedCount, max));
		CustomItemPreset before = preset.importedState == null || preset.importedState.isBlank() ? null : GSON.fromJson(preset.importedState, CustomItemPreset.class);
        ItemStack stack = new ItemStack(item, count);
        if (before != null && preset.rawStack != null && !preset.rawStack.isBlank()) {
            try { stack = ItemStack.CODEC.parse(registryProvider().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), net.minecraft.nbt.TagParser.parseCompoundFully(preset.rawStack)).getOrThrow(); }
            catch (Exception error) { throw new IllegalArgumentException("Не удалось прочитать импортированный предмет", error); }
            if (!preset.baseItem.equals(before.baseItem)) { var replacement = new ItemStack(item,count); replacement.applyComponents(stack.getComponentsPatch()); stack = replacement; }
        }

		Component name = ItemText.font(ItemText.parse(preset.name == null || preset.name.isBlank() ? "Предмет" : preset.name, preset.color),preset.nameFont);
        if (before == null || !java.util.Objects.equals(preset.name,before.name) || !java.util.Objects.equals(preset.color,before.color) || !preset.nameFont.equals(before.nameFont)) stack.set(DataComponents.CUSTOM_NAME, name);

		List<Component> lore = new ArrayList<>();
		if (preset.lore != null) {
			for (String line : preset.lore.split("\\|")) {
				String value = line.trim();
				if (!value.isEmpty()) {
					lore.add(ItemText.font(ItemText.parse(value, "GRAY"),preset.loreFont));
				}
			}
		}
        if (before == null || !java.util.Objects.equals(preset.lore,before.lore) || !preset.loreFont.equals(before.loreFont)) { if(lore.isEmpty()) stack.remove(DataComponents.LORE); else stack.set(DataComponents.LORE,new ItemLore(lore)); }

		if(before == null || !java.util.Objects.equals(preset.enchantments,before.enchantments)) { stack.remove(DataComponents.ENCHANTMENTS); applyEnchantments(stack, preset.enchantments); }

		if (preset.model > 0 && (before == null || preset.model != before.model)) {
			stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
					List.of((float) preset.model), List.of(), List.of(), List.of()));
			stack.set(DataComponents.RARITY, Rarity.EPIC);
			// Force the custom item model directly — survives servers/resource packs
			// that ship their own minecraft:items/stick.json.
			String[] names = {"emerald", "amethyst", "fire", "ice", "gold", "blood",
					"ender", "rainbow", "netherite"};
			if (preset.model <= names.length) {
				stack.set(DataComponents.ITEM_MODEL,
						Identifier.fromNamespaceAndPath("envyworld", "stick_" + names[preset.model - 1]));
			}
		}

		if(before == null || preset.glint != before.glint) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, preset.glint);
		if(before == null || preset.unbreakable != before.unbreakable) { if(preset.unbreakable) stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE); else stack.remove(DataComponents.UNBREAKABLE); }
        if (preset.components != null && !preset.components.isBlank()) {
            try { stack.applyComponentsAndValidate(parseComponents(preset)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException | IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Неверные компоненты: " + invalid.getMessage(), invalid);
            }
        }

        ItemOptions.apply(preset,before,stack);
        // Raw imported component patches may still contain the old custom_name.
        // An explicit edit in the text form wins over that preserved import.
        if(before==null || !java.util.Objects.equals(preset.name,before.name) || !java.util.Objects.equals(preset.color,before.color) || !java.util.Objects.equals(preset.nameFont,before.nameFont))stack.set(DataComponents.CUSTOM_NAME,name);
        stack.setCount(Math.max(1, Math.min(requestedCount <= 0 ? preset.count : requestedCount, stack.getMaxStackSize())));
		return stack;
	}

    public static CustomItemPreset fromStack(ItemStack stack, String author) {
        if(stack.isEmpty()) throw new IllegalArgumentException("Пустой слот");
        var p=new CustomItemPreset(); p.id=nextId(); p.baseItem=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        p.name=ItemText.codes(stack.getHoverName()); p.color="#FFFFFF"; p.count=stack.getCount(); p.model=modelOf(stack);
        var lore=stack.get(DataComponents.LORE); p.lore=lore==null?"":lore.lines().stream().map(ItemText::codes).collect(java.util.stream.Collectors.joining("|"));
        var ench=stack.get(DataComponents.ENCHANTMENTS); if(ench!=null) p.enchantments=ench.entrySet().stream().map(e->e.getKey().getRegisteredName()+":"+e.getIntValue()).collect(java.util.stream.Collectors.joining(","));
        p.glint=stack.hasFoil(); p.unbreakable=stack.has(DataComponents.UNBREAKABLE);
        var itemName=stack.get(DataComponents.ITEM_NAME);p.itemName=itemName==null?"":ItemText.codes(itemName);
        var attributes=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if(attributes!=null)for(var entry:attributes.modifiers()){
            var a=new CustomItemPreset.AttributeSpec();a.type=entry.attribute().getRegisteredName();a.amount=entry.modifier().amount();
            a.operation=entry.modifier().operation().getSerializedName();a.slot=entry.slot().getSerializedName();p.attributes.add(a);
        }
        var food=stack.get(DataComponents.FOOD);if(food!=null){p.foodEnabled=true;p.nutrition=food.nutrition();p.saturation=food.saturation();p.alwaysEdible=food.canAlwaysEat();}
        var tool=stack.get(DataComponents.TOOL);if(tool!=null){p.toolEnabled=true;p.miningSpeed=tool.defaultMiningSpeed();p.damagePerBlock=tool.damagePerBlock();
            if(!tool.rules().isEmpty())p.toolBlocks=tool.rules().getFirst().blocks().unwrap().map(tag->"#"+tag.location(),holders->holders.stream().map(h->h.getRegisteredName()).collect(java.util.stream.Collectors.joining(",")));
        }
        p.author=author; p.createdAt=System.currentTimeMillis();
        var display=stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY,net.minecraft.world.item.component.TooltipDisplay.DEFAULT);
        p.hideTooltip=display.hideTooltip(); p.hideEnchantments=display.hiddenComponents().contains(DataComponents.ENCHANTMENTS);
        p.hideAttributes=display.hiddenComponents().contains(DataComponents.ATTRIBUTE_MODIFIERS); p.hideCanPlace=display.hiddenComponents().contains(DataComponents.CAN_PLACE_ON); p.hideCanBreak=display.hiddenComponents().contains(DataComponents.CAN_BREAK);
        p.importedState=GSON.toJson(p); p.rawStack=nbt(stack); return p;
    }

    public static String give(Minecraft client, ItemStack stack) {
        if (client.player == null || client.getConnection() == null || client.gameMode == null) return "Нет подключения к миру";
        if (!client.player.isCreative()) return "Выдача доступна только в креативе";
        if (stack == null || stack.isEmpty()) return "Предмет не сформирован";
        var inventory = client.player.getInventory();
        int free = -1;
        for (int slot = 0; slot < 36; slot++) if (inventory.getItem(slot).isEmpty()) { free = slot; break; }
        if (free < 0) return "В инвентаре нет свободного места";
        ItemStack given = stack.copy();
        inventory.setItem(free, given);
        client.gameMode.handleCreativeModeItemAdd(given, free < 9 ? 36 + free : free);
        return "Выдано: " + given.getHoverName().getString();
    }

    public static net.minecraft.core.HolderLookup.Provider registryProvider() {
        var client = Minecraft.getInstance();
        return client == null || client.level == null ? net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY) : client.level.registryAccess();
    }
    public static net.minecraft.core.component.DataComponentPatch parseComponents(CustomItemPreset preset) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var reader = new com.mojang.brigadier.StringReader(preset.baseItem + "[" + preset.components.trim() + "]");
        var result = new net.minecraft.commands.arguments.item.ItemParser(registryProvider()).parse(reader);
        if (reader.canRead()) throw new IllegalArgumentException("Лишний текст после компонентов");
        return result.components();
    }
    public static void invalidate() { BUILT_CACHE.clear(); }
    public static boolean matchesPreset(ItemStack stack,String id){
        var preset=byId(id);if(preset==null)return false;
        try{return sameMacroItem(stack,cached(preset));}catch(RuntimeException invalid){return false;}
    }
    /** Compare the effective item data; normal wear and stack count do not detach a macro. */
    public static boolean sameMacroItem(ItemStack actual,ItemStack expected){
        if(actual==null||expected==null||actual.isEmpty()||expected.isEmpty()||actual.getItem()!=expected.getItem())return false;
        var a=actual.getComponents();var b=expected.getComponents();
        for(var type:a.keySet())if(type!=DataComponents.DAMAGE&&type!=DataComponents.REPAIR_COST
                &&!java.util.Objects.equals(a.get(type),b.get(type)))return false;
        for(var type:b.keySet())if(type!=DataComponents.DAMAGE&&type!=DataComponents.REPAIR_COST&&!a.has(type))return false;
        return true;
    }
    private static ItemStack cached(CustomItemPreset preset) {
        var client=Minecraft.getInstance();
        Object registry = client==null?null:client.level;
        if (registryIdentity != registry) { registryIdentity = registry; invalidate(); }
        String key = GSON.toJson(preset);
        if (BUILT_CACHE.size() > 512) invalidate();
        return BUILT_CACHE.computeIfAbsent(key, ignored -> build(preset, 1));
    }

    public static String nbt(ItemStack stack) {
        return ItemStack.CODEC.encodeStart(registryProvider().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
                .result().map(Object::toString).orElse(stack.getComponentsPatch().toString());
    }

    public static List<Component> describe(CustomItemPreset preset, ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        ItemLore actualLore = stack.get(DataComponents.LORE);
        if (actualLore != null) lines.addAll(actualLore.lines());
        lines.add(Component.literal("Автор: " + (preset.author == null || preset.author.isBlank() ? "не указан" : preset.author)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Сохранённый предмет: " + preset.id).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.literal(preset.baseItem + " · модель " + preset.model + " · количество " + stack.getCount()).withStyle(ChatFormatting.GRAY));
        ItemEnchantments actualEnchants = stack.get(DataComponents.ENCHANTMENTS);
        if (actualEnchants != null && !actualEnchants.isEmpty()) {
            for (var entry : actualEnchants.entrySet()) lines.add(Component.literal(entry.getKey().getRegisteredName() + " " + entry.getIntValue()).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else enchantments(preset.enchantments).forEach((id, level) -> lines.add(Component.literal(id + " " + level).withStyle(ChatFormatting.LIGHT_PURPLE)));
        lines.add(Component.literal("Неразрушимый: " + (stack.has(DataComponents.UNBREAKABLE) ? "да" : "нет")
                + " · блеск: " + (preset.glint ? "да" : "нет")).withStyle(ChatFormatting.GRAY));
        var attributes=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if(attributes!=null)for(var entry:attributes.modifiers())lines.add(Component.literal("Атрибут: "+entry.attribute().getRegisteredName()+" · "+entry.modifier().amount()+" · "+entry.modifier().operation()+" · "+entry.slot()).withStyle(ChatFormatting.AQUA));
        for(var b:EnvyWorld.config().binds)if(preset.id.equals(b.customItem)){
            lines.add(Component.literal("Макрос: "+b.id+" · автор: "+(b.author==null||b.author.isBlank()?"не указан":b.author)).withStyle(ChatFormatting.YELLOW));
            lines.add(Component.literal("Триггер: "+b.trigger.id()+" · "+(b.enabled?"включён":"выключен")+" · "+com.envyworld.client.MacroKeys.label(b.keyCode,b.keyModifiers)).withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("Команда: "+b.command).withStyle(ChatFormatting.GREEN));
            lines.add(Component.literal("%target% — ник цели · "+b.keyMode+" · повторов "+b.repeatCount+" · интервал "+b.repeatDelayMs+" мс").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("Радиус: "+b.radius+" · дальность взгляда: "+b.lookDistance+" · задержка: "+b.cooldownTicks+" тиков · точный предмет: "+b.customItem).withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.add(Component.literal("Компоненты / NBT (включая скрытые):").withStyle(ChatFormatting.GOLD));
        String raw = nbt(stack);
        for (int offset = 0; offset < raw.length(); offset += 85) lines.add(Component.literal(raw.substring(offset, Math.min(raw.length(), offset + 85))).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        CustomItemPreset preset = findPreset(stack);
        if (preset == null) return;
        List<Component> details = describe(preset, stack);
        if (lines.isEmpty()) lines.add(details.getFirst());
        lines.add(Component.empty());
        for (int i = 1; i < details.size(); i++) lines.add(details.get(i));
    }
    public static void normalTooltip(ItemStack stack,List<Component> lines){
        if(findPreset(stack)==null)return;
        lines.clear();lines.add(stack.getHoverName());
        var lore=stack.get(DataComponents.LORE);if(lore!=null)lines.addAll(lore.lines());
    }

    public static java.util.Map<String, Integer> enchantments(String value) {
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        if (value == null || value.isBlank()) return result;
        for (String raw : value.split("[,;]")) {
            String part = raw.trim();
            if (part.isEmpty()) continue;
            int colon = part.lastIndexOf(':');
            int level = 1;
            String id = part;
            if (colon > 0 && part.substring(colon + 1).matches("[0-9]+")) {
                level = Integer.parseInt(part.substring(colon + 1));
                id = part.substring(0, colon);
            }
            if (level < 1 || level > 255) throw new IllegalArgumentException("Уровень зачарования: 1–255");
            if (!id.contains(":")) id = "minecraft:" + id;
            if (Identifier.tryParse(id) == null) throw new IllegalArgumentException("Неверное зачарование: " + id);
            result.put(id, level);
        }
        return result;
    }

    private static void applyEnchantments(ItemStack stack, String value) {
        var entries = enchantments(value);
        if (entries.isEmpty() || Minecraft.getInstance().level == null) return;
        var lookup = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        entries.forEach((id, level) -> {
            var key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(id));
            var holder = lookup.get(key).orElseThrow(() -> new IllegalArgumentException("Не найдено зачарование: " + id));
            mutable.set(holder, level);
        });
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    public static CustomItemPreset findPreset(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (CustomItemPreset preset : EnvyWorld.config().customItems) {
            if (preset == null) continue;
            try { if (ItemStack.isSameItemSameComponents(stack, cached(preset))) return preset; }
            catch (RuntimeException invalidPreset) { /* A malformed local entry must not break inventory rendering. */ }
        }
        return null;
    }

	public static int modelOf(ItemStack stack) {
		CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		if (data == null || data.floats().isEmpty()) {
			return 0;
		}
		return Math.round(data.floats().get(0));
	}

	/** Reapplies the client-only item model after a server creative-slot resync. */
    public static void refreshInventoryModels(Minecraft minecraft) {
        // Never infer a saved item from its name/model or rewrite unrelated stacks.
        // ITEM_MODEL is part of the exact component set built by this catalog.
    }

	public static CustomItemPreset byId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		for (CustomItemPreset preset : EnvyWorld.config().customItems) {
			if (preset != null && id.equalsIgnoreCase(preset.id)) {
				return preset;
			}
		}
		return null;
	}

	public static String nextId() {
		int next = 1;
		for (CustomItemPreset preset : EnvyWorld.config().customItems) {
			if (preset == null || preset.id == null || !preset.id.startsWith("custom")) {
				continue;
			}
			try {
				next = Math.max(next, Integer.parseInt(preset.id.substring(6)) + 1);
			} catch (NumberFormatException ignored) {
				// Custom ids are allowed.
			}
		}
		return "custom" + next;
	}

	private static ChatFormatting colour(String value) {
		if (value != null) {
			try {
				return ChatFormatting.valueOf(value.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
				// Fall through.
			}
		}
		return ChatFormatting.LIGHT_PURPLE;
	}

	public static List<ChatFormatting> colours() {
		return List.of(ChatFormatting.WHITE, ChatFormatting.GRAY, ChatFormatting.GREEN, ChatFormatting.AQUA,
				ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD, ChatFormatting.RED, ChatFormatting.BLUE,
				ChatFormatting.YELLOW);
	}
}
