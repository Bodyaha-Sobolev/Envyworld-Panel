package com.envyworld.client.gui;

import com.envyworld.logs.SessionChat;
import com.envyworld.logs.TelegramNotify;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class SessionChatScreen extends Screen implements MenuScreen {
    private final Screen parent;private final ScrollBars bars=new ScrollBars();private int scroll,maximum,page,loaded,cachedWidth=-1;private boolean follow=true;
    private long generation=-1;private String lastContext="",category="Чат";
    private final List<Line> cached=new ArrayList<>();
    private net.minecraft.client.gui.components.Button previous,next,followButton;
    private record Line(String text,int color,String context){}
    public SessionChatScreen(Screen parent){super(Component.literal("Чат сессии"));this.parent=parent;}
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(UiButton.builder(Component.literal(text),b->action.run()).bounds(x,y,w,22).build());}
    @Override protected void init(){
        int w=Math.max(80,(width-48)/5);
        button(TelegramNotify.relayEnabled()?"В Telegram: вкл":"В Telegram: выкл",12,44,w,()->{TelegramNotify.relayEnabled(!TelegramNotify.relayEnabled());rebuildWidgets();});
        followButton=addRenderableWidget(UiButton.builder(Component.literal("Автопрокрутка"),b->follow=!follow).bounds(18+w,44,w,22).build());
        button("Очистить",24+w*2,44,w,()->{SessionChat.clear();scroll=0;});
        button("Файл сессии",30+w*3,44,w,()->{var p=SessionChat.file().getParent();try{java.nio.file.Files.createDirectories(p);net.minecraft.util.Util.getPlatform().openPath(p);}catch(Exception ignored){}});
        button(category,36+w*4,44,w,()->minecraft.setScreen(new ChoiceScreen(this,"Категория сообщений",List.of("Все","Чат","Личные","Команда","Команды игроков","Наказание","События","Друзья").stream().map(v->new ChoiceScreen.Choice(v,v)).toList(),v->{category=v;cachedWidth=-1;page=0;scroll=0;})));
        previous=addRenderableWidget(UiButton.builder(Component.literal("Предыдущая"),b->{follow=false;page=Math.max(0,page-1);scroll=0;}).bounds(12,height-57,100,22).build());
        next=addRenderableWidget(UiButton.builder(Component.literal("Следующая"),b->{follow=false;page=Math.min(pages()-1,page+1);scroll=0;}).bounds(118,height-57,100,22).build());
        button("Назад",width-100,height-32,88,this::onClose);
    }
    private int pages(){return Math.max(1,(cached.size()+99)/100);}
    private void refresh(){
        if(generation!=SessionChat.generation()||cachedWidth!=width){generation=SessionChat.generation();cachedWidth=width;cached.clear();loaded=0;lastContext="";}
        for(var e:SessionChat.since(loaded)){loaded++;if(!category.equals("Все")&&!(category.equals("Чат")?List.of("Чат","Личные","Отправлено").contains(e.kind()):category.equals(e.kind())))continue;
            if(!e.context().equals(lastContext)){for(String line:UiFont.wrap("── "+e.time()+"  |  "+e.context()+" ──",Math.max(30,width-56),7))cached.add(new Line(line,0xFF94B4DB,e.context()));lastContext=e.context();}
            int color=e.kind().equals("Команда")?0xFFD9C69F:e.kind().equals("Отправлено")?0xFFB1DCC8:0xFFCED8E7;
            for(String line:UiFont.wrap("["+e.time()+"] "+e.kind()+": "+e.text(),Math.max(30,width-56),7))cached.add(new Line(line,color,e.context()));
        }
        page=follow?pages()-1:Math.min(page,pages()-1);
    }
    private List<Line> visibleLines(){refresh();int start=Math.min(cached.size(),page*100);return cached.subList(start,Math.min(cached.size(),start+100));}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int mx,int my,float dt){
        UiRenderer.beginFrame();bars.beginFrame();var g=new UiGraphics(original,mx,my);HudSurface.fullscreen(width,height);
        UiFont.draw(g,"Чат и отправленные команды  |  текущая сессия",12,20,0xFFE2EAF5);
        var lines=visibleLines();
        String context=lines.isEmpty()?SessionChat.context():lines.get(Math.min(lines.size()-1,scroll/13)).context();
        UiFont.draw(g,context,12,33,6,0xFF91ADC9);
        int top=78,h=Math.max(20,height-148),total=lines.size()*13;maximum=Math.max(0,total-h);scroll=follow?maximum:Math.min(scroll,maximum);
        g.fill(12,top,width-12,top+h,0x70121923);g.enableScissor(14,top+1,width-26,top+h-1);
        if(lines.isEmpty())UiFont.draw(g,"Новые сообщения появятся здесь автоматически",20,top+10,0xFF8D9BAE);
        for(int i=scroll/13;i<lines.size()&&i*13-scroll<h;i++)UiFont.draw(g,lines.get(i).text(),20,top+i*13-scroll,7,lines.get(i).color());g.disableScissor();
        var bar=new ScrollBars.Bar(width-16,top,h,scroll,h,total,v->{scroll=v;follow=false;});bars.add(bar.x(),top,h,scroll,h,total,bar.setter());UiGraphics.rounded(bar.x()-1,top,5,h,1,0x20B8C5D6);if(maximum>0)UiGraphics.rounded(bar.x(),bar.thumbY(),3,bar.thumbHeight(),1,0xFFA6B3C4);
        previous.active=page>0;next.active=page<pages()-1;followButton.setMessage(Component.literal(follow?"Автопрокрутка: вкл":"Автопрокрутка: выкл"));
        UiFont.draw(g,"Страница "+(page+1)+" / "+pages()+"  |  "+lines.size()+" / 100 строк  |  сообщений за сессию: "+SessionChat.size(),228,height-49,6,0xFF96A8BF);
        UiFont.draw(g,SessionChat.status()+"   Скрыто рекламы: "+SessionChat.filtered(),12,height-27,6,0xFF96A8BF);super.render(g,mx,my,dt);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){return bars.press(e.x(),e.y())||super.mouseClicked(e,doubled);}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return bars.drag(e.y())||super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return bars.release()||super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){follow=false;scroll=Math.max(0,Math.min(maximum,scroll-(int)(dy*26)));return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
