package com.envyworld.client.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Accessible full-row switch with a restrained, time-based thumb transition. */
public final class UiToggle extends Button {
    private final String title, detail;
    private final BooleanSupplier checked;
    private final UiMotion state, hover = new UiMotion(0);
    private int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;
    private boolean narratedState;

    public UiToggle(int x, int y, int width, int height, String title, String detail,
                     BooleanSupplier checked, Runnable action) {
        super(x, y, width, height, Component.literal(title), ignored -> action.run(), DEFAULT_NARRATION);
        this.title = title;
        this.detail = detail;
        this.checked = checked;
        narratedState = checked.getAsBoolean();
        state = new UiMotion(narratedState ? 1 : 0);
        setMessage(Component.literal(title + (narratedState ? ": включено" : ": выключено")));
    }

    public void clip(int top, int bottom) { clipTop = top; clipBottom = bottom; }

    @Override public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseY >= clipTop && mouseY < clipBottom && super.isMouseOver(mouseX, mouseY);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return event.y() >= clipTop && event.y() < clipBottom && super.mouseClicked(event, doubled);
    }

    @Override protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean enabled = checked.getAsBoolean();
        if (enabled != narratedState) {
            narratedState = enabled;
            setMessage(Component.literal(title + (enabled ? ": включено" : ": выключено")));
        }
        float progress = state.update(enabled ? 1 : 0);
        float over = hover.update(active && (isMouseOver(mouseX, mouseY) || isFocused()) ? 1 : 0);
        int top = Math.max(clipTop, getY()), bottom = Math.min(clipBottom, getBottom());
        if (bottom <= top) return;
        graphics.enableScissor(getX(), top, getRight(), bottom);
        var palette = MenuTheme.PALETTE;
        UiGraphics.rounded(getX(), getY() + 1, getWidth(), getHeight() - 2, 5, Math.round(11 * over) << 24 | 0xFFFFFF);
        float trackX = getRight() - 33, trackY = getY() + (getHeight() - 13) / 2f;
        int trackColor = mix(0xFF292A30, 0xFFE7E8EB, progress);
        int knobColor = mix(0xFFA1A2A8, 0xFF232429, progress);
        if (!active) { trackColor = mix(trackColor, 0xFF17181B, .55f); knobColor = mix(knobColor, 0xFF53545B, .5f); }
        UiGraphics.rounded(trackX, trackY, 25, 13, 6.5f, trackColor);
        UiGraphics.rounded(trackX + 2 + 12 * progress, trackY + 2, 9, 9, 4.5f, knobColor);
        if (isFocused()) UiGraphics.outline(trackX - 2, trackY - 2, 29, 17, 8.5f, 0x66FFFFFF);
        text(graphics, title, getX() + 8, getY() + 7, getWidth() - 49, 6.5f,
                active ? palette.textPrimary() : palette.textDisabled());
        text(graphics, detail, getX() + 8, getY() + 19, getWidth() - 49, 5.5f,
                active ? palette.textMuted() : palette.textDisabled());
        graphics.disableScissor();
    }

    static void text(GuiGraphics graphics, String value, float x, float y, float width, float size, int color) {
        if (width <= 0) return;
        if (UiFont.measure(value, size) <= width) { UiFont.draw(graphics, value, x, y, size, color); return; }
        int end = value.length();
        while (end > 0 && UiFont.measure(value.substring(0, end) + "…", size) > width)
            end = value.offsetByCodePoints(end, -1);
        if (end > 0) UiFont.draw(graphics, value.substring(0, end) + "…", x, y, size, color);
    }

    private static int mix(int a, int b, float t) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int channel = a >>> shift & 255;
            result |= Math.round(channel + ((b >>> shift & 255) - channel) * t) << shift;
        }
        return result;
    }
}
