package com.envyworld.client.gui;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** An anchored editor popup. It never changes Minecraft's current Screen. */
public final class ColorPickerPopup {
    private static final int WIDTH=104,HEIGHT=154,PAD=6,SV_Y=6,SV_H=92,HUE_Y=105,FIELD_Y=116;
    private static final String[] FORMATS={"HEX","RGB","HSV","HSL"};
    private final Consumer<String> accept;private final Runnable cancel;
    private final UiMotion opening=new UiMotion(0);
    private final UiMotion markerS,markerV,markerH;
    private final int x,y;private final EditBox input;
    private float hue,saturation,value,progress;private int drag,format,original;private boolean closed,syncing;
    public ColorPickerPopup(AbstractWidget anchor,String initial,Consumer<String> accept,Runnable cancel,int screenW,int screenH){
        this.accept=accept;this.cancel=cancel;
        int beside=anchor.getRight()+5;if(beside+WIDTH>screenW-5)beside=anchor.getX()-WIDTH-5;
        x=Math.clamp(beside,5,Math.max(5,screenW-WIDTH-5));y=Math.clamp(anchor.getY(),5,Math.max(5,screenH-HEIGHT-5));
        int color=0x55AAFF;try{color=Integer.parseInt(initial.replace("#",""),16);}catch(RuntimeException ignored){}setRgb(color);original=color;markerS=new UiMotion(saturation);markerV=new UiMotion(value);markerH=new UiMotion(hue);
        input=new UiEditBox(Minecraft.getInstance().font,x+39,y+FIELD_Y,WIDTH-45,17,Component.literal("HEX / RGB"));
        input.setMaxLength(32);sync();input.setResponder(this::typed);
    }
    private int color(){return java.awt.Color.HSBtoRGB(hue,saturation,value)|0xFF000000;}
    private String hex(){return String.format(Locale.ROOT,"#%06X",color()&0xFFFFFF);}
    private void setRgb(int color){float[] hsv=java.awt.Color.RGBtoHSB(color>>16&255,color>>8&255,color&255,null);hue=hsv[0];saturation=hsv[1];value=hsv[2];}
    private String formatted(){
        int c=color();if(format==0)return hex();if(format==1)return (c>>16&255)+", "+(c>>8&255)+", "+(c&255);
        float light=value*(1-saturation/2),sl=light==0||light==1?0:(value-light)/Math.min(light,1-light);
        return Math.round(hue*360)+", "+Math.round((format==2?saturation:sl)*100)+"%, "+Math.round((format==2?value:light)*100)+"%";
    }
    private void sync(){syncing=true;input.setValue(formatted());syncing=false;}
    private void typed(String text){
        if(syncing)return;
        try{
            if(format==0){if(!text.matches("#[0-9a-fA-F]{6}"))return;setRgb(Integer.parseInt(text.substring(1),16));}
            else{
                String[] parts=text.replace("%", "").split("\\s*,\\s*");if(parts.length!=3)return;
                float a=Float.parseFloat(parts[0]),b=Float.parseFloat(parts[1]),c=Float.parseFloat(parts[2]);if(!Float.isFinite(a)||!Float.isFinite(b)||!Float.isFinite(c))return;
                if(format==1){if(a<0||a>255||b<0||b>255||c<0||c>255)return;setRgb(Math.round(a)<<16|Math.round(b)<<8|Math.round(c));}
                else{if(a<0||a>360||b<0||b>100||c<0||c>100)return;hue=a/360;b/=100;c/=100;if(format==2){saturation=b;value=c;}else{value=c+b*Math.min(c,1-c);saturation=value==0?0:2*(1-c/value);}}
            }
            accept.accept(hex());
        }catch(NumberFormatException ignored){}
    }
    public boolean finished(){return closed&&progress<=.01f;}
    public void close(boolean revert){if(closed)return;closed=true;drag=0;input.setFocused(false);if(revert)cancel.run();}
    private int alpha(int color){return color;}
    private void gradient(float xx,float yy,float w,float h,int tl,int tr,int br,int bl){int a=alpha(tl),b=alpha(tr),c=alpha(br),d=alpha(bl);UiRenderer.submit((draw,matrix)->draw.drawRoundedRectangleGradient(matrix,xx,yy,w,h,h<=3?0:4,d,c,b,a));}
    public void render(GuiGraphics g,int mx,int my,float dt){
        progress=opening.update(closed?0:1);if(progress<=.01f)return;
        UiRenderer.overlayCommands(()->{
            var palette=MenuTheme.PALETTE;
            // Opaque popup: do not recapture and blur the already rendered menu.
            g.enableScissor(x,y,x+WIDTH,y+Math.max(1,Math.round(HEIGHT*progress)));
            UiGraphics.rounded(x,y,WIDTH,HEIGHT,10,alpha(palette.panelBackground()));UiGraphics.outline(x,y,WIDTH,HEIGHT,10,alpha(palette.borderPrimary()));
            int left=x+PAD,top=y+SV_Y,svWidth=WIDTH-PAD*2,hueColor=java.awt.Color.HSBtoRGB(hue,1,1);
            gradient(left,top,svWidth,SV_H,0xFFFFFFFF,hueColor,hueColor,0xFFFFFFFF);
            gradient(left,top,svWidth,SV_H,0,0,0xFF000000,0xFF000000);
            float cx=left+markerS.update(saturation)*svWidth,cy=top+(1-markerV.update(value))*SV_H;
            UiGraphics.rounded(cx-3,cy-3,6,6,6,alpha(0xFFFFFFFF));UiGraphics.rounded(cx-2,cy-2,4,4,4,alpha(color()));
            for(int i=0;i<6;i++){int a=java.awt.Color.HSBtoRGB(i/6f,1,1),b=java.awt.Color.HSBtoRGB((i+1)/6f,1,1);gradient(left+i*svWidth/6f,y+HUE_Y,svWidth/6f+.5f,3,a,b,b,a);}
            float hx=left+markerH.update(hue)*svWidth;UiGraphics.rounded(hx-2.5f,y+HUE_Y-1,5,5,5,0xFFF5F5F5);UiGraphics.rounded(hx-1.5f,y+HUE_Y,3,3,3,java.awt.Color.HSBtoRGB(hue,1,1));
            UiGraphics.rounded(x+PAD,y+FIELD_Y,21,17,3,alpha(palette.backgroundControl()));
            UiFont.draw(g,FORMATS[format],x+PAD+1,UiFont.centeredY(y+FIELD_Y,17,5.5f),5.5f,alpha(palette.textSecondary()));
            UiIcons.chevron(x+27,y+FIELD_Y+8,false,0xFF909094);
            UiGraphics.rounded(x+30,y+FIELD_Y+5,6,6,2,color());
            input.render(g,mx,my,dt);
            // Previous/current swatches and compact copy/paste controls.
            UiGraphics.rounded(x+6,y+138,10,10,2,0xFF000000|original);UiGraphics.rounded(x+20,y+138,10,10,2,color());
            UiGraphics.outline(x+6,y+138,10,10,2,0x40FFFFFF);UiGraphics.outline(x+20,y+138,10,10,2,0x40FFFFFF);
            UiGraphics.outline(x+67,y+139,7,7,1,0xFFA0A0A4);UiGraphics.rounded(x+65,y+137,7,7,1,0xFF101012);UiGraphics.outline(x+65,y+137,7,7,1,0xFFDDDDDF);
            UiGraphics.outline(x+85,y+138,8,9,1,0xFFDDDDDF);UiGraphics.rounded(x+87,y+136,4,3,1,0xFFDDDDDF);
            g.disableScissor();
        });
    }
    private boolean contains(double mx,double my){return mx>=x&&mx<x+WIDTH&&my>=y&&my<y+HEIGHT;}
    public boolean press(MouseButtonEvent event,boolean doubled){
        if(closed)return true;if(!contains(event.x(),event.y())){close(false);return true;}if(event.button()!=0)return true;
        int left=x+PAD,svWidth=WIDTH-PAD*2;double mx=event.x(),my=event.y();
        if(mx>=left&&mx<=left+svWidth&&my>=y+SV_Y&&my<=y+SV_Y+SV_H){input.setFocused(false);drag=1;update(mx,my);return true;}
        if(mx>=left-3&&mx<=left+svWidth+3&&my>=y+HUE_Y-4&&my<=y+HUE_Y+8){input.setFocused(false);drag=2;update(mx,my);return true;}
        if(mx>=left&&mx<x+29&&my>=y+FIELD_Y&&my<y+FIELD_Y+18){format=(format+1)%FORMATS.length;sync();input.setFocused(false);return true;}
        if(my>=y+135&&my<y+151){
            if(mx<x+32){setRgb(mx<x+18?original:color());sync();accept.accept(hex());}
            else if(mx>=x+60&&mx<x+79)Minecraft.getInstance().keyboardHandler.setClipboard(formatted());
            else if(mx>=x+80){String text=Minecraft.getInstance().keyboardHandler.getClipboard().strip();typed(text);sync();}
            return true;
        }
        input.setFocused(input.isMouseOver(mx,my));if(input.isFocused())input.mouseClicked(event,doubled);return true;
    }
    private void update(double mx,double my){
        if(drag==1){saturation=Math.clamp((float)(mx-x-PAD)/(WIDTH-PAD*2),0,1);value=1-Math.clamp((float)(my-y-SV_Y)/SV_H,0,1);}
        else if(drag==2)hue=Math.clamp((float)(mx-x-PAD)/(WIDTH-PAD*2),0,1);else return;sync();accept.accept(hex());
    }
    public boolean drag(MouseButtonEvent event,double dx,double dy){if(!closed){if(drag!=0)update(event.x(),event.y());else if(input.isFocused())input.mouseDragged(event,dx,dy);}return true;}
    public boolean release(MouseButtonEvent event){drag=0;input.mouseReleased(event);return true;}
    public boolean key(KeyEvent event){if(event.key()==GLFW.GLFW_KEY_ESCAPE){close(true);return true;}if(event.key()==GLFW.GLFW_KEY_ENTER){close(false);return true;}if(!closed&&input.isFocused())input.keyPressed(event);return true;}
    public boolean character(CharacterEvent event){if(!closed&&input.isFocused())input.charTyped(event);return true;}
}
