package com.envyworld.client.gui;

import com.envyworld.mixin.EditBoxAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** Envyworld field visuals and MSDF caret measurements, with Minecraft's existing edit events. */
public final class UiEditBox extends EditBox {
    private int firstVisible;

    public UiEditBox(Font font, int x, int y, int width, int height, Component label) {
        super(font, x, y, width, height, label);
        setHint(label);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;
        var palette = MenuTheme.PALETTE;
        UiGraphics.rounded(getX(), getY(), getWidth(), getHeight(), 6, palette.controlFill());
        UiGraphics.outline(getX(), getY(), getWidth(), getHeight(), 6,
                isFocused() ? palette.borderStrong() : palette.borderPrimary());
        String value = getValue();
        int cursor = getCursorPosition();
        firstVisible = Math.min(firstVisible, Math.min(cursor, value.length()));
        int innerWidth = Math.max(1, getWidth() - 8);
        while (firstVisible < cursor && UiFont.measure(value.substring(firstVisible, cursor), UiFont.SIZE) > innerWidth - 2) {
            firstVisible += Character.charCount(value.codePointAt(firstVisible));
        }
        EditBoxAccessor state = (EditBoxAccessor) (Object) this;
        float x = getX() + 4;
        float y = UiFont.centeredY(getY(), getHeight(), UiFont.SIZE);
        graphics.enableScissor(getX() + 3, getY() + 1, getRight() - 3, getBottom() - 1);
        int selected = state.envyworld$highlightPos();
        if (selected != cursor) {
            int start = Math.max(firstVisible, Math.min(cursor, selected));
            int end = Math.max(firstVisible, Math.max(cursor, selected));
            float left = x + UiFont.measure(value.substring(firstVisible, start), UiFont.SIZE);
            float right = x + UiFont.measure(value.substring(firstVisible, end), UiFont.SIZE);
            UiGraphics.rounded(left, y, right - left, UiFont.lineHeight(), 1, palette.accentTint());
        }
        int color = state.envyworld$isEditable() ? state.envyworld$textColor() : state.envyworld$textColorUneditable();
        UiFont.draw(graphics, value.substring(firstVisible), x, y, color);
        if (value.isEmpty() && state.envyworld$hint() != null) {
            UiFont.draw(graphics, state.envyworld$hint().getString(), x, y, palette.textPlaceholder());
        }
        if (isFocused() && (Util.getMillis() - state.envyworld$focusedTime()) / 300 % 2 == 0) {
            float caretX = x + UiFont.measure(value.substring(firstVisible, cursor), UiFont.SIZE);
            UiGraphics.rounded(caretX, y, .6f, UiFont.lineHeight(), 0, palette.textSecondary());
        }
        graphics.disableScissor();
    }

    private int clickedPosition(double mouseX) {
        String value = getValue();
        int start = Math.min(firstVisible, value.length());
        float target = (float) mouseX - getX() - 4;
        for (int i = start; i < value.length();) {
            int next = i + Character.charCount(value.codePointAt(i));
            float left = UiFont.measure(value.substring(start, i), UiFont.SIZE);
            float right = UiFont.measure(value.substring(start, next), UiFont.SIZE);
            if (target < (left + right) * .5f) return i;
            i = next;
        }
        return value.length();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        int position = clickedPosition(event.x());
        if (doubled) {
            String value = getValue();
            int left = position, right = position;
            while (left > 0 && !Character.isWhitespace(value.charAt(left - 1))) left--;
            while (right < value.length() && !Character.isWhitespace(value.charAt(right))) right++;
            moveCursorTo(left, false);
            moveCursorTo(right, true);
        } else moveCursorTo(position, event.hasShiftDown());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        moveCursorTo(clickedPosition(event.x()), true);
    }

    @Override
    public int getScreenX(int position) {
        return getX() + 4 + UiFont.width(getValue().substring(0, Math.min(position, getValue().length())));
    }
}
