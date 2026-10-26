package com.envyworld.items;

import com.envyworld.EnvyWorld;
import com.envyworld.bind.Binding;
import com.envyworld.client.ClientMacros;
import com.envyworld.config.RulesConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/** Offline regression checks: no inventory, network, commands or persistent user config. */
public final class CatalogItemSmoke {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var configField=EnvyWorld.class.getDeclaredField("config");configField.setAccessible(true);
        Object originalConfig=configField.get(null);
        try {
            var config=new RulesConfig();configField.set(null,config);
            var preset=new CustomItemPreset();preset.id="catalog-smoke";preset.baseItem="minecraft:diamond_sword";
            preset.name="Проверочный меч";preset.color="#ABCDEF";
            var original=CustomItems.build(preset,1);
            var used=original.copy();used.set(DataComponents.DAMAGE,73);used.set(DataComponents.REPAIR_COST,6);
            check(CustomItems.sameMacroItem(used,original),"Attacking and anvil repair cost must not disable a catalog macro");
            check(used.getDamageValue()==73,"Matching must not mutate the held stack");
            var renamed=used.copy();renamed.set(DataComponents.CUSTOM_NAME,Component.literal("Чужой меч"));
            check(!CustomItems.sameMacroItem(renamed,original),"A different custom name must not match");
            var relored=used.copy();relored.set(DataComponents.LORE,new ItemLore(java.util.List.of(Component.literal("Чужое описание"))));
            check(!CustomItems.sameMacroItem(relored,original),"Different lore must not match");
            check(!CustomItems.sameMacroItem(new ItemStack(Items.IRON_SWORD),original),"A different base item must not match");
            var changedGlint=used.copy();changedGlint.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,!preset.glint);
            check(!CustomItems.sameMacroItem(changedGlint,original),"Meaningful components must remain exact");
            var stackable=new ItemStack(Items.STICK,1);var many=stackable.copyWithCount(32);
            check(CustomItems.sameMacroItem(stackable,many),"Stack count must not change item identity");

            config.customItems.add(preset);
            var binding=new Binding();binding.item="minecraft:stick";binding.customItem=preset.id;
            var matches=ClientMacros.class.getDeclaredMethod("matchesItem",Binding.class,ItemStack.class,String.class);matches.setAccessible(true);
            check((Boolean)matches.invoke(null,binding,used,preset.baseItem),"A catalog reference must follow a changed base item, not its stale copied id");
            binding.customItem="";
            check(!(Boolean)matches.invoke(null,binding,used,preset.baseItem),"An ordinary item filter must keep its configured base id");

            var patchPreset=new CustomItemPreset();patchPreset.name="Изменённое название";
            patchPreset.components="minecraft:custom_name={text:\"Мой предмет\"}";
            check(CustomItems.build(patchPreset,1).getHoverName().getString().equals("Изменённое название"),"The edited name must win over an old raw custom_name");
            var imported=CustomItems.fromStack(original,"Smoke");
            check(ItemStack.isSameItemSameComponents(CustomItems.build(imported,1),original),"Unedited imports must keep their original component set");
            imported.name="Новое имя импорта";
            check(CustomItems.build(imported,1).getHoverName().getString().equals("Новое имя импорта"),"An edited imported name must survive its preserved raw stack");
            System.out.println("PASS: catalog macros survive wear/base edits, reject different names/components, preserve imports and give the edited name");
        } finally {configField.set(null,originalConfig);CustomItems.invalidate();}
    }
}
