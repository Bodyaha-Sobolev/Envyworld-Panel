package com.envyworld.client.gui;

import com.envyworld.logs.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The account currently running this mod; never requests another player's private information. */
public final class OwnProfileScreen extends Screen implements MenuScreen {
    private final Screen parent;
    public OwnProfileScreen(Screen parent){super(Component.literal("Мой профиль"));this.parent=parent;}
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(UiButton.builder(Component.literal(text),b->action.run()).bounds(x,y,w,24).build());}
    @Override protected void init(){
        int w=Math.min(580,width-32),x=(width-w)/2;
        button("Telegram и привязка",x,240,(w-8)/2,()->minecraft.setScreen(new TelegramScreen(this)));
        button("Чат текущей сессии",x+(w+8)/2,240,(w-8)/2,()->minecraft.setScreen(new SessionChatScreen(this)));
        button("Проверить бота",x,272,(w-8)/2,TelegramNotify::checkConnection);
        button("Настройки",x+(w+8)/2,272,(w-8)/2,()->{if(parent instanceof EnvyScreen menu)menu.openSettings();minecraft.setScreen(parent);});
        button("Назад",width-104,height-34,88,this::onClose);
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int mx,int my,float dt){
        UiRenderer.beginFrame();var g=new UiGraphics(original,mx,my);HudSurface.fullscreen(width,height);int w=Math.min(580,width-32),x=(width-w)/2;
        var state=SessionState.snapshot();String nick=minecraft.getUser().getName();
        PlayerFaceRenderer.draw(g,PlayerProfileView.cached(nick).skin(),x+8,28,34);
        UiFont.draw(g,nick,x+54,32,10,0xFFE5EDF7);UiFont.draw(g,state.visibility(),x+54,51,6,0xFFACC4DE);
        UiGraphics.rounded(x,78,w,146,9,MenuTheme.PALETTE.backgroundControl());
        String[] lines={"Сервер и режим: "+state.context(),"Время в игре за сессию: "+SessionState.duration(state.onlineMs()),"Невидимость за сессию: "+SessionState.duration(state.invisibleMs()),"Telegram: "+TelegramNotify.ownerLabel(),"Бот: "+TelegramNotify.botName(),TelegramNotify.health(),"Сообщений в сессии: "+SessionChat.size()+"   Скрыто рекламы: "+SessionChat.filtered()};
        int y=90;for(String line:lines){UiFont.draw(g,line,x+12,y,7,0xFFBCCDE2);y+=18;}
        super.render(g,mx,my,dt);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
