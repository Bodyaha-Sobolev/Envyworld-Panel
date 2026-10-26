package com.envyworld.items;

/**
 * A locally saved custom item. Everything is stored in
 * {@code config/envyworld/config.json}; no server-side data is needed.
 */
public class CustomItemPreset {
	public String id = "";
	public String name = "Мой предмет";
	public String baseItem = "minecraft:stick";
	/** Lore lines separated by {@code |}. */
	public String lore = "";
	/** Enchantments in the form {@code sharpness:5, unbreaking:3}. */
	public String enchantments = "";
	/** 0 = vanilla model, 1..9 = bundled EnvyWorld textures. */
	public int model = 0;
	public int count = 1;
	/** ChatFormatting name for the item name. */
	public String color = "LIGHT_PURPLE";
	public boolean unbreakable = false;
	public boolean glint = true;
    public String author = "";
    public long createdAt;
    /** Item component patch, e.g. minecraft:custom_data={example:1b}. */
    public String components = "";
    public String rawStack = "", importedState = "";
    public String itemName = "", rarity = "", tooltipStyle = "";
    public String nameFont = "minecraft:default", loreFont = "minecraft:default", itemNameFont = "minecraft:default";
    public boolean hideTooltip, hideEnchantments, hideAttributes, hideCanPlace, hideCanBreak;
    public java.util.List<AttributeSpec> attributes = new java.util.ArrayList<>();
    public String canPlace = "", canBreak = "", toolBlocks = "";
    public boolean foodEnabled, alwaysEdible, toolEnabled;
    public int nutrition = 4, damagePerBlock = 1;
    public float saturation = .3f, miningSpeed = 6;
    public static class AttributeSpec {
        public String type = "minecraft:armor", slot = "any", operation = "add_value";
        public double amount;
    }

	public CustomItemPreset copy() {
		CustomItemPreset copy = new CustomItemPreset();
		copy.id = id;
		copy.name = name;
		copy.baseItem = baseItem;
		copy.lore = lore;
		copy.enchantments = enchantments;
		copy.model = model;
		copy.count = count;
		copy.color = color;
		copy.unbreakable = unbreakable;
		copy.glint = glint;
        copy.author = author;
        copy.createdAt = createdAt;
        copy.components = components;
        copy.rawStack = rawStack; copy.importedState = importedState;
        copy.itemName = itemName; copy.rarity = rarity; copy.tooltipStyle = tooltipStyle;
        copy.nameFont=nameFont;copy.loreFont=loreFont;copy.itemNameFont=itemNameFont;
        copy.hideTooltip = hideTooltip; copy.hideEnchantments = hideEnchantments; copy.hideAttributes = hideAttributes;
        copy.hideCanPlace = hideCanPlace; copy.hideCanBreak = hideCanBreak;
        copy.attributes = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(attributes), new com.google.gson.reflect.TypeToken<java.util.ArrayList<AttributeSpec>>(){}.getType());
        copy.canPlace = canPlace; copy.canBreak = canBreak; copy.toolBlocks = toolBlocks;
        copy.foodEnabled = foodEnabled; copy.alwaysEdible = alwaysEdible; copy.toolEnabled = toolEnabled;
        copy.nutrition = nutrition; copy.damagePerBlock = damagePerBlock; copy.saturation = saturation; copy.miningSpeed = miningSpeed;
		return copy;
	}
}
