package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/** Responsive settings rows share the menu font, clipping and draggable scrolling. */
public final class SettingsPanel {
    private record Control(AbstractWidget widget, int x, int offset, String caption) {}
    private final List<Control> controls = new ArrayList<>();
    private final ScrollBars bars = new ScrollBars();
    private final int x, y, width, height, total;
    private int scroll;
    public SettingsPanel(int x, int y, int width, int height, Consumer<AbstractWidget> add, Runnable editHud) {
        int availableWidth=width;width=Math.min(740,width);x+=(availableWidth-width)/2;
        this.x = x; this.y = y; this.width = width; this.height = height;
        var s = EnvyWorld.config().client;
        int columns = width >= 470 ? 2 : 1, w = (width - 24 - (columns - 1) * 12) / columns;
        String[] captions = {"HUD и редактор", "Новые наказания", "Встроенный звук уведомления", "Звук грома", "Молния рядом с игроком", "Вход и выход отслеживаемых", "Громкость уведомлений", "Размещение HUD", "Сетка редактора", "Привязка к сетке", "Проверка уведомления", "Выбор вариантов причины наказания", "Интерфейс GM1 в наблюдателе", "Проверки IP над неймтегом", "Обзор игроков в GM3", "Неймтеги", "Рамки игроков", "Здоровье и шкала HP", "Предметы и броня", "Защита от перегрузки", "Дальность ESP", "Блюр неймтегов", "Разрыв при зависании", "Порог зависания", "Наказания над неймтегами", "Проверять IP из истории", "Бинд /dupeip", "Бинд /iphistory", "Остановить проверку IP", "Анимации кнопок"};
        BooleanSupplier[] values = {() -> s.hudEnabled, () -> s.logNotifications, () -> s.notificationSound, () -> s.thunderSound,
                () -> s.nearbyLightning, () -> s.onlineNotifications, null, null, () -> s.hudGrid, () -> s.hudSnap, null, () -> s.guidedReasons, ()->s.spectatorCreativeUi, ()->s.espChecks, ()->s.spectatorEsp, ()->s.espNames, ()->s.espBoxes, ()->s.espHealth, ()->s.espEquipment, ()->s.clientProtection, null, ()->s.espBlur, ()->s.freezeDisconnect, null, ()->s.espPunishments, ()->s.checkIpAddresses, null, null, null, ()->s.menuAnimations};
        Runnable[] actions = {() -> s.hudEnabled = !s.hudEnabled, () -> s.logNotifications = !s.logNotifications,
                () -> s.notificationSound = !s.notificationSound, () -> s.thunderSound = !s.thunderSound,
                () -> s.nearbyLightning = !s.nearbyLightning, () -> s.onlineNotifications = !s.onlineNotifications,
                () -> s.notificationVolume = s.notificationVolume >= .99f ? 0 : Math.min(1, s.notificationVolume + .1f), editHud,
                () -> s.hudGrid = !s.hudGrid, () -> s.hudSnap = !s.hudSnap,
                () -> { HudNotices.add("Тестовое уведомление", "Настройки звука и оформления", 0xFFB5D5FA); com.envyworld.logs.PunishNotify.playSound(net.minecraft.client.Minecraft.getInstance()); }, () -> s.guidedReasons = !s.guidedReasons, ()->s.spectatorCreativeUi=!s.spectatorCreativeUi, ()->s.espChecks=!s.espChecks, ()->s.spectatorEsp=!s.spectatorEsp, ()->s.espNames=!s.espNames, ()->s.espBoxes=!s.espBoxes, ()->s.espHealth=!s.espHealth, ()->s.espEquipment=!s.espEquipment, ()->s.clientProtection=!s.clientProtection, ()->s.espDistance=s.espDistance>=256?32:s.espDistance*2, ()->s.espBlur=!s.espBlur, ()->s.freezeDisconnect=!s.freezeDisconnect, ()->s.freezeTimeoutSeconds=s.freezeTimeoutSeconds<3?3:s.freezeTimeoutSeconds<5?5:s.freezeTimeoutSeconds<8?8:s.freezeTimeoutSeconds<15?15:s.freezeTimeoutSeconds<30?30:3,
                ()->s.espPunishments=!s.espPunishments,
                ()->{s.checkIpAddresses=!s.checkIpAddresses;if(!s.checkIpAddresses)ProfileChecks.cancel();},
                ()->capture(false), ()->capture(true), ProfileChecks::cancel, ()->s.menuAnimations=!s.menuAnimations};
        for (int i = 0; i < captions.length; i++) {
            final int index = i;
            int xx = x + 8 + i % columns * (w + 12), offset = 40 + i / columns * 42;
            var button = UiButton.builder(Component.literal(label(index, values)), b -> {
                actions[index].run(); EnvyWorld.saveConfig(); b.setMessage(Component.literal(label(index, values)));
            }).bounds(xx+w-88, y+offset, 80, 22).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(captions[i]))).build();
            controls.add(new Control(button, xx, offset, captions[i])); add.accept(button);
        }
        total = 48 + (captions.length + columns - 1) / columns * 42;
        position();
    }
    private static void capture(boolean history){
        var c=net.minecraft.client.Minecraft.getInstance();
        c.setScreen(new KeyCaptureScreen(c.screen,(code,mods)->com.envyworld.client.TargetInspector.bind(history,code),true));
    }
    private String label(int i, BooleanSupplier[] values) {
        if (values[i] != null) return values[i].getAsBoolean() ? "Включено" : "Выключено";
        return switch (i) { case 6 -> Math.round(EnvyWorld.config().client.notificationVolume * 100) + "%";
            case 26,27 -> com.envyworld.client.TargetInspector.bindingName(i==27); case 28 -> "Остановить"; case 23 -> EnvyWorld.config().client.freezeTimeoutSeconds+" с"; case 20 -> EnvyWorld.config().client.espDistance+" м"; case 7 -> "Редактор"; default -> "Проверить"; };
    }
    private void position() {
        scroll = Math.max(0, Math.min(scroll, total - height));
        for (var control : controls) {
            var w = control.widget(); w.setY(y + control.offset() - scroll);
            w.visible = w.getY() >= y + 2 && w.getBottom() <= y + height - 2;
        }
    }
    public void draw(GuiGraphics graphics, int mouseX, int mouseY) {
        position(); bars.beginFrame();
        graphics.enableScissor(x, y, x + width - 12, y + height);
        UiFont.draw(graphics, "Интерфейс, уведомления и защита", x + 8, y + 7 - scroll, 0xFFE2E9F4);
        UiFont.draw(graphics, "Сохраняется автоматически", x+8,y+21-scroll,6,0xFF93A3B8);
        for (var control : controls) if (control.widget().visible) {
            int rowY=y+control.offset()-scroll;
            UiGraphics.rounded(control.x()-2,rowY-4,control.widget().getRight()-control.x()+6,30,5,MenuTheme.PALETTE.backgroundControl());
            UiFont.drawFaded(graphics, control.caption(), control.x()+6, UiFont.centeredY(rowY,22,7), control.widget().getX()-control.x()-12,0xFFB7C5D6);
        }
        graphics.disableScissor();
        {
            var bar = new ScrollBars.Bar(x + width - 4, y, height, scroll, height, total, v -> { scroll = v; position(); });
            bars.add(bar.x(), y, height, scroll, height, total, bar.setter());
            UiGraphics.rounded(bar.x()-1,bar.y(),5,bar.height(),1,0x20B8C5D6);
            if(total>height)UiGraphics.rounded(bar.x(), bar.thumbY(), 3, bar.thumbHeight(), 1.5f, 0xFF64758B);
        }
    }
    public boolean press(double x, double y) { return bars.press(x, y); }
    public boolean drag(double y) { return bars.drag(y); }
    public boolean release() { return bars.release(); }
    public void wheel(double amount) { scroll -= (int) Math.round(amount * 26); position(); }
}
