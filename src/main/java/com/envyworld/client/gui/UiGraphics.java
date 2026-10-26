package com.envyworld.client.gui;

import com.envyworld.graphics.misc.FontRegistry;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;

/** Adapts existing screen layout to Envyworld drawing; Minecraft only bakes item/skin images. */
public final class UiGraphics extends GuiGraphics {
    private final GuiGraphics original;
    private static int tooltipKey, tooltipOffset, tooltipMaximum;
    private static long tooltipAt;

    public static boolean scrollTooltip(double amount) {
        long window = Minecraft.getInstance().getWindow().handle();
        boolean shift = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == 1
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == 1;
        if (!shift || tooltipMaximum <= 0 || System.nanoTime() - tooltipAt > 300_000_000L) return false;
        tooltipOffset = Math.max(0, Math.min(tooltipMaximum, tooltipOffset - (int) Math.round(amount * 3))); return true;
    }

    public UiGraphics(GuiGraphics original, int mouseX, int mouseY) {
        super(Minecraft.getInstance(), original.guiRenderState, mouseX, mouseY);
        this.original = original;
    }

    @Override
    public void requestCursor(CursorType cursor) { original.requestCursor(cursor); }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int color) {
        float x = Math.min(x0, x1), y = Math.min(y0, y1);
        float w = Math.abs(x1 - x0), h = Math.abs(y1 - y0);
        float radius = x <= 0 && w >= guiWidth() ? 0 : w >= 16 && h >= 12 ? 9f : 0;
        rounded(x, y, w, h, radius, color);
    }

    public static void rounded(float x, float y, float w, float h, float radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        UiRenderer.submit((draw, matrix) -> draw.drawRoundedRectangle(matrix, x, y, w, h, radius, color));
    }

    @Override
    public void renderOutline(int x, int y, int width, int height, int color) {
        outline(x, y, width, height, width >= 16 && height >= 12 ? 9f : 0, color);
    }

    public static void outline(float x, float y, float w, float h, float radius, int color) {
        UiRenderer.submit((draw, matrix) -> draw.drawRoundedOutline(matrix, x, y, w, h, radius, .75f, color));
    }

    @Override
    public void enableScissor(int x0, int y0, int x1, int y1) {
        super.enableScissor(x0, y0, x1, y1);
        // Use the intersection computed by Minecraft for native item icons as well.
        var rect = scissorStack.peek();
        UiRenderer.submit((draw, matrix) -> draw.pushScreenScissor(
                rect.left() * matrix.m00(), rect.top() * matrix.m11(),
                rect.width() * matrix.m00(), rect.height() * matrix.m11()));
    }

    @Override
    public void disableScissor() {
        super.disableScissor();
        UiRenderer.submit((draw, matrix) -> draw.popScissor());
    }

    @Override
    public void setTooltipForNextFrame(Font font, Component text, int x, int y) {
        tooltip(List.of(text), x, y);
    }

    @Override
    public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> image, int x, int y) {
        tooltip(lines, x, y);
    }

    @Override
    public void setTooltipForNextFrame(Font font, List<FormattedCharSequence> lines, ClientTooltipPositioner positioner, int x, int y, boolean override) {
        tooltip(lines.stream().map(line -> {
            StringBuilder text = new StringBuilder();
            line.accept((index, style, codepoint) -> { text.appendCodePoint(codepoint); return true; });
            return (Component) Component.literal(text.toString());
        }).toList(), x, y);
    }

    private void tooltip(List<Component> components, int mouseX, int mouseY) {
        float maxWidth = Math.max(80, Math.min(500, guiWidth() - 24));
        List<Component> wrapped = components.stream().flatMap(text -> UiText.wrap(text, maxWidth, UiFont.SIZE).stream()).toList();
        int maxLines = Math.max(1, (guiHeight() - 24) / (UiFont.lineHeight() + 2));
        int key = components.hashCode();
        if (key != tooltipKey) { tooltipKey = key; tooltipOffset = 0; }
        int available = Math.max(1, maxLines - (wrapped.size() > maxLines ? 1 : 0));
        tooltipMaximum = Math.max(0, wrapped.size() - available); tooltipAt = System.nanoTime();
        tooltipOffset = Math.min(tooltipOffset, tooltipMaximum);
        List<Component> lines = new java.util.ArrayList<>();
        if (tooltipMaximum > 0) lines.add(Component.literal("Shift + колесо  |  строки " + (tooltipOffset + 1) + " - " + Math.min(wrapped.size(), tooltipOffset + available) + " / " + wrapped.size()).withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.addAll(wrapped.subList(tooltipOffset, Math.min(wrapped.size(), tooltipOffset + available)));
        float w = 12;
        for (Component line : lines) w = Math.max(w, UiText.measure(line, UiFont.SIZE) + 12);
        float h = lines.size() * (UiFont.lineHeight() + 2) + 10;
        float x = Math.max(2, Math.min(mouseX + 10, guiWidth() - w - 3));
        float y = Math.max(2, Math.min(mouseY + 10, guiHeight() - h - 3));
        float width = w;
        if(lines.stream().anyMatch(UiText::nativeNeeded)){
            UiRenderer.submit((draw,matrix)->{
                draw.drawBlurredRoundedRectangle(matrix,x,y,width,h,5);
                draw.drawRoundedRectangle(matrix,x,y,width,h,5,MenuTheme.PALETTE.backgroundPrimary());
                draw.drawRoundedOutline(matrix,x,y,width,h,5,.75f,MenuTheme.PALETTE.borderPrimary());
            });
            for(int i=0;i<lines.size();i++)UiText.component(original,lines.get(i),x+6,y+5+i*(UiFont.lineHeight()+2),width-12,UiFont.SIZE,MenuTheme.PALETTE.textPrimary());
            return;
        }
        UiRenderer.overlay((draw, matrix) -> {
            draw.drawBlurredRoundedRectangle(matrix, x, y, width, h, 5);
            draw.drawRoundedRectangle(matrix, x, y, width, h, 5, MenuTheme.PALETTE.backgroundPrimary());
            draw.drawRoundedOutline(matrix, x, y, width, h, 5, .75f, MenuTheme.PALETTE.borderPrimary());
            for (int i = 0; i < lines.size(); i++) {
                UiText.overlayComponent(draw, matrix, lines.get(i), x + 6,
                        y + 5 + i * (UiFont.lineHeight() + 2), UiFont.SIZE, MenuTheme.PALETTE.textPrimary());
            }
        });
    }
}
