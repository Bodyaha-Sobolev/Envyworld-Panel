package com.envyworld.client.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ChoiceScreen extends Screen implements MenuScreen {
    public record Choice(String value,String label) {}
    private final Screen parent; private final List<Choice> choices; private final Consumer<String> accept;
    private final ScrollBars bars=new ScrollBars(); private String query=""; private int scroll;
    public ChoiceScreen(Screen parent,String title,List<Choice> choices,Consumer<String> accept) { super(Component.literal(title)); this.parent=parent; this.choices=choices; this.accept=accept; }
    @Override protected void init() {
        var search=new UiEditBox(font,12,42,width-24,22,Component.literal("Поиск")); search.setValue(query); search.setResponder(s->{query=s;scroll=0;}); addRenderableWidget(search);
        addRenderableWidget(UiButton.builder(Component.literal("Назад"),b->onClose()).bounds(width-90,10,78,22).build());
    }
    private List<Choice> filtered() { String q=query.toLowerCase(java.util.Locale.ROOT); return choices.stream().filter(c->(c.label()+" "+c.value()).toLowerCase(java.util.Locale.ROOT).contains(q)).toList(); }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float dt) { minecraft.gui.renderDeferredSubtitles(); }
    @Override public void render(GuiGraphics original,int x,int y,float dt) {
        UiRenderer.beginFrame(); bars.beginFrame(); var g=new UiGraphics(original,x,y); HudSurface.fullscreen(width,height);
        UiFont.draw(g,getTitle().getString(),12,17,0xFFE0E8F3); var list=filtered(); int h=height-86; scroll=Math.max(0,Math.min(scroll,Math.max(0,list.size()*28-h)));
        g.enableScissor(12,74,width-24,74+h);
        for(int i=scroll/28;i<list.size()&&i*28-scroll<h;i++) { int yy=74+i*28-scroll;
            if(y>=yy&&y<yy+28) UiGraphics.rounded(12,yy,width-36,28,5,0x383F5068);
            UiFont.draw(g,list.get(i).label(),20,yy+5,0xFFCBD9EA); UiFont.draw(g,list.get(i).value(),20,yy+17,6,0xFF8797AD);
        } g.disableScissor();
        var bar=new ScrollBars.Bar(width-17,74,h,scroll,h,list.size()*28,v->scroll=v); bars.add(bar.x(),74,h,scroll,h,list.size()*28,bar.setter());
        UiGraphics.rounded(bar.x()-1,bar.y(),5,bar.height(),1,0x20B8C5D6);
        if(bar.maximum()>0) UiGraphics.rounded(bar.x(),bar.thumbY(),3,bar.thumbHeight(),1.5f,0xFF8999AF);
        super.render(g,x,y,dt);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled) {
        if(bars.press(e.x(),e.y())||super.mouseClicked(e,doubled)) return true;
        if(e.button()==0&&e.x()>=12&&e.x()<width-24&&e.y()>=74&&e.y()<height-12) { int i=(int)(e.y()-74+scroll)/28;var list=filtered();if(i<list.size()){accept.accept(list.get(i).value());minecraft.setScreen(parent);}return true; }return false;
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll=Math.max(0,scroll-(int)(dy*28));return true;}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return bars.drag(e.y())||super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return bars.release()||super.mouseReleased(e);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
