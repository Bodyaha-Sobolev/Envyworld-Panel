package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;
import com.envyworld.client.ClientMacros;
import com.envyworld.config.ClientSettings.HudBox;
import com.envyworld.logs.OnlineWatch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/** The same Envyworld shapes/fonts as the menu, rendered into small configurable HUD cards. */
public final class EnvyHud {
    public record Line(String text, int color) {}
    public record Box(String id, HudBox settings, float x, float y, float width, float height, String title, List<Line> lines) {
        public boolean contains(double px, double py) { return px >= x && px <= x + width && py >= y && py <= y + height; }
    }
    private static boolean frame;
    private static net.minecraft.client.gui.GuiGraphics frameGraphics;
    public static void captureGraphics(net.minecraft.client.gui.GuiGraphics graphics){frameGraphics=graphics;}
    private EnvyHud() {}
    public static List<Box> boxes(int width, int height, boolean preview) {
        var settings = EnvyWorld.config().client;
        List<Box> boxes = new ArrayList<>();
        var active = ClientMacros.activeNames();
        if (preview || !active.isEmpty()) {
            List<Line> lines = new ArrayList<>();
            for (String name : active) lines.add(new Line(name + "  |  выполняется", 0xFFE6E6E6));
            if (lines.isEmpty()) lines.add(new Line("Нет активных макросов", 0xFF969699));
            boxes.add(box("macros", settings.macros, "Макросы", lines, width, height));
        }
        if (preview || !settings.watchedPlayers.isEmpty()) {
            List<Line> lines = new ArrayList<>();
            for (String name : settings.watchedPlayers.stream().limit(12).toList()) {
                var state = OnlineWatch.presence(name);
                lines.add(new Line(name + "  |  " + (!state.known() ? "проверяем" : state.online() ? "в сети" : "не в сети"),
                        !state.known() ? 0xFF9AA8BA : state.online() ? 0xFF87D9B0 : 0xFFB9A4B1));
            }
            if (lines.isEmpty()) lines.add(new Line("Добавьте игроков во вкладке Онлайн", 0xFF969699));
            boxes.add(box("online", settings.online, "Отслеживаемые игроки", lines, width, height));
        }
        List<HudNotices.Notice> notices = HudNotices.current();
        if (preview || !notices.isEmpty()) {
            List<Line> lines = new ArrayList<>();
            for (var notice : notices.stream().limit(3).toList()) {
                lines.add(new Line(notice.title(), notice.color()));
                for (String line : UiFont.wrap(notice.body(), 200, 6).stream().limit(2).toList()) lines.add(new Line(line, 0xFFB6C2D0));
            }
            if (lines.isEmpty()) { lines.add(new Line("Предпросмотр уведомления", 0xFFE6E6E6)); lines.add(new Line("Новые наказания и изменения онлайна", 0xFF969699)); }
            boxes.add(box("notices", settings.notices, "Уведомления", lines, width, height));
        }
        return boxes;
    }
    private static Box box(String id, HudBox settings, String title, List<Line> lines, int width, int height) {
        float scale = Math.max(.65f, Math.min(1.7f, settings.scale));
        int maxLines = Math.max(1, (int) ((height - 52) / scale - 26) / 12);
        if (lines.size() > maxLines) {
            int hidden = lines.size() - maxLines + 1;
            lines = new ArrayList<>(lines.subList(0, maxLines));
            lines.set(maxLines - 1, new Line("Ещё " + hidden + "  |  откройте меню", 0xFF969699));
        }
        float measured=UiFont.measure(title,7)+32;
        for(var line:lines)measured=Math.max(measured,UiFont.measure(line.text(),6)+20);
        float w = Math.min(width - 12, Math.max(110,Math.min(250,measured))*scale), h = (26 + lines.size() * 12) * scale;
        float x = Math.max(6, Math.min(width - w - 6, settings.x * width));
        float y = Math.max(6, Math.min(height - h - 6, settings.y * height));
        if(settings.anchorX>=0)x=settings.anchorX==0?6:settings.anchorX==1?(width-w)/2:width-w-6;
        if(settings.anchorY>=0)y=settings.anchorY==0?6:settings.anchorY==1?(height-h)/2:height-h-6;
        return new Box(id, settings, x, y, w, h, title, lines);
    }
    public static void draw(int width, int height, boolean preview, String selected) {
        draw(frameGraphics,width,height,preview,selected);
    }
    public static void draw(net.minecraft.client.gui.GuiGraphics graphics,int width,int height,boolean preview,String selected){
        for (Box box : boxes(width, height, preview)) {
            if (!box.settings().visible && !preview) continue;
            drawBox(graphics,box, preview, selected);
        }
    }
    private static void drawBox(net.minecraft.client.gui.GuiGraphics graphics,Box box, boolean preview, String selected) {
            float s = Math.max(.65f, Math.min(1.7f, box.settings().scale));
            int alpha = box.settings().visible ? 255 : 100;
            if (box.settings().background || preview) {
                var style=EnvyWorld.config().client;
                int opacity=box.settings().visible?Math.clamp(style.hudOpacity,120,255):100;
                HudSurface.draw(box.x(),box.y(),box.width(),box.height(),10,opacity);
                UiGraphics.outline(box.x(), box.y(), box.width(), box.height(), 10, box.id().equals(selected) ? 0x99FFFFFF : 0x28FFFFFF);
            }
            UiFont.draw(graphics, box.title(), box.x() + 10 * s, box.y() + 8 * s, 7 * s, alpha << 24 | 0xEEEEEE);
            float y = box.y() + 24 * s;
            for (Line line : box.lines()) {
                String text = line.text();
                while (UiFont.measure(text, 6 * s) > box.width() - 20 * s && !text.isEmpty()) text = text.substring(0, text.length() - 1);
                UiFont.draw(graphics, text, box.x() + 10 * s, y, 6 * s, alpha << 24 | line.color() & 0xFFFFFF);
                y += 12 * s;
            }
    }
    public static void noticesOverlay() {
        var client = Minecraft.getInstance();
        if (!EnvyWorld.config().client.hudEnabled || client.screen instanceof MenuScreen) return;
        int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
        UiRenderer.overlayCommands(() -> {
            for (var box : boxes(width, height, false)) if (box.id().equals("notices") && box.settings().visible) drawBox(null,box, false, "");
        });
    }
    public static void beforeGameGui() {
        var client = Minecraft.getInstance();
        frame = false;
        if (client.player == null || frameGraphics == null || !EnvyWorld.config().client.hudEnabled) return;
        UiRenderer.beginFrame();
        SpectatorOverlay.draw(frameGraphics);
        if (client.screen == null) UiRenderer.overlayCommands(()->draw(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(), false, ""));
        else noticesOverlay();
        UiRenderer.beforeVanillaGui(); frame = true;
    }
    public static void afterGameGui() { if (frame) UiRenderer.afterVanillaGui(); frame = false;frameGraphics=null; }
}
