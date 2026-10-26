package com.envyworld.client.gui;

import com.envyworld.items.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.*;

/** Inline rich text editing. Every change updates only the current item draft. */
final class ItemTextForm {
    @FunctionalInterface interface PickerOpener {void open(AbstractWidget anchor,String initial,Consumer<String> apply,Runnable cancel);}
    private record Selection(Consumer<UnaryOperator<String>> apply,Runnable cancel){}
    private final CustomItemPreset draft;
    private final List<EditBox> fields=new ArrayList<>();
    private final List<String> captions=new ArrayList<>();
    static final class Colors {String start="#55AAFF",end="#FF55AA";}
    private final Colors colors;
    private EditBox selected;
    private int index;
    ItemTextForm(CustomItemPreset draft,Colors colors,int x,int y,int width,BiConsumer<AbstractWidget,Integer> add,PickerOpener picker){
        this.draft=draft;
        this.colors=colors;
        String[] codes={"&l","&o","&n","&m","&k","&r"};
        String[] icons={"format:bold","format:italic","format:underline","format:strike","format:magic","format:reset"};
        String[] hints={"Жирный","Курсив","Подчёркнутый","Зачёркнутый","Случайные символы","Сброс оформления"};
        int buttonW=Math.min(30,(width-12)/8);
        for(int i=0;i<codes.length;i++){
            final String code=codes[i];
            var b=UiButton.builder(Component.literal(icons[i]),v->transform(s->ItemText.format(s,code))).bounds(x+i*(buttonW+3),y,buttonW,22).tooltip(Tooltip.create(Component.literal(hints[i]))).build();
            add.accept(b,0);
        }
        var c=Minecraft.getInstance();
        int fontX=x+6*(buttonW+3);
        if(width-(fontX-x)>55)add.accept(UiButton.builder(Component.literal("Шрифт"),v->{
            var parent=c.screen;int target=index;
            c.setScreen(new ChoiceScreen(parent,"Шрифт",List.of(new ChoiceScreen.Choice("minecraft:default","Minecraft"),new ChoiceScreen.Choice("minecraft:uniform","Unicode"),new ChoiceScreen.Choice("minecraft:alt","Альтернативный")),value->{if(target==0)draft.nameFont=value;else if(target==1)draft.itemNameFont=value;else draft.loreFont=value;}));
        }).bounds(fontX,y,width-(fontX-x),22).build(),0);
        int colorWidth=(width-8)/2;
        add.accept(UiButton.builder(Component.literal("Цвет "+colors.start),v->{
            var selection=capture();String original=colors.start;
            picker.open(v,original,value->{colors.start=value;v.setMessage(Component.literal("Цвет "+value));selection.apply().accept(text->ItemText.recolor(text,value));},()->{colors.start=original;v.setMessage(Component.literal("Цвет "+original));selection.cancel().run();});
        }).bounds(x,y+32,colorWidth,20).build(),32);
        add.accept(UiButton.builder(Component.literal("Цвет "+colors.end),v->{
            var selection=capture();String original=colors.end;
            picker.open(v,original,value->{colors.end=value;v.setMessage(Component.literal("Цвет "+value));selection.apply().accept(text->ItemText.gradient(text,colors.start,value));},()->{colors.end=original;v.setMessage(Component.literal("Цвет "+original));selection.cancel().run();});
        }).bounds(x+colorWidth+8,y+32,colorWidth,20).build(),32);
        add.accept(UiButton.builder(Component.literal("Применить цвет"),v->{if(hex(colors.start))transform(s->ItemText.recolor(s,colors.start));}).bounds(x,y+60,(width-8)/2,20).build(),60);
        add.accept(UiButton.builder(Component.literal("Градиент"),v->{if(hex(colors.start)&&hex(colors.end))transform(s->ItemText.gradient(s,colors.start,colors.end));}).bounds(x+(width+8)/2,y+60,(width-8)/2,20).build(),60);
        String[] values={draft.name,draft.itemName,draft.lore};
        String[] labels={"Название","Базовое имя (необязательно)","Описание. Enter: новая строка"};
        int[] offsets={104,148,192};
        for(int i=0;i<3;i++){
            final int target=i;
            EditBox box=new StyledTextBox(c.font,x,y+offsets[i],width,i==2?110:22,i==2);
            box.setMaxLength(i==2?65535:32767);box.setValue(values[i]);
            box.setResponder(v->{if(target==0)draft.name=v;else if(target==1)draft.itemName=v;else draft.lore=v;});
            fields.add(box);captions.add(labels[i]);add.accept(box,offsets[i]);
        }
        selected=fields.getFirst();
    }
    private static boolean hex(String s){return s.matches("#[0-9a-fA-F]{6}");}
    void press(double x,double y){for(int i=0;i<fields.size();i++)if(fields.get(i).isMouseOver(x,y)){selected=fields.get(i);index=i;}}
    void draw(GuiGraphics g,int x,int y){for(int i=0;i<fields.size();i++)if(fields.get(i).visible)UiFont.draw(g,captions.get(i),x,fields.get(i).getY()-11,6,0xFFADBDCF);}
    private void transform(UnaryOperator<String> operation){capture().apply().accept(operation);}
    private Selection capture(){
        if(selected==null)return new Selection(op->{},()->{});
        EditBox field=selected;int target=index;
        String value=field.getValue();int cursor=field.getCursorPosition(),anchor=((com.envyworld.mixin.EditBoxAccessor)field).envyworld$highlightPos();
        int left=Math.min(cursor,anchor),right=Math.max(cursor,anchor);if(left==right){left=0;right=value.length();}
        final int a=left,b=right;
        final Style[] first={Style.EMPTY},last={Style.EMPTY};String base=target==2?"GRAY":draft.color;
        ItemText.parse(value.substring(0,a)+" ",base).visit((style,part)->{first[0]=style;return Optional.empty();},Style.EMPTY);
        ItemText.parse(value.substring(0,b)+" ",base).visit((style,part)->{last[0]=style;return Optional.empty();},Style.EMPTY);
        String part=ItemText.styleCode(first[0])+value.substring(a,b);
        return new Selection(operation->{
            String replacement=target==2?Arrays.stream(part.split("\\|",-1)).map(operation).collect(java.util.stream.Collectors.joining("|")):operation.apply(part);
            field.setValue(value.substring(0,a)+replacement+ItemText.styleCode(last[0])+value.substring(b));field.moveCursorTo(a,false);field.moveCursorTo(a+replacement.length(),true);
        },()->{field.setValue(value);field.moveCursorTo(anchor,false);field.moveCursorTo(cursor,true);});
    }
}
