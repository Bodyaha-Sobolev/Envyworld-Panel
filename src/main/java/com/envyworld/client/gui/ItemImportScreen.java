package com.envyworld.client.gui;

import com.envyworld.items.CustomItems;
import com.envyworld.items.CustomItemPreset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ItemImportScreen extends Screen implements MenuScreen {
    private record Entry(String source,ItemStack stack) {}
    private final Screen parent; private final Consumer<CustomItemPreset> saved; private int source,scroll; private final ScrollBars bars=new ScrollBars();
    private List<Entry> cached=List.of();private int cachedSource=-1;private long cacheUntil;
    public ItemImportScreen(Screen parent,Consumer<CustomItemPreset> saved){super(Component.literal("Импорт предметов"));this.parent=parent;this.saved=saved;}
    @Override protected void init(){
        addRenderableWidget(UiButton.builder(Component.literal("Инвентарь"),b->{source=0;scroll=0;}).bounds(12,42,120,22).build());
        addRenderableWidget(UiButton.builder(Component.literal("Сохранённые панели креатива"),b->{source=1;scroll=0;}).bounds(140,42,200,22).build());
        addRenderableWidget(UiButton.builder(Component.literal("Назад"),b->onClose()).bounds(width-90,10,78,22).build());
    }
    private List<Entry> entries(){
        if(source==cachedSource && System.currentTimeMillis()<cacheUntil)return cached;
        List<Entry> result=new ArrayList<>();if(minecraft.player==null)return result;
        if(source==0){var inv=minecraft.player.getInventory();for(int i=0;i<inv.getContainerSize();i++)if(!inv.getItem(i).isEmpty())result.add(new Entry("Инвентарь  |  слот "+(i+1),inv.getItem(i).copy()));}
        else for(int row=0;row<9;row++){var stacks=minecraft.getHotbarManager().get(row).load(minecraft.level.registryAccess());for(int i=0;i<stacks.size();i++)if(!stacks.get(i).isEmpty())result.add(new Entry("Панель "+(row+1)+"  |  слот "+(i+1),stacks.get(i).copy()));}
        cached=List.copyOf(result);cachedSource=source;cacheUntil=System.currentTimeMillis()+1000;return cached;
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float dt){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int x,int y,float dt){
        UiRenderer.beginFrame();bars.beginFrame();var g=new UiGraphics(original,x,y);HudSurface.fullscreen(width,height);
        UiFont.draw(g,"Импорт: исходные компоненты и NBT сохраняются",12,17,0xFFDDE6F2);var list=entries();int h=height-92;scroll=Math.max(0,Math.min(scroll,Math.max(0,list.size()*32-h)));
        if(list.isEmpty())UiFont.draw(g,minecraft.player==null?"Подключитесь к миру":"В этом источнике нет предметов",20,94,0xFF92A3BA);
        g.enableScissor(12,80,width-26,height-12);
        for(int i=scroll/32;i<list.size()&&i*32-scroll<h;i++){int yy=80+i*32-scroll;var e=list.get(i);boolean hover=x>=12&&x<width-26&&y>=yy&&y<yy+32;
            if(hover)UiGraphics.rounded(12,yy,width-38,31,5,0x3B34465D);g.renderItem(e.stack(),20,yy+8);
            UiText.component(g,e.stack().getHoverName(),44,yy+6,width-80,7,0xFFE0E8F4);UiFont.draw(g,e.source(),44,yy+20,6,0xFF91A3BA);
        }g.disableScissor();var bar=new ScrollBars.Bar(width-18,80,h,scroll,h,list.size()*32,v->scroll=v);bars.add(bar.x(),80,h,scroll,h,list.size()*32,bar.setter());UiGraphics.rounded(bar.x()-1,bar.y(),5,bar.height(),1,0x20B8C5D6);if(bar.maximum()>0)UiGraphics.rounded(bar.x(),bar.thumbY(),3,bar.thumbHeight(),1.5f,0xFF8195AF);
        super.render(g,x,y,dt);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){if(bars.press(e.x(),e.y())||super.mouseClicked(e,doubled))return true;
        if(e.button()==0&&e.y()>=80&&e.y()<height-12&&e.x()<width-26){int i=(int)(e.y()-80+scroll)/32;var list=entries();if(i<list.size())minecraft.setScreen(new ItemEditorScreen(parent,CustomItems.fromStack(list.get(i).stack(),minecraft.getUser().getName()),saved));return true;}return false;}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll=Math.max(0,scroll-(int)(dy*32));return true;}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return bars.drag(e.y())||super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return bars.release()||super.mouseReleased(e);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
