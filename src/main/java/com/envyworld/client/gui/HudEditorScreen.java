package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;
import com.envyworld.config.ClientSettings.HudBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditorScreen extends Screen implements MenuScreen {
    private final Screen parent;
    private String selected = "macros", dragging = "";
    private double grabX, grabY;
    private int contextX, contextY;
    private boolean context, opacityDrag;
    private final UiMotion[] toggles={new UiMotion(0),new UiMotion(0),new UiMotion(0),new UiMotion(0)};
    public HudEditorScreen(Screen parent) { super(Component.literal("Редактор HUD")); this.parent = parent; }
    @Override protected void init() {
        addRenderableWidget(UiButton.builder(Component.literal("Готово"), button -> onClose()).bounds(width - 88, 10, 78, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Сетка: " + (EnvyWorld.config().client.hudGrid ? "да" : "нет")), button -> {
            EnvyWorld.config().client.hudGrid = !EnvyWorld.config().client.hudGrid;
            button.setMessage(Component.literal("Сетка: " + (EnvyWorld.config().client.hudGrid ? "да" : "нет")));
        }).bounds(10, 10, 70, 20).build());
        addRenderableWidget(UiButton.builder(Component.literal("Привязка: " + (EnvyWorld.config().client.hudSnap ? "да" : "нет")), button -> {
            EnvyWorld.config().client.hudSnap = !EnvyWorld.config().client.hudSnap;
            button.setMessage(Component.literal("Привязка: " + (EnvyWorld.config().client.hudSnap ? "да" : "нет")));
        }).bounds(86, 10, 100, 20).build());
    }
    @Override public void renderBackground(GuiGraphics graphics, int x, int y, float delta) { minecraft.gui.renderDeferredSubtitles(); }
    @Override public void render(GuiGraphics original, int mouseX, int mouseY, float delta) {
        UiRenderer.beginFrame(); GuiGraphics g = new UiGraphics(original, mouseX, mouseY);
        g.fill(0, 0, width, height, 0x18000000);
        if (EnvyWorld.config().client.hudGrid) {
            for(int zone=1;zone<3;zone++){
                UiGraphics.rounded(width*zone/3f,0,.5f,height,0,0x30FFFFFF);
                UiGraphics.rounded(0,height*zone/3f,width,.5f,0,0x30FFFFFF);
            }
            if(!dragging.isEmpty()){
                UiGraphics.rounded(width/2f,0,.5f,height,0,0x50FFFFFF);
                UiGraphics.rounded(0,height/2f,width,.5f,0,0x50FFFFFF);
            }
        }
        UiFont.draw(g, "ЛКМ: перенос  |  ПКМ: настройки  |  Shift: без привязки", 12, height - 16, 6, 0xFFB5B5B8);
        EnvyHud.draw(g,width, height, true, selected);
        super.render(g, mouseX, mouseY, delta);
        if (context) drawContext(g, mouseX, mouseY);
        drawSelector(g);
    }
    private int selectorX(){return width-170;}
    private HudBox[] hudBlocks(){var c=EnvyWorld.config().client;return new HudBox[]{c.macros,c.online,c.notices};}
    private void drawSelector(GuiGraphics g){
        int x=selectorX(),y=40,w=160,h=252;var c=EnvyWorld.config().client;
        HudSurface.draw(x,y,w,h,14,Math.clamp(c.hudOpacity,120,255));UiGraphics.outline(x,y,w,h,14,0x30FFFFFF);
        UiFont.draw(g,"Элементы HUD",x+9,y+9,7,0xFFF0F0F0);
        String[] names={"Макросы","Отслеживаемые игроки","Уведомления","Размытие фона"};var blocks=hudBlocks();
        for(int i=0;i<4;i++){
            int row=i==3?y+146:y+29+i*22;boolean enabled=i==3?c.hudBlur:blocks[i].visible;
            if(i<3&&selected.equals(new String[]{"macros","online","notices"}[i]))UiGraphics.rounded(x+5,row,125,20,3,0x18FFFFFF);
            UiFont.draw(g,names[i],x+9,row+5,6,0xFFD0D0D3);
            float t=toggles[i].update(enabled?1:0);int tx=x+w-27;
            UiGraphics.rounded(tx,row+4,17,10,10,enabled?0xFFE2E2E5:0xFF303034);
            UiGraphics.rounded(tx+2+t*7,row+6,6,6,6,enabled?0xFF202022:0xFF99999D);
        }
        UiGraphics.rounded(x+8,y+99,w-16,.5f,0,0x22FFFFFF);
        UiFont.draw(g,"Прозрачность фона",x+9,y+108,6,0xFFB8B8BC);
        UiFont.draw(g,Math.round(Math.clamp(c.hudOpacity,120,255)/255f*100)+"%",x+w-29,y+108,6,0xFFEAEAEA);
        float t=(Math.clamp(c.hudOpacity,120,255)-120)/135f;
        UiGraphics.rounded(x+10,y+131,w-20,2,1,0xFF303034);UiGraphics.rounded(x+10,y+131,(w-20)*t,2,1,0xFFDADADD);
        UiGraphics.rounded(x+7+(w-20)*t,y+129,6,6,6,0xFFF5F5F5);
        UiFont.draw(g,"Положение выбранного блока",x+9,y+176,6,0xFFB8B8BC);
        var setting=selectedSettings();
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            float bx=x+10+col*47,by=y+191+row*17;
            boolean active=setting.anchorX==col&&setting.anchorY==row;
            UiGraphics.rounded(bx,by,43,14,3,active?0xFF3A3A3E:0xFF202023);
            UiGraphics.rounded(bx+5+col*14,by+3+row*3,5,3,1,active?0xFFFFFFFF:0xFF9A9A9F);
        }
    }
    private void preset(int column,int row){var b=selectedSettings();b.anchorX=column;b.anchorY=row;EnvyWorld.saveConfig();}
    private void opacity(double mouseX){EnvyWorld.config().client.hudOpacity=120+Math.round((float)Math.clamp((mouseX-selectorX()-10)/140,0,1)*135);}
    private HudBox selectedSettings() {
        return switch (selected) { case "online" -> EnvyWorld.config().client.online; case "notices" -> EnvyWorld.config().client.notices; default -> EnvyWorld.config().client.macros; };
    }
    private void drawContext(GuiGraphics g, int mouseX, int mouseY) {
        var settings = selectedSettings();
        HudSurface.draw(contextX,contextY,164,108,12,Math.clamp(EnvyWorld.config().client.hudOpacity,120,255));
        UiGraphics.outline(contextX, contextY, 164, 108, 8, 0x40FFFFFF);
        String[] labels = {"Показывать: " + (settings.visible ? "да" : "нет"), "Фон: " + (settings.background ? "да" : "нет"),
                "Масштаб: " + Math.round(settings.scale * 100) + "%   − / +", "Вернуть исходную позицию"};
        UiFont.draw(g, "Настройки блока", contextX + 10, contextY + 9, 0xFFE0E7F1);
        for (int i = 0; i < labels.length; i++) {
            int y = contextY + 27 + i * 19;
            if (inside(mouseX, mouseY, contextX + 4, y - 2, 156, 19)) UiGraphics.rounded(contextX + 4, y - 2, 156, 19, 5, 0x4029292C);
            UiFont.draw(g, labels[i], contextX + 10, y, 6, 0xFFB6C6DA);
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if(inside(event.x(),event.y(),selectorX(),40,160,252)){
            if(event.button()==0){
                int row=(int)((event.y()-69)/22);
                if(event.y()>=69&&event.y()<135&&row>=0&&row<3){if(event.x()>selectorX()+130){var b=hudBlocks()[row];b.visible=!b.visible;}else selected=new String[]{"macros","online","notices"}[row];}
                else if(event.y()>=164&&event.y()<181){opacityDrag=true;opacity(event.x());}
                else if(event.y()>=231&&event.y()<282&&event.x()>=selectorX()+10&&event.x()<selectorX()+151)preset(Math.min(2,(int)(event.x()-selectorX()-10)/47),Math.min(2,(int)(event.y()-231)/17));
                else if(event.y()>=186&&event.y()<209)EnvyWorld.config().client.hudBlur=!EnvyWorld.config().client.hudBlur;
                EnvyWorld.saveConfig();
            }
            context=false;return true;
        }
        if (context && inside(event.x(), event.y(), contextX, contextY, 164, 108)) {
            if (event.button() == 0) {
                int row = (int) ((event.y() - contextY - 25) / 19);
                var settings = selectedSettings();
                if (event.y() >= contextY + 25) switch (row) {
                    case 0 -> settings.visible = !settings.visible;
                    case 1 -> settings.background = !settings.background;
                    case 2 -> settings.scale = Math.max(.65f, Math.min(1.7f, settings.scale + (event.x() > contextX + 130 ? .1f : -.1f)));
                    case 3 -> { settings.anchorX=-1;settings.anchorY=-1;settings.x = selected.equals("notices") ? .73f : .015f; settings.y = selected.equals("online") ? .37f : selected.equals("notices") ? .12f : .19f; settings.scale = 1; }
                }
                EnvyWorld.saveConfig();
            }
            return true;
        }
        context = false;
        if (super.mouseClicked(event, doubled)) return true;
        var boxes = EnvyHud.boxes(width, height, true);
        for (int i = boxes.size() - 1; i >= 0; i--) {
            var box = boxes.get(i);
            if (!box.contains(event.x(), event.y())) continue;
            selected = box.id();
            if (event.button() == 1) { context = true; contextX = Math.min(width - 174, (int) event.x()); contextY = Math.min(height - 118, Math.max(40, (int) event.y())); }
            else if (event.button() == 0) { dragging = box.id(); grabX = event.x() - box.x(); grabY = event.y() - box.y(); }
            return true;
        }
        selected = ""; return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if(opacityDrag){opacity(event.x());return true;}
        if (dragging.isEmpty()) return super.mouseDragged(event, dx, dy);
        var box = EnvyHud.boxes(width, height, true).stream().filter(value -> value.id().equals(dragging)).findFirst().orElseThrow();
        double x = event.x() - grabX, y = event.y() - grabY;
        if (EnvyWorld.config().client.hudSnap && !event.hasShiftDown()) { x = Math.round(x / 24) * 24; y = Math.round(y / 24) * 24; }
        box.settings().anchorX=-1;box.settings().anchorY=-1;
        box.settings().x = (float) (Math.max(6, Math.min(width - box.width() - 6, x)) / width);
        box.settings().y = (float) (Math.max(6, Math.min(height - box.height() - 6, y)) / height);
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(opacityDrag){opacityDrag=false;EnvyWorld.saveConfig();return true;}
        if (!dragging.isEmpty()) { dragging = ""; EnvyWorld.saveConfig(); return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (selected.isEmpty()) return false;
        var settings = selectedSettings(); settings.scale = Math.max(.65f, Math.min(1.7f, settings.scale + (float) dy * .05f)); EnvyWorld.saveConfig(); return true;
    }
    @Override public void onClose() { EnvyWorld.saveConfig(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    private static boolean inside(double x, double y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
}
