package com.envyworld.client.gui;

import com.envyworld.items.CustomItemPreset;
import com.envyworld.items.ItemText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Editing controls operate on selected text; the right column previews the actual components. */
public final class ItemTextEditorScreen extends Screen implements MenuScreen {
    private final Screen parent; private final CustomItemPreset draft;
    private String name,itemName,start="#55AAFF",end="#FF55AA",status="";
    private final String[] fonts=new String[3];
    private final List<String> lore=new ArrayList<>();
    private final List<EditBox> lines=new ArrayList<>(); private final ScrollBars bars=new ScrollBars();
    private EditBox active; private int section,scroll,fieldW,viewH;
    private final String[] palette={"#FFFFFF","#AAAAAA","#555555","#000000","#FF5555","#AA0000","#FFAA00","#FFFF55","#55FF55","#00AA00","#55FFFF","#00AAAA","#5555FF","#0000AA","#FF55FF","#AA00AA"};
    public ItemTextEditorScreen(Screen parent,CustomItemPreset draft){super(Component.literal("Текст и градиент"));this.parent=parent;this.draft=draft;name=draft.name;itemName=draft.itemName;lore.addAll(Arrays.asList(draft.lore.split("\\|",-1)));fonts[0]=draft.nameFont;fonts[1]=draft.loreFont;fonts[2]=draft.itemNameFont;}
    @Override protected void init(){
        lines.clear();active=null;fieldW=Math.max(180,width*2/3-24);viewH=Math.max(44,height-284);
        String[] labels={"B","I","U","S","Magic","Сброс"},codes={"&l","&o","&n","&m","&k","&r"};
        int stride=Math.min(48,(width-24)/9);
        for(int i=0;i<labels.length;i++){final String code=codes[i];button(labels[i],12+i*stride,42,stride-4,22,()->transform(v->ItemText.format(v,code)));}
        button("Шрифт",12+6*stride,42,90,22,()->{final int target=section;minecraft.setScreen(new ChoiceScreen(this,"Шрифт текста",List.of(new ChoiceScreen.Choice("minecraft:default","Minecraft  |  обычный"),new ChoiceScreen.Choice("minecraft:uniform","Unicode  |  ровный"),new ChoiceScreen.Choice("minecraft:alt","Альтернативные символы")),v->fonts[target]=v));});
        int swatch=Math.min(20,(width-24)/palette.length);
        for(int i=0;i<palette.length;i++){final String color=palette[i];button("",12+i*swatch,72,swatch-2,17,()->transform(v->ItemText.recolor(v,color)));}
        field(start,12,100,86,v->start=v);field(end,104,100,86,v->end=v);
        button("Применить градиент",198,100,146,22,()->{if(!start.matches("#[0-9a-fA-F]{6}")||!end.matches("#[0-9a-fA-F]{6}")){status="Цвет: #RRGGBB";return;}transform(v->ItemText.gradient(v,start,end));});
        EditBox nameBox=new StyledTextBox(font,12,147,fieldW,22);nameBox.setMaxLength(32767);nameBox.setValue(name);nameBox.setResponder(v->name=v);lines.add(addRenderableWidget(nameBox));
        EditBox baseBox=new StyledTextBox(font,12,192,fieldW,22);baseBox.setMaxLength(32767);baseBox.setValue(itemName);baseBox.setResponder(v->itemName=v);lines.add(addRenderableWidget(baseBox));
        EditBox loreBox=new StyledTextBox(font,12,237,fieldW,viewH,true);loreBox.setMaxLength(65535);loreBox.setValue(String.join("|",lore));loreBox.setResponder(v->{lore.clear();lore.addAll(Arrays.asList(v.split("\\|",-1)));});lines.add(addRenderableWidget(loreBox));
        active=nameBox;section=0;
        button("Применить",width-148,height-32,136,22,()->{draft.name=name;draft.itemName=itemName;draft.lore=String.join("|",lore);draft.nameFont=fonts[0];draft.loreFont=fonts[1];draft.itemNameFont=fonts[2];minecraft.setScreen(parent);});
        button("Отмена",width-90,10,78,22,this::onClose);
    }

