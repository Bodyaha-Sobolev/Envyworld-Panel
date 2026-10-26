package com.envyworld.client.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.envyworld.rules.RulePreset;

/** Builds a concrete reason; applying it returns to the menu and never sends a command. */
public final class ReasonChoiceScreen extends Screen implements MenuScreen {
    private final Screen parent;private final RulePreset rule;private final String offender;private final Consumer<String> accept;
    private String offense="",channel="",victim="",status="";private boolean playerVictim;
    public ReasonChoiceScreen(Screen parent,RulePreset rule,String offender,Consumer<String> accept){super(Component.literal("Уточните нарушение"));this.parent=parent;this.rule=rule;this.offender=offender;this.accept=accept;}
    @Override protected void init(){
        addRenderableWidget(UiButton.builder(Component.literal("Назад"),b->onClose()).bounds(width-90,10,78,22).build());
        List<String> offenses=rule.code.startsWith("2.1")?List.of("Оскорбление","Насмешка","Хамство"):rule.code.equals("2.5")?List.of("Провокация","Подстрекательство","Подстава игроков"):
                java.util.Arrays.stream(rule.title.split(",\\s*|\\s+или\\s+")).map(String::trim).filter(s->!s.isBlank()).toList();
        if(offense.isBlank()&&!offenses.isEmpty())status="Выберите, что именно произошло";
        int w=Math.min(210,(width-36)/Math.max(1,Math.min(3,offenses.size())));
        for(int i=0;i<offenses.size();i++){String option=offenses.get(i);button(option,12+(i%3)*(w+6),64+(i/3)*27,w,()->{offense=option;status="";});}
        int y=64+((offenses.size()+2)/3)*27+30;
        String[] channels=rule.title.contains("/bc")?new String[]{"/bc","/toast","/titlemsg"}:new String[]{"Общий чат","Личные сообщения","Голосовой чат"};
        for(int i=0;i<channels.length;i++){String option=channels[i];button(option,12+i*(w+6),y,w,()->channel=option);}
        int targetY=y+55;
        button("Конкретного игрока",12,targetY,w,()->{playerVictim=true;status="Укажите ник пострадавшего";});
        button("Участников чата",18+w,targetY,w,()->{playerVictim=false;victim="участников чата";status="";});
        var field=new UiEditBox(font,12,targetY+32,Math.min(width-24,430),22,Component.literal("Ник пострадавшего"));field.setMaxLength(16);field.setValue(playerVictim?victim:"");field.setResponder(s->{victim=s;playerVictim=true;});addRenderableWidget(field);
        button("Применить причину",12,height-34,180,()->{
            if(offense.isBlank()||channel.isBlank()){status="Выберите нарушение и канал";return;}
            if(rule.code.startsWith("2.1")&&(victim.isBlank()||playerVictim&&!victim.matches("[A-Za-z0-9_]{1,16}"))){status="Выберите пострадавшего или укажите его ник";return;}
            accept.accept(reason());minecraft.setScreen(parent);
        });
    }
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(UiButton.builder(Component.literal(text),b->action.run()).bounds(x,y,w,22).build());}
    private String reason(){
        String action=victim.isBlank()?offense:offender+switch(offense){case "Оскорбление"->" оскорбил ";case "Насмешка"->" высмеял ";case "Хамство"->" нагрубил ";default->"  |  "+offense+"  |  ";}+victim;
        boolean context=rule.code.equals("2.1.2")||rule.code.equals("2.1.3")||rule.code.equals("2.1.4");
        return "["+rule.code+"] "+action+(context?"  |  "+rule.title:"")+"  |  "+channel;
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float dt){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int x,int y,float dt){UiRenderer.beginFrame();var g=new UiGraphics(original,x,y);HudSurface.fullscreen(width,height);
        UiFont.draw(g,"Уточните нарушение  |  ["+rule.code+"]  |  "+offender,12,17,0xFFE0E9F5);
        UiFont.draw(g,"Выбрано: "+(offense.isBlank()?"нарушение не выбрано":offense)+"  |  "+(channel.isBlank()?"канал не выбран":channel),12,43,0xFF9DB2CE);
        int h=Math.min(70,height/6);var lines=UiFont.wrap(reason(),width-32);for(int i=0;i<lines.size()&&i<3;i++)UiFont.draw(g,lines.get(i),12,height-108+i*13,0xFFBED0E7);
        if(!status.isBlank())UiFont.draw(g,status,12,height-51,0xFFE8B8BE);super.render(g,x,y,dt);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
