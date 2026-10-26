package com.envyworld.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditBox.class)
public interface EditBoxAccessor {
    @Accessor("highlightPos") int envyworld$highlightPos();
    @Accessor("hint") Component envyworld$hint();
    @Accessor("textColor") int envyworld$textColor();
    @Accessor("textColorUneditable") int envyworld$textColorUneditable();
    @Accessor("isEditable") boolean envyworld$isEditable();
    @Accessor("focusedTime") long envyworld$focusedTime();
}
