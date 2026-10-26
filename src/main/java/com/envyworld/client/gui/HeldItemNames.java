package com.envyworld.client.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Keep readable Russian names; replace decorative/unfamiliar script with the real item type. */
public final class HeldItemNames {
    private static ClientLanguage russian;
    private static Object resources;
    private HeldItemNames(){}
    public static boolean readable(String value){
        return !value.isBlank()&&value.length()<=56&&value.codePoints().allMatch(cp->cp==' '||cp>='0'&&cp<='9'||cp>=0x410&&cp<=0x44F||cp==0x401||cp==0x451||"-.,:!?()".indexOf(cp)>=0);
    }
    public static String label(ItemStack stack){
        String custom=stack.getHoverName().getString();if(readable(custom))return custom;
        var manager=Minecraft.getInstance().getResourceManager();if(resources!=manager||russian==null){resources=manager;russian=ClientLanguage.loadFrom(manager,List.of("ru_ru"),false);}
        String base=russian.getOrDefault(stack.getItem().getDescriptionId(),"");
        return base.isBlank()?stack.getItem().getName().getString():base;
    }
}
