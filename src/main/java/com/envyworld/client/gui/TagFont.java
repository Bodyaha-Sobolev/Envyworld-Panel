package com.envyworld.client.gui;

import com.envyworld.graphics.misc.MsdfFontBuilder;
import com.envyworld.graphics.util.MsdfFontRenderer;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Font atlas for player nameplates. */
public final class TagFont {
    private static MsdfFontRenderer font;
    private record Glyph(String value,float advance){}
    private static final java.util.Map<String,java.util.List<Glyph>> layouts=new java.util.HashMap<>();
    private TagFont(){}
    private static MsdfFontRenderer font(){if(font==null){font=new MsdfFontBuilder("/assets/envyworld/nametags").process4("font.json").process("font.png").process2("Nametag font").getMsdfFontRenderer();font.smoothing(.3535534f);}return font;}
    public static float measure(String text,float size){return UiFont.needsNative(text)?UiFont.measure(text,size):font().process3(UiFont.sanitize(text),size)*1.05f;}
    public static float measure(Component text,float size){if(UiText.nativeNeeded(text))return UiText.measure(text,size);float[] width={0};text.visit((style,value)->{width[0]+=measure(value,size);return Optional.empty();},Style.EMPTY);return width[0];}
    public static float top(float y,float height,float size){return y+(height-1.193359375f*size)/2;}
    public static void draw(GuiGraphics graphics,String text,float x,float y,float size,int color){
        if(UiFont.needsNative(text)){UiFont.draw(graphics,text,x,y,size,color);return;}
        String display=UiFont.sanitize(text);var glyphs=layouts.get(display);
        if(glyphs==null){if(layouts.size()>=4096)layouts.clear();var result=new java.util.ArrayList<Glyph>();for(int i=0;i<display.length();i++){String glyph=display.substring(i,i+1);result.add(new Glyph(glyph,font().process3(glyph,1)+.025f));}glyphs=java.util.List.copyOf(result);layouts.put(display,glyphs);}
        var line=glyphs;UiRenderer.submit((draw,matrix)->{float cursor=x;for(var glyph:line){font().process7(matrix,draw,glyph.value(),cursor,y,size,.05f,0,color,color);cursor+=glyph.advance()*size;}});
    }
    public static void component(GuiGraphics graphics,Component text,float x,float y,float size,int fallback){
        if(UiText.nativeNeeded(text)){UiText.component(graphics,text,x,y,measure(text,size)+2,size,fallback);return;}
        float[] cursor={x};text.visit((style,value)->{int color=style.getColor()==null?fallback:0xFF000000|style.getColor().getValue();draw(graphics,value,cursor[0],y,size,color);cursor[0]+=measure(value,size);return Optional.empty();},Style.EMPTY);
    }
    public static void close(){layouts.clear();if(font!=null){font.close();font=null;}}
}
