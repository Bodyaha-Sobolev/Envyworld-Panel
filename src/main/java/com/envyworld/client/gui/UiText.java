package com.envyworld.client.gui;

import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class UiText {
    private UiText() {}
    private static com.envyworld.graphics.util.MsdfFontRenderer font(Style style) { return style.isBold() ? com.envyworld.graphics.misc.FontRegistry.boldText : com.envyworld.graphics.misc.FontRegistry.regularText; }
    private static boolean nativeFont(Style style){return !style.getFont().equals(Style.EMPTY.getFont());}
    public static boolean nativeNeeded(Component component){boolean[] needed={false};component.visit((style,value)->{needed[0]|=nativeFont(style)||UiFont.needsNative(value);return Optional.empty();},Style.EMPTY);return needed[0];}
    private static float measure(String text,float size,Style style){
        String clean=UiFont.sanitize(text);if(nativeFont(style))return net.minecraft.client.Minecraft.getInstance().font.width(Component.literal(clean).withStyle(style))*UiFont.nativeScale(size);
        float width=0;for(int i=0;i<clean.length();){int next=i+Character.charCount(clean.codePointAt(i));boolean nativeRun=UiFont.needsNative(clean.substring(i,next));
            while(next<clean.length()){int end=next+Character.charCount(clean.codePointAt(next));if(UiFont.needsNative(clean.substring(next,end))!=nativeRun)break;next=end;}
            String run=clean.substring(i,next);width+=nativeRun?net.minecraft.client.Minecraft.getInstance().font.width(Component.literal(run).withStyle(style))*UiFont.nativeScale(size):font(style).process3(run,size);i=next;
        }return width;
    }
    public static float measure(Component text,float size){float[] width={0};text.visit((style,value)->{width[0]+=measure(value,size,style);return Optional.empty();},Style.EMPTY);return width[0];}
    private static void run(com.envyworld.graphics.util.GuiDrawApi draw, org.joml.Matrix4f matrix, String text,Style style,float x,float y,float size,int color) {
        String display=UiFont.sanitize(text);
        if(style.isObfuscated()){StringBuilder b=new StringBuilder();long phase=System.currentTimeMillis()/80;for(int i=0;i<display.length();i++)b.append(Character.isWhitespace(display.charAt(i))?' ':(char)('A'+Math.floorMod(phase+i*7,26)));display=b.toString();}
        var transform=new org.joml.Matrix4f(matrix).translate(x,y,0);if(style.isItalic())transform.mul(new org.joml.Matrix4f().m10(-.2f));
        font(style).process2(transform,draw,display,0,0,size,color);
        float w=measure(text,size,style);
        if(style.isUnderlined())draw.drawRoundedRectangle(matrix,x,y+size*.93f,w,Math.max(.45f,size*.065f),0,color);
        if(style.isStrikethrough())draw.drawRoundedRectangle(matrix,x,y+size*.37f,w,Math.max(.45f,size*.065f),0,color);
    }
    public static java.util.List<Component> wrap(Component source, float width, float size) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        net.minecraft.network.chat.MutableComponent[] line = {Component.empty()};
        float[] used = {0};
        source.visit((style, value) -> {
            StringBuilder run = new StringBuilder();
            for (int i = 0; i < value.length();) {
                int cp = value.codePointAt(i); String glyph = new String(Character.toChars(cp)); i += Character.charCount(cp);
                float advance = measure(glyph, size, style);
                if (cp == '\n' || used[0] + advance > width && used[0] > 0) {
                    line[0].append(Component.literal(run.toString()).withStyle(style)); run.setLength(0);
                    lines.add(line[0]); line[0] = Component.empty(); used[0] = 0;
                    if (cp == '\n') continue;
                }
                run.append(glyph); used[0] += advance;
            }
            line[0].append(Component.literal(run.toString()).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        lines.add(line[0]); return lines;
    }
    public static void overlayComponent(com.envyworld.graphics.util.GuiDrawApi draw, org.joml.Matrix4f matrix, Component text, float x, float y, float size, int fallback) {
        float[] advance = {0};
        text.visit((style, value) -> {
            String clean = UiFont.sanitize(value);
            int color = style.getColor() == null ? fallback : 0xFF000000 | style.getColor().getValue();
            run(draw,matrix,clean,style,x+advance[0],y,size,color);
            advance[0] += measure(clean, size, style); return Optional.empty();
        }, Style.EMPTY);
    }
    public static void component(GuiGraphics graphics,Component text,float x,float y,float width,float size,int fallback){
        float[] used={0};
        text.visit((style,value)->{
            String clean=UiFont.sanitize(value);
            for(int i=0;i<clean.length();){
                int next=i+Character.charCount(clean.codePointAt(i));boolean nativeRun=nativeFont(style)||UiFont.needsNative(clean.substring(i,next));
                while(next<clean.length()){int end=next+Character.charCount(clean.codePointAt(next));if((nativeFont(style)||UiFont.needsNative(clean.substring(next,end)))!=nativeRun)break;next=end;}
                String part=clean.substring(i,next);while(!part.isEmpty()&&used[0]+measure(part,size,style)>width)part=part.substring(0,part.offsetByCodePoints(part.length(),-1));
                float left=x+used[0];int color=style.getColor()==null?fallback:0xFF000000|style.getColor().getValue();
                if(nativeRun){graphics.enableScissor((int)x,(int)y-2,(int)(x+width+1),(int)(y+size+6));graphics.pose().pushMatrix();try{graphics.pose().translate(Math.round(left),Math.round(y+(size-9*UiFont.nativeScale(size))/2));graphics.pose().scale(UiFont.nativeScale(size),UiFont.nativeScale(size));graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,Component.literal(part).withStyle(style),0,0,color,false);}finally{graphics.pose().popMatrix();graphics.disableScissor();}}
                else{String display=part;UiRenderer.submit((draw,matrix)->run(draw,matrix,display,style,left,y,size,color));}
                used[0]+=measure(part,size,style);if(part.length()<next-i)return Optional.of(Boolean.TRUE);i=next;
            }return used[0]>=width-2?Optional.of(Boolean.TRUE):Optional.empty();
        },Style.EMPTY);
    }
}
