package com.envyworld.client.gui;

import com.envyworld.logs.TelegramNotify;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;

public final class TelegramScreen extends Screen implements MenuScreen {
    private final Screen parent;private EditBox token;private String draft="";
    public TelegramScreen(Screen parent){super(Component.literal("Telegram"));this.parent=parent;}
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(UiButton.builder(Component.literal(text),b->action.run()).bounds(x,y,w,22).build());}
    private void open(String url){if(!url.isBlank())net.minecraft.util.Util.getPlatform().openUri(url);}
    @Override protected void init(){
        int w=Math.min(620,width-32),x=(width-w)/2,third=(w-12)/3;
        token=new EditBox(font,x,148,w,22,Component.literal("Токен личного бота")){
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){UiGraphics.rounded(getX(),getY(),getWidth(),getHeight(),5,0xFF11161E);UiGraphics.outline(getX(),getY(),getWidth(),getHeight(),5,isFocused()?0xFF91AACB:0xFF3D4B5E);g.enableScissor(getX()+4,getY()+1,getRight()-4,getBottom()-1);UiFont.draw(g,getValue().isBlank()?"Вставьте токен из BotFather":"*".repeat(getValue().length()),getX()+6,UiFont.centeredY(getY(),getHeight(),7),0xFFC4D3E5);g.disableScissor();}
        };token.setMaxLength(160);token.setValue(draft);token.setResponder(v->draft=v);addRenderableWidget(token);
        button("BotFather",x,178,third,()->open("https://t.me/BotFather"));
        button("Проверить токен",x+third+6,178,third,()->TelegramNotify.prepare(token.getValue()));
        button("Открыть бота",x+2*(third+6),178,third,()->open(TelegramNotify.botLink()));
        button("Подключить личный чат",x,250,(w-6)/2,TelegramNotify::pair);
        button("Проверить связь",x+(w+6)/2,250,(w-6)/2,TelegramNotify::checkConnection);
        button("Тестовое сообщение",x,314,third,TelegramNotify::testMessage);
        button("Чат и команды",x+third+6,314,third,()->minecraft.setScreen(new SessionChatScreen(this)));
        button("Отключить",x+2*(third+6),314,third,TelegramNotify::disable);
        button("Назад",width-110,height-34,90,this::onClose);
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int mx,int my,float dt){UiRenderer.beginFrame();var g=new UiGraphics(original,mx,my);HudSurface.fullscreen(width,height);int w=Math.min(620,width-32),x=(width-w)/2;
        UiFont.draw(g,"Личный Telegram-бот",x,22,0xFFE2EAF5);
        UiIcons.draw(TelegramNotify.alive()?"circle-check":"circle-x",x+5,49,11,TelegramNotify.alive()?0xFF91D4B2:0xFF94A3B8);
        UiFont.draw(g,TelegramNotify.health(),x+16,45,6,0xFFA7BAD0);
        String[] steps={"1. Откройте BotFather и отправьте отдельным сообщением: /newbot","2. Отправьте имя бота, например: Bodyaha Envy Test","3. Отправьте свободный username, например: bodyahaenvytest1_bot","4. Скопируйте токен из ответа BotFather и вставьте в поле ниже."};
        for(int i=0;i<steps.length;i++)UiFont.draw(g,steps[i],x,74+i*17,6,0xFFB6C5D8);
        UiFont.draw(g,"5. Нажмите «Проверить токен», затем «Открыть бота».",x,214,6,0xFFB6C5D8);
        UiFont.draw(g,"6. В Telegram нажмите Start. Здесь нажмите «Подключить личный чат».",x,231,6,0xFFB6C5D8);
        int y=284;for(String line:UiFont.wrap(TelegramNotify.status(),w,6)){if(y>304)break;UiFont.draw(g,line,x,y,6,0xFFBFD2EA);y+=10;}
        UiFont.draw(g,"7. Отправьте тест. Передачу чата включите в разделе «Чат и команды».",x,349,6,0xFFB6C5D8);
        UiFont.draw(g,"Токен остаётся на этом ПК. Бот работает, пока Minecraft запущен.",x,366,6,0xFF899BAF);
        super.render(g,mx,my,dt);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