    private EditBox field(String value,int x,int y,int w,java.util.function.Consumer<String> change){var box=new UiEditBox(font,x,y,w,22,Component.literal("Текст"));box.setMaxLength(16384);box.setValue(value);box.setResponder(change);addRenderableWidget(box);return box;}
    private void button(String text,int x,int y,int w,int h,Runnable action){addRenderableWidget(UiButton.builder(Component.literal(text),b->action.run()).bounds(x,y,w,h).build());}
    private void transform(java.util.function.UnaryOperator<String> action){
        if(active==null)return;String value=active.getValue();int cursor=active.getCursorPosition(),anchor=((com.envyworld.mixin.EditBoxAccessor)active).envyworld$highlightPos();
        int from=Math.min(cursor,anchor),to=Math.max(cursor,anchor);if(from==to){from=0;to=value.length();}
        String base=section==1?"GRAY":draft.color;
        net.minecraft.network.chat.Style[] atStart={net.minecraft.network.chat.Style.EMPTY},atEnd={net.minecraft.network.chat.Style.EMPTY};
        ItemText.parse(value.substring(0,from)+" ",base).visit((style,part)->{atStart[0]=style;return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        ItemText.parse(value.substring(0,to)+" ",base).visit((style,part)->{atEnd[0]=style;return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        String selected=ItemText.styleCode(atStart[0])+value.substring(from,to);
        String replacement=section==1?Arrays.stream(selected.split("\\|",-1)).map(action).collect(java.util.stream.Collectors.joining("|")):action.apply(selected);
        active.setValue(value.substring(0,from)+replacement+ItemText.styleCode(atEnd[0])+value.substring(to));active.moveCursorTo(from+replacement.length(),false);status="Оформление применено";
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float dt){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int x,int y,float dt){
        UiRenderer.beginFrame();bars.beginFrame();var g=new UiGraphics(original,x,y);HudSurface.fullscreen(width,height);
        UiFont.draw(g,"Название и описание  |  оформление выделенного текста",12,17,0xFFDCE6F4);
        UiFont.draw(g,"Название",12,135,6,0xFFA6B6CB);
        UiFont.draw(g,"Базовое имя предмета  |  необязательно",12,180,6,0xFFA6B6CB);
        UiFont.draw(g,"Описание  |  Enter — новая строка",12,225,6,0xFFA6B6CB);
        int px=fieldW+24,pw=width-px-12;
        UiFont.draw(g,"ПРЕДПРОСМОТР",px+6,135,6,0xFFA6B6CB);
        g.enableScissor(px,147,width-12,height-52);float yy=150;
        List<Component> preview=new ArrayList<>();preview.add(ItemText.font(ItemText.parse(name,draft.color),fonts[0]));for(String line:lore)preview.add(ItemText.font(ItemText.parse(line,"GRAY"),fonts[1]));
        for(Component component:preview)for(var line:font.split(component,Math.max(20,pw-12))){g.drawString(font,line,px+6,(int)yy,0xFFDCE6F2,false);yy+=13;}g.disableScissor();
        super.render(g,x,y,dt);int swatch=Math.min(20,(width-24)/palette.length);for(int i=0;i<palette.length;i++)UiGraphics.rounded(14+i*swatch,74,swatch-6,13,3,0xFF000000|Integer.parseInt(palette[i].substring(1),16));
        if(!status.isBlank())UiFont.draw(g,status,12,height-42,6,0xFFA6BEDC);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean d){for(int i=0;i<lines.size();i++){var line=lines.get(i);if(e.x()>=line.getX()&&e.x()<line.getRight()&&e.y()>=line.getY()&&e.y()<line.getBottom()){active=line;section=i==0?0:i==1?2:1;}}return super.mouseClicked(e,d);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){for(var box:lines)if(box.isMouseOver(x,y)&&box.mouseScrolled(x,y,dx,dy))return true;return super.mouseScrolled(x,y,dx,dy);}

    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return bars.drag(e.y())||super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return bars.release()||super.mouseReleased(e);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
