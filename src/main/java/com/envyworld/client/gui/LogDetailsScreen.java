package com.envyworld.client.gui;

import com.envyworld.logs.PunishLogClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class LogDetailsScreen extends Screen implements MenuScreen {
    private final Screen parent;private final PunishLogClient.Row row;private int scroll;private final ScrollBars bars=new ScrollBars();
    public LogDetailsScreen(Screen parent,PunishLogClient.Row row){super(Component.literal("Подробности наказания"));this.parent=parent;this.row=row;}
    @Override protected void init(){
        addRenderableWidget(UiButton.builder(Component.literal("Назад"),b->onClose()).bounds(width-90,10,78,22).build());
        addRenderableWidget(UiButton.builder(Component.literal("Копировать причину"),b->minecraft.keyboardHandler.setClipboard(row.reason())).bounds(12,height-34,160,22).build());
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float dt){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int x,int y,float dt){
        UiRenderer.beginFrame();bars.beginFrame();var g=new UiGraphics(original,x,y);HudSurface.fullscreen(width,height);
        UiFont.draw(g,row.typeLabel()+"  |  полная запись",12,17,0xFFDCE6F3);
        UiFont.draw(g,"Получил: "+row.punished(),16,54,0xFFEAB5C0);UiFont.draw(g,"Выдал: "+row.punisher(),width/2f,54,0xFFAFCFF1);
        UiFont.draw(g,row.date()+"  |  "+row.server(),16,74,0xFF8EA0B9);
        var timer=com.envyworld.logs.PunishmentClock.status(row);UiFont.draw(g,timer.label(),16,92,timer.color());
        List<String> lines=new ArrayList<>();String reason=row.reason().replaceAll("\\s+-\\s+(?=Предупрежд|Warn|Игрок)","\n");
        lines.addAll(UiFont.wrap(reason,width-56,7));int top=120,h=height-top-50;scroll=Math.max(0,Math.min(scroll,Math.max(0,lines.size()*14-h)));
        g.enableScissor(16,top,width-30,top+h);for(int i=scroll/14;i<lines.size()&&i*14-scroll<h;i++)UiFont.draw(g,lines.get(i),20,top+i*14-scroll,0xFFD2DDEB);g.disableScissor();
        var bar=new ScrollBars.Bar(width-21,top,h,scroll,h,lines.size()*14,v->scroll=v);bars.add(bar.x(),top,h,scroll,h,lines.size()*14,bar.setter());if(bar.maximum()>0)UiGraphics.rounded(bar.x(),bar.thumbY(),3,bar.thumbHeight(),1.5f,0xFF8398B2);super.render(g,x,y,dt);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean d){return bars.press(e.x(),e.y())||super.mouseClicked(e,d);}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return bars.drag(e.y())||super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return bars.release()||super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll=Math.max(0,scroll-(int)(dy*28));return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
