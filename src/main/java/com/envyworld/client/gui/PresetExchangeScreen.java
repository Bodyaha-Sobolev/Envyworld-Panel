package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;
import com.envyworld.items.*;
import com.envyworld.bind.Binding;
import java.nio.file.*;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PresetExchangeScreen extends Screen implements MenuScreen {
    private final Screen parent;
    private PresetExchange.Pack export;
    private EditBox input;
    private String source="",status="",file="";
    private boolean showingSource;
    public PresetExchangeScreen(Screen parent,CustomItemPreset item,Binding macro){super(Component.literal("Импорт предметов и макросов"));this.parent=parent;export=PresetExchange.selection(EnvyWorld.config(),item,macro);if(export.items.isEmpty()&&export.macros.isEmpty())export=PresetExchange.all(EnvyWorld.config());}
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(UiButton.builder(Component.literal(text),b->{try{action.run();}catch(Exception e){status=e.getMessage()==null?"Ошибка обмена":e.getMessage();}}).bounds(x,y,w,22).build());}
    @Override protected void init(){
        int x=20,w=width-40;
        input=new UiEditBox(font,x,164,w,24,Component.literal("Вставьте JSON или путь к файлу через Ctrl+V"));input.setMaxLength(8_000_000);input.setResponder(v->{if(!showingSource)source=v;});showSource();addRenderableWidget(input);
        button("Из буфера",x,198,130,this::paste);
        button("Импортировать",x+138,198,150,()->{var pack=read();status=PresetExchange.merge(EnvyWorld.config(),pack);EnvyWorld.saveConfig();});
        button("Из инвентаря",x+306,198,140,()->minecraft.setScreen(new ItemImportScreen(parent,p->{})));
        button("Назад",width-110,height-34,90,this::onClose);
    }
    private void paste(){
        source="";try{var path=com.envyworld.client.ClipboardFiles.first();if(path!=null)source=path.toAbsolutePath().toString();}catch(Exception|LinkageError ignored){}
        if(source.isBlank())source=minecraft.keyboardHandler.getClipboard();
        showSource();status=source.isBlank()?"В буфере нет текста или файла":"Данные из буфера загружены";
    }
    private void showSource(){showingSource=true;try{String text=source.stripLeading();input.setValue(text.startsWith("{")||text.startsWith("```")?"Пакет JSON  |  "+source.length()+" символов  |  готов к импорту":source);}finally{showingSource=false;}}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent e){if(e.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_V&&(e.modifiers()&2)!=0){paste();return true;}return super.keyPressed(e);}
    private PresetExchange.Pack read(){String value=source.trim();if(!value.startsWith("{")&&!value.startsWith("```"))try{Path path=Path.of(value.replaceAll("^\"|\"$",""));if(Files.size(path)>8_000_000)throw new IllegalArgumentException("Файл больше 8 МБ");value=Files.readString(path);}catch(Exception e){throw new IllegalArgumentException("Не удалось открыть файл: "+e.getMessage(),e);}return PresetExchange.decode(value);}
    @Override public void onFilesDrop(List<Path> paths){if(paths.isEmpty())return;source=paths.getFirst().toAbsolutePath().toString();showSource();status="Файл выбран  |  нажмите «Импортировать»";}
    @Override public void onClose(){if(parent instanceof EnvyScreen menu)menu.refreshSavedSelection();minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float dt){minecraft.gui.renderDeferredSubtitles();}
    @Override public void render(GuiGraphics original,int mx,int my,float dt){
        UiRenderer.beginFrame();var g=new UiGraphics(original,mx,my);HudSurface.fullscreen(width,height);
        UiFont.draw(g,"Импорт предметов и макросов",20,20,0xFFE3EAF2);
        UiFont.draw(g,"Импорт  |  сообщение JSON, файл .json или путь к нему",20,140,0xFFB5C6DB);
        int y=238;for(String line:UiFont.wrap(status,width-40)){UiFont.draw(g,line,20,y,0xFFBBCFE5);y+=12;}
        if(!file.isBlank()){y+=12;for(String line:UiFont.wrap(file,width-40)){UiFont.draw(g,line,20,y,6,0xFF91A1B5);y+=10;}}
        super.render(g,mx,my,dt);
    }
}
