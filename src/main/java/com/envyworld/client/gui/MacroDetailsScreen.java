package com.envyworld.client.gui;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** One command per row; literal semicolons inside command arguments remain intact. */
public final class MacroDetailsScreen extends Screen implements MenuScreen {
    public interface Save {void accept(String name,String description,String commands,List<Integer> delays);}
    private final Screen parent;private final Save save;private String name,description;private final List<String> commands=new ArrayList<>();
    private final List<String> delays=new ArrayList<>();private String status="";
    private final List<EditBox> delayFields=new ArrayList<>();
    private final List<EditBox> fields=new ArrayList<>();private final ScrollBars bars=new ScrollBars();private int scroll;
    public MacroDetailsScreen(Screen parent,String name,String description,String commands,List<Integer> delays,Save save){super(Component.literal("Название и команды макроса"));this.parent=parent;this.name=name;this.description=description;this.save=save;this.commands.addAll(Arrays.asList(commands.split("\n",-1)));for(int i=0;i<this.commands.size();i++)this.delays.add(Integer.toString(delays!=null&&i<delays.size()&&delays.get(i)!=null?Math.max(0,delays.get(i)):0));}
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(UiButton.builder(Component.literal(text),b->action.run()).bounds(x,y,w,22).build());}
    private EditBox field(int x,int y,int w,String value,String hint,java.util.function.Consumer<String> change){var f=new UiEditBox(font,x,y,w,22,Component.literal(hint));f.setMaxLength(2048);f.setValue(value);f.setHint(Component.literal(hint));f.setResponder(change);addRenderableWidget(f);return f;}
    @Override protected void init(){
        fields.clear();delayFields.clear();field(16,54,width-32,name,"Название макроса",v->name=v);field(16,100,width-32,description,"Нет описания",v->description=v);
        for(int i=0;i<commands.size();i++){final int index=i;fields.add(field(42,158+i*28-scroll,width-192,commands.get(i),"/команда  |  %target% — выбранный игрок",v->commands.set(index,v)));delayFields.add(field(width-140,158+i*28-scroll,112,delays.get(i),"0",v->delays.set(index,v)));}
        button("+ Команда",16,height-37,104,()->{if(commands.size()<16){commands.add("");delays.add("0");rebuildWidgets();}});
        button("− Последняя",126,height-37,104,()->{if(commands.size()>1){commands.removeLast();delays.removeLast();rebuildWidgets();}});
        button("Сохранить",width-214,height-37,96,this::save);
        button("Назад",width-112,height-37,96,this::onClose);
    }
    private void save(){var kept=new ArrayList<String>();var waits=new ArrayList<Integer>();try{for(int i=0;i<commands.size();i++)if(!commands.get(i).isBlank()){int delay=Integer.parseInt(delays.get(i).strip());if(delay<0||delay>3600000)throw new NumberFormatException();kept.add(commands.get(i).strip());waits.add(delay);}}catch(NumberFormatException e){status="Задержка должна быть числом от 0 до 3600000 мс";return;}save.accept(name.strip(),description.strip(),String.join("\n",kept),waits);onClose();}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int mx,int my,float dt){
        UiRenderer.beginFrame();bars.beginFrame();var g=new UiGraphics(original,mx,my);HudSurface.fullscreen(width,height);
        UiFont.draw(g,"Название, описание и набор команд",16,20,0xFFE2EAF5);UiFont.draw(g,"Название",16,42,6,0xFF9DAFC5);UiFont.draw(g,"Описание",16,88,6,0xFF9DAFC5);
        UiFont.draw(g,"Каждая команда на своей строке. Задержка перед выполнением задаётся справа.",16,136,6,0xFF9DAFC5);
        UiFont.draw(g,"Пауза перед, мс",width-140,146,6,0xFF9DAFC5);
        int h=Math.max(22,height-212),total=commands.size()*28;scroll=Math.max(0,Math.min(scroll,Math.max(0,total-h)));
        for(int i=0;i<fields.size();i++){var f=fields.get(i);f.setY(158+i*28-scroll);f.visible=f.getY()>=158&&f.getBottom()<=158+h;var delay=delayFields.get(i);delay.setY(f.getY());delay.visible=f.visible;if(f.visible)UiFont.draw(g,(i+1)+".",18,f.getY()+7,6,0xFF9DAFC5);}
        var bar=new ScrollBars.Bar(width-20,158,h,scroll,h,total,v->scroll=v);bars.add(bar.x(),158,h,scroll,h,total,bar.setter());UiGraphics.rounded(bar.x()-1,158,5,h,1,0x20B8C5D6);if(total>h)UiGraphics.rounded(bar.x(),bar.thumbY(),3,bar.thumbHeight(),1,0xFFA6B3C4);
        if(!status.isBlank())UiFont.draw(g,status,16,height-49,6,0xFFE4A5AA);super.render(g,mx,my,dt);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){return bars.press(e.x(),e.y())||super.mouseClicked(e,doubled);}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return bars.drag(e.y())||super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return bars.release()||super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(y>=158&&y<height-54)scroll=Math.max(0,Math.min(Math.max(0,commands.size()*28-Math.max(22,height-212)),scroll-(int)(dy*28)));return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
