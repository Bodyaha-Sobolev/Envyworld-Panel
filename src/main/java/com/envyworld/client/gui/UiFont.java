package com.envyworld.client.gui;

import com.envyworld.graphics.misc.FontRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** Thin call adapter to the unmodified Envyworld SF Pro MSDF renderer and measurements. */
public final class UiFont {
    public static final float SIZE = 7;
    public static final float SIZE_SMALL = 6;
    private UiFont() {}

    public static void draw(GuiGraphics ignored, String text, float x, float y, int color) {
        draw(ignored, text, x, y, SIZE, color);
    }

    public static void draw(GuiGraphics ignored, String text, float x, float y, float size, int color) {
        if (text == null || text.isEmpty() || (color >>> 24) == 0) return;
        String display = sanitize(text);
        if(display.codePoints().anyMatch(UiFont::nativeGlyph)){
            float cursor=x;
            for(int i=0;i<display.length();){int cp=display.codePointAt(i),next=i+Character.charCount(cp);boolean nativeRun=nativeGlyph(cp);
                while(next<display.length()&&nativeGlyph(display.codePointAt(next))==nativeRun)next+=Character.charCount(display.codePointAt(next));
                String run=display.substring(i,next);
                if(nativeRun){if(ignored!=null){ignored.pose().pushMatrix();try{ignored.pose().translate(Math.round(cursor),Math.round(y+(size-9*nativeScale(size))/2));ignored.pose().scale(nativeScale(size),nativeScale(size));ignored.drawString(net.minecraft.client.Minecraft.getInstance().font,run,0,0,color,false);}finally{ignored.pose().popMatrix();}}}
                else{float left=cursor;UiRenderer.submit((draw,matrix)->FontRegistry.regularText.process2(matrix,draw,run,left,y,size,color));}
                cursor+=measure(run,size);i=next;
            }return;
        }
        UiRenderer.submit((draw, matrix) -> FontRegistry.regularText.process2(matrix, draw, display, x, y, size, color));
    }
    private static final java.util.Set<Integer> ATLAS=loadGlyphs();
    private static java.util.Set<Integer> loadGlyphs(){var set=new java.util.HashSet<Integer>();try(var in=UiFont.class.getResourceAsStream("/assets/envyworld/renderer/fonts/msdf/sf_pro_display_medium.json")){var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();for(var glyph:json.getAsJsonArray("glyphs"))set.add(glyph.getAsJsonObject().get("unicode").getAsInt());}catch(Exception ignored){}return java.util.Set.copyOf(set);}
    public static float nativeScale(float size){return Math.max(1,Math.round(size/9f));}
    private static boolean nativeGlyph(int cp){return cp!='\n'&&cp!='\r'&&!ATLAS.contains(cp);}
    public static boolean needsNative(String text){return text!=null&&sanitize(text).codePoints().anyMatch(UiFont::nativeGlyph);}

    public static float measure(String text, float size) {
        if(text==null)return 0;String value=sanitize(text);if(value.codePoints().noneMatch(UiFont::nativeGlyph))return FontRegistry.regularText.process3(value,size);
        float width=0;for(int i=0;i<value.length();){int cp=value.codePointAt(i),next=i+Character.charCount(cp);boolean nativeRun=nativeGlyph(cp);while(next<value.length()&&nativeGlyph(value.codePointAt(next))==nativeRun)next+=Character.charCount(value.codePointAt(next));String run=value.substring(i,next);width+=nativeRun?net.minecraft.client.Minecraft.getInstance().font.width(run)*nativeScale(size):FontRegistry.regularText.process3(run,size);i=next;}return width;
    }

    public static int width(String text) { return width(text, SIZE); }
    public static int width(String text, float size) { return (int) Math.ceil(measure(text, size)); }
    public static int lineHeight() { return (int) Math.ceil(FontRegistry.regularText.process(" ", SIZE)); }
    public static void drawFaded(GuiGraphics graphics,String text,float x,float y,float width,int color){
        String display=sanitize(text);float used=0;int end=0;
        while(end<display.length()){int cp=display.codePointAt(end);float advance=measure(new String(Character.toChars(cp)),SIZE);if(used+advance>width)break;used+=advance;end+=Character.charCount(cp);}
        if(end==display.length()){draw(graphics,display,x,y,color);return;}
        float cursor=0;int prefixEnd=0;
        while(prefixEnd<end){int next=prefixEnd+Character.charCount(display.codePointAt(prefixEnd));float advance=measure(display.substring(prefixEnd,next),SIZE);if(cursor+advance>width-24)break;cursor+=advance;prefixEnd=next;}
        draw(graphics,display.substring(0,prefixEnd),x,y,color);
        for(int i=prefixEnd;i<end;){int next=i+Character.charCount(display.codePointAt(i));String glyph=display.substring(i,next);float opacity=Math.max(0,Math.min(1,(width-cursor)/24));int faded=(Math.round((color>>>24)*opacity)<<24)|(color&0xFFFFFF);draw(graphics,glyph,x+cursor,y,faded);cursor+=measure(glyph,SIZE);i=next;}
    }
    /** Optical center for SF Pro's visible mixed-case glyphs, excluding line spacing. */
    public static float centeredY(float top, float height, float size) { return top + height * .5f - size * .67f; }

    public static List<String> wrap(String text, float width) { return wrap(text, width, SIZE); }

    public static List<String> wrap(String text, float width, float size) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;
        for (String paragraph : text.split("\n", -1)) {
            String line = "";
            for (String word : paragraph.split(" ")) {
                if (measure(word, size) > width) {
                    if (!line.isEmpty()) { result.add(line); line = ""; }
                    for (int offset = 0; offset < word.length();) {
                        int codepoint = word.codePointAt(offset);
                        String next = line + new String(Character.toChars(codepoint));
                        if (!line.isEmpty() && measure(next, size) > width) { result.add(line); line = new String(Character.toChars(codepoint)); }
                        else line = next;
                        offset += Character.charCount(codepoint);
                    }
                    continue;
                }
                String next = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && measure(next, size) > width) {
                    result.add(line);
                    line = word;
                } else line = next;
            }
            result.add(line);
        }
        return result;
    }

    // The original SF Pro atlas contains Latin and Cyrillic; UI symbols use available glyphs.
    public static String sanitize(String text) {
        if (text == null) return "";
        return text.replace(" · ","  |  ").replace('–','-');
    }
}
