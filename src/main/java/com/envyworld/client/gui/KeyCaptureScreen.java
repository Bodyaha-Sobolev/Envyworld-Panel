package com.envyworld.client.gui;

import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class KeyCaptureScreen extends Screen implements MenuScreen {
    private final Screen parent;
    private final BiConsumer<Integer,Integer> accept;
    private long armed;
    private final boolean direct;
    public KeyCaptureScreen(Screen parent, BiConsumer<Integer,Integer> accept) {this(parent,accept,false);}
    public KeyCaptureScreen(Screen parent, BiConsumer<Integer,Integer> accept,boolean direct) { super(Component.literal("Назначение клавиши")); this.parent = parent; this.accept = accept;this.direct=direct; }
    @Override protected void init() {
        armed = System.currentTimeMillis() + 300;
        if(!direct)addRenderableWidget(UiButton.builder(Component.literal("Общая клавиша"), b -> finish(-1,0)).bounds(width/2-177,height/2+28,114,22).build());
        addRenderableWidget(UiButton.builder(Component.literal("Снять клавишу"), b -> finish(0,0)).bounds(width/2-57,height/2+28,114,22).build());
        addRenderableWidget(UiButton.builder(Component.literal("Отмена"), b -> onClose()).bounds(width/2+63,height/2+28,114,22).build());
    }
    private void finish(int code,int mods) { accept.accept(code,mods&15); minecraft.setScreen(parent); }
    @Override public boolean keyPressed(KeyEvent e) { if(e.key()==GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; } finish(e.key(),e.modifiers()); return true; }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled) { if(super.mouseClicked(e,doubled)) return true; if(System.currentTimeMillis()>=armed) finish(-e.button()-2,e.modifiers()); return true; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float dt) { minecraft.gui.renderDeferredSubtitles(); }
    @Override public void render(GuiGraphics original,int x,int y,float dt) {
        UiRenderer.beginFrame(); var g=new UiGraphics(original,x,y); HudSurface.fullscreen(width,height);
        String text=direct?"Нажмите клавишу для проверки игрока под прицелом":"Нажмите клавишу или сочетание с Ctrl / Shift / Alt";
        UiFont.draw(g,text,(width-UiFont.width(text))/2f,height/2-12,0xFFE0E8F3);
        UiFont.draw(g,"Можно назначить и кнопку мыши",width/2f-100,height/2+5,0xFF99A9BE); super.render(g,x,y,dt);
    }
}
