package com.envyworld.client.gui;

import com.envyworld.graphics.misc.FontRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** StyledActionButton drawing from Envyworld, bound to Envyworld's existing callbacks. */
public final class UiButton extends Button {
    private final UiMotion hover=new UiMotion(0);
    private boolean selected;
    public void selected(boolean value){selected=value;}

    private UiButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, message, onPress, narration);
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        var palette = MenuTheme.PALETTE;
        float hoverProgress=hover.update(isHoveredOrFocused()&&active?1:0);
        int fill = blend(selected?0xA0181818:palette.backgroundControl(), palette.backgroundHover(), hoverProgress);
        if(net.minecraft.client.Minecraft.getInstance().screen instanceof HudEditorScreen){
            HudSurface.draw(getX(),getY(),getWidth(),getHeight(),6,Math.clamp(com.envyworld.EnvyWorld.config().client.hudOpacity,120,255));
            UiGraphics.rounded(getX(),getY(),getWidth(),getHeight(),6,Math.round(14*hoverProgress)<<24|0xFFFFFF);
        }else UiGraphics.rounded(getX(), getY(), getWidth(), getHeight(), 6, fill);
        UiGraphics.outline(getX(), getY(), getWidth(), getHeight(), 6, blend(selected?palette.borderStrong():palette.borderPrimary(), palette.borderStrong(), hoverProgress));
        String raw = getMessage().getString();
        if(raw.startsWith("format:")){UiIcons.format(raw.substring(7),getX()+getWidth()/2f,getY()+getHeight()/2f,active?palette.textPrimary():palette.textDisabled());return;}

        if(raw.matches("Цвет #[0-9a-fA-F]{6}")){
            int swatch=0xFF000000|Integer.parseInt(raw.substring(6),16);
            float side=7,swatchX=getRight()-16,swatchY=getY()+(getHeight()-side)/2f;
            UiGraphics.rounded(swatchX,swatchY,side,side,2.5f,swatch);
            UiGraphics.outline(swatchX,swatchY,side,side,2.5f,blend(0x18FFFFFF,0x55FFFFFF,hoverProgress));
            UiFont.draw(graphics,"Цвет",getX()+8,UiFont.centeredY(getY(),getHeight(),6.5f),6.5f,active?palette.textPrimary():palette.textDisabled());
            return;
        }
        boolean chevron = raw.endsWith("▾") || raw.endsWith(" v") || raw.endsWith(" ^") || raw.endsWith("▼");
        boolean check = raw.startsWith("✔") || raw.startsWith("✓");
        boolean plus = raw.startsWith("+");
        boolean minus = raw.startsWith("−") || raw.equals("-");
        String icon = switch(raw){case "Telegram","Telegram и привязка"->"telegram";case "Импорт"->"download";case "Экспорт"->"upload";case "Удалить","Стереть"->"trash";default->"";};
        String text = UiFont.sanitize(chevron ? raw.substring(0, raw.length() - 1).trim() : check || plus || minus ? raw.substring(1).trim() : raw);
        float fontSize = 6.5f;
        float width = UiFont.measure(text, fontSize);
        float textX = getX() + (getWidth() - width) / 2;
        int color = active ? palette.textPrimary() : palette.textDisabled();
        if (chevron) { textX -= 4; UiIcons.chevron(getRight() - 10, getY() + getHeight() / 2f, raw.endsWith(" ^"), color); }
        if (check) UiIcons.check(textX - 8, getY() + getHeight() / 2f, color);
        if (!icon.isEmpty()) {textX+=5;UiIcons.draw(icon,textX-9,getY()+getHeight()/2f,11,color);}
        if (minus) UiIcons.minus(text.isBlank()?getX()+getWidth()/2f:textX-8,getY()+getHeight()/2f,color);
        if (plus) UiIcons.plus(textX - 8, getY() + getHeight() / 2f, color);
        graphics.enableScissor(getX() + 3, getY(), getRight() - 3, getBottom());
        UiFont.draw(graphics, text, textX,
                UiFont.centeredY(getY(), getHeight(), fontSize), fontSize, color);
        graphics.disableScissor();
    }

    public static Button.Builder builder(Component message, OnPress onPress) {
        return new Button.Builder(message, onPress) {
            private int x, y;
            private int width = DEFAULT_WIDTH, height = DEFAULT_HEIGHT;
            private Tooltip tooltip;
            private CreateNarration narration = DEFAULT_NARRATION;
            @Override public Button.Builder pos(int x, int y) { this.x = x; this.y = y; return this; }
            @Override public Button.Builder width(int width) { this.width = width; return this; }
            @Override public Button.Builder size(int width, int height) { this.width = width; this.height = height; return this; }
            @Override public Button.Builder bounds(int x, int y, int width, int height) { return pos(x, y).size(width, height); }
            @Override public Button.Builder tooltip(Tooltip tooltip) { this.tooltip = tooltip; return this; }
            @Override public Button.Builder createNarration(CreateNarration narration) { this.narration = narration; return this; }
            @Override public Button build() {
                Button button = new UiButton(x, y, width, height, message, onPress, narration);
                button.setTooltip(tooltip);
                return button;
            }
        };
    }

    private static int blend(int a, int b, float amount) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int x = a >>> shift & 255, y = b >>> shift & 255;
            result |= Math.round(x + (y - x) * amount) << shift;
        }
        return result;
    }
}
