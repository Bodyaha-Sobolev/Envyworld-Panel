package com.envyworld.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;

/** Structured controls compile through Minecraft's own item-component parser. */
public final class ItemOptions {
    private ItemOptions() {}
    public static void apply(CustomItemPreset p, CustomItemPreset before, ItemStack stack) {
        List<String> patch = new ArrayList<>();
        if(changed(p.itemName,before==null?null:before.itemName) || before!=null && !p.itemNameFont.equals(before.itemNameFont)) {
            if(!p.itemName.isBlank()) stack.set(DataComponents.ITEM_NAME,ItemText.font(ItemText.parse(p.itemName,p.color),p.itemNameFont));
            else if(before!=null) stack.remove(DataComponents.ITEM_NAME);
        }
        if(!p.rarity.isBlank() && changed(p.rarity,before==null?null:before.rarity)) patch.add("minecraft:rarity="+quote(p.rarity));
        if(!p.tooltipStyle.isBlank() && changed(p.tooltipStyle,before==null?null:before.tooltipStyle)) patch.add("minecraft:tooltip_style="+quote(p.tooltipStyle));
        if(before==null || !new com.google.gson.Gson().toJson(p.attributes).equals(new com.google.gson.Gson().toJson(before.attributes))) {
            if(!p.attributes.isEmpty()) {
                List<String> entries=new ArrayList<>(); int index=0;
                for(var a:p.attributes) {
                    if(!Double.isFinite(a.amount)) throw new IllegalArgumentException("Значение атрибута должно быть конечным числом");
                    entries.add("{type:"+quote(a.type)+",amount:"+a.amount+",operation:"+quote(a.operation)+",slot:"+quote(a.slot)+",id:"+quote("envyworld:attribute_"+index++)+"}");
                }
                patch.add("minecraft:attribute_modifiers=["+String.join(",",entries)+"]");
            } else if(before!=null) patch.add("!minecraft:attribute_modifiers");
        }
        if(changed(p.canPlace,before==null?null:before.canPlace)) predicate(p.canPlace,"can_place_on",patch);
        if(changed(p.canBreak,before==null?null:before.canBreak)) predicate(p.canBreak,"can_break",patch);
        if(p.foodEnabled && (before==null || !before.foodEnabled || p.nutrition!=before.nutrition || p.saturation!=before.saturation || p.alwaysEdible!=before.alwaysEdible)) {
            if(p.nutrition<0 || p.saturation<0 || !Float.isFinite(p.saturation)) throw new IllegalArgumentException("Питательность и насыщение должны быть положительными");
            patch.add("minecraft:food={nutrition:"+p.nutrition+",saturation:"+p.saturation+",can_always_eat:"+p.alwaysEdible+"}");
            patch.add("minecraft:consumable={}");
        } else if(!p.foodEnabled && before!=null && before.foodEnabled) { patch.add("!minecraft:food"); patch.add("!minecraft:consumable"); }
        if(p.toolEnabled && (before==null || !before.toolEnabled || !p.toolBlocks.equals(before.toolBlocks) || p.miningSpeed!=before.miningSpeed || p.damagePerBlock!=before.damagePerBlock)) {
            if(p.miningSpeed<=0 || !Float.isFinite(p.miningSpeed) || p.damagePerBlock<0) throw new IllegalArgumentException("Проверьте скорость и расход прочности");
            String blocks=blocks(p.toolBlocks);
            patch.add("minecraft:tool={rules:[{blocks:"+blocks+",speed:"+p.miningSpeed+",correct_for_drops:true}],default_mining_speed:1.0,damage_per_block:"+p.damagePerBlock+"}");
        } else if(!p.toolEnabled && before!=null && before.toolEnabled) patch.add("!minecraft:tool");
        if(!patch.isEmpty()) {
            var temp=new CustomItemPreset(); temp.baseItem=p.baseItem; temp.components=String.join(",",patch);
            try { stack.applyComponentsAndValidate(CustomItems.parseComponents(temp)); }
            catch(Exception error) { throw new IllegalArgumentException("Параметры предмета: "+error.getMessage(),error); }
        }
        if(before==null || p.hideTooltip!=before.hideTooltip || p.hideEnchantments!=before.hideEnchantments || p.hideAttributes!=before.hideAttributes || p.hideCanPlace!=before.hideCanPlace || p.hideCanBreak!=before.hideCanBreak) {
            var original=stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY,TooltipDisplay.DEFAULT);
            var display=new TooltipDisplay(p.hideTooltip,original.hiddenComponents()).withHidden(DataComponents.ENCHANTMENTS,p.hideEnchantments)
                    .withHidden(DataComponents.ATTRIBUTE_MODIFIERS,p.hideAttributes).withHidden(DataComponents.CAN_PLACE_ON,p.hideCanPlace).withHidden(DataComponents.CAN_BREAK,p.hideCanBreak);
            if(!display.equals(TooltipDisplay.DEFAULT) || stack.has(DataComponents.TOOLTIP_DISPLAY)) stack.set(DataComponents.TOOLTIP_DISPLAY,display);
        }
    }
    private static boolean changed(Object a,Object b) { return !Objects.equals(a,b); }
    private static void predicate(String value,String key,List<String> patch) {
        if(value.isBlank()) { patch.add("!minecraft:"+key); return; }
        patch.add("minecraft:"+key+"={blocks:"+blocks(value)+"}");
    }
    private static String blocks(String value) {
        var names=java.util.Arrays.stream(value.split(",")).map(String::trim).filter(s->!s.isBlank()).toList();
        if(names.isEmpty()) throw new IllegalArgumentException("Выберите хотя бы один блок");
        if(names.size()==1 && names.getFirst().startsWith("#")) return quote(names.getFirst());
        return "["+names.stream().map(ItemOptions::quote).collect(java.util.stream.Collectors.joining(","))+"]";
    }
    private static String quote(String value) { return new com.google.gson.Gson().toJson(value); }
}
