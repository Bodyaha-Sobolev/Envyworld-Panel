package com.envyworld.client.gui;

import com.envyworld.items.ItemText;
import com.envyworld.mixin.EditBoxAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A visual text field: formatting codes remain in storage, never in the editing surface. */
public final class StyledTextBox extends EditBox {
    private float offset;
    private boolean multiline;
    private int firstLine;
    private int previousCursor=-1;
    private final ScrollBars bars=new ScrollBars();
    public StyledTextBox(Font font,int x,int y,int width,int height){super(font,x,y,width,height,Component.literal("Текст"));}
    public StyledTextBox(Font font,int x,int y,int width,int height,boolean multiline){this(font,x,y,width,height);this.multiline=multiline;}
    @Override public void insertText(String text){super.insertText(multiline?text.replace("\r\n","|").replace('\n','|').replace('\r','|'):text);}
    private int lineAt(int p){return (int)getValue().substring(0,p).chars().filter(c->c=='|').count();}
    private int lineStart(int p){return getValue().lastIndexOf('|',Math.max(-1,p-1))+1;}
    private int lineEnd(int p){int end=getValue().indexOf('|',p);return end<0?getValue().length():end;}
    private int multiPosition(double x,double y){
        String value=getValue();int wanted=Math.max(0,firstLine+(int)((y-getY()-6)/14)),start=0;
        for(int i=0;i<wanted;i++){int next=value.indexOf('|',start);if(next<0){start=value.length();break;}start=next+1;}
        int end=lineEnd(start),result=start;float best=Float.MAX_VALUE,target=(float)x-getX()-30+offset;
        for(int p:boundaries())if(p>=start&&p<=end){float distance=Math.abs(advance(value.substring(start,p))-target);if(distance<=best){best=distance;result=p;}}
        return result;
    }
    private void renderMultiline(GuiGraphics g){
        bars.beginFrame();
        UiGraphics.rounded(getX(),getY(),getWidth(),getHeight(),6,MenuTheme.PALETTE.controlFill());
        UiGraphics.outline(getX(),getY(),getWidth(),getHeight(),6,isFocused()?MenuTheme.PALETTE.borderStrong():MenuTheme.PALETTE.borderPrimary());
        String raw=getValue();int cursor=getCursorPosition(),anchor=((EditBoxAccessor)(Object)this).envyworld$highlightPos();
        int caretLine=lineAt(cursor),visibleLines=Math.max(1,(getHeight()-12)/14);
        firstLine=Math.max(0,Math.min(firstLine,Math.max(0,lineAt(raw.length())+1-visibleLines)));
        if(isFocused()&&cursor!=previousCursor){if(caretLine<firstLine)firstLine=caretLine;if(caretLine>=firstLine+visibleLines)firstLine=caretLine-visibleLines+1;}previousCursor=cursor;
        float caret=advance(raw.substring(lineStart(cursor),cursor));int inner=Math.max(1,getWidth()-34);
        if(isFocused()){if(caret-offset>inner-2)offset=caret-inner+2;if(caret<offset)offset=caret;offset=Math.max(0,offset);}
        g.enableScissor(getX()+2,getY()+2,getRight()-6,getBottom()-2);
        int start=0,line=0;for(String part:raw.split("\\|",-1)){
            int yy=getY()+6+(line-firstLine)*14;
            if(line>=firstLine&&line<=firstLine+visibleLines){
                UiFont.draw(g,(line+1)+".",getX()+5,yy,6,0xFF7D8C9F);
                g.enableScissor(getX()+20,getY()+2,getRight()-6,getBottom()-2);
                int a=Math.max(start,Math.min(cursor,anchor)),b=Math.min(start+part.length(),Math.max(cursor,anchor));
                if(b>a){float ax=advance(raw.substring(start,a)),bx=advance(raw.substring(start,b));UiGraphics.rounded(getX()+22+ax-offset,yy,bx-ax,10,1,0x665E5E64);}
                UiText.component(g,ItemText.parse(part,"GRAY"),getX()+22-offset,yy,Math.max(inner,advance(part))+4,7,0xFFE1EAF5);
                if(isFocused()&&line==caretLine&&System.currentTimeMillis()/500%2==0)UiGraphics.rounded(getX()+22+caret-offset,yy,.7f,9,0,0xFFE6E6E8);
                g.disableScissor();
            }start+=part.length()+1;line++;
        }g.disableScissor();
        {var bar=new ScrollBars.Bar(getRight()-6,getY()+1,getHeight()-2,firstLine,visibleLines,line,v->firstLine=v);bars.add(bar.x(),bar.y(),bar.height(),firstLine,visibleLines,line,bar.setter());ScrollBars.drawInside(bar,getX(),getY(),getWidth(),getHeight(),6);}
    }
    private float advance(String raw){return UiText.measure(ItemText.parse(raw,"WHITE"),7);}
    private List<Integer> boundaries(){
        String value=getValue();List<Integer> points=new ArrayList<>();points.add(0);
        for(int i=0;i<value.length();){char c=value.charAt(i);if((c=='&'||c=='§')&&i+1<value.length()){
                if(i+7<value.length()&&value.charAt(i+1)=='#'&&value.substring(i+2,i+8).matches("[0-9a-fA-F]{6}")){i+=8;continue;}
                if(net.minecraft.ChatFormatting.getByCode(value.charAt(i+1))!=null){i+=2;continue;}
            }points.add(i);i+=Character.charCount(value.codePointAt(i));points.add(i);
        }return points.stream().distinct().sorted().toList();
    }
    private int position(double x){String value=getValue();float target=(float)x-getX()-6+offset;int result=0;float best=Float.MAX_VALUE;
        for(int p:boundaries()){float distance=Math.abs(advance(value.substring(0,p))-target);if(distance<=best){best=distance;result=p;}}return result;}
    @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta){
        if(!visible)return;
        if(multiline){renderMultiline(g);return;}
        UiGraphics.rounded(getX(),getY(),getWidth(),getHeight(),6,MenuTheme.PALETTE.controlFill());UiGraphics.outline(getX(),getY(),getWidth(),getHeight(),6,isFocused()?MenuTheme.PALETTE.borderStrong():MenuTheme.PALETTE.borderPrimary());
        String raw=getValue();int cursor=getCursorPosition();float caret=advance(raw.substring(0,cursor));int inner=Math.max(1,getWidth()-12);
        if(caret-offset>inner-2)offset=caret-inner+2;if(caret<offset)offset=caret;offset=Math.max(0,offset);
        float top=UiFont.centeredY(getY(),getHeight(),7);g.enableScissor(getX()+4,getY()+1,getRight()-4,getBottom()-1);
        int anchor=((EditBoxAccessor)(Object)this).envyworld$highlightPos();if(anchor!=cursor){float a=advance(raw.substring(0,Math.min(anchor,cursor))),b=advance(raw.substring(0,Math.max(anchor,cursor)));UiGraphics.rounded(getX()+6+a-offset,top,b-a,10,1,0x665E5E64);}
        UiText.component(g,ItemText.parse(raw,"WHITE"),getX()+6-offset,top,Math.max(getWidth(),advance(raw))+4,7,0xFFE1EAF5);
        if(isFocused()&&System.currentTimeMillis()/500%2==0)UiGraphics.rounded(getX()+6+caret-offset,top,.7f,9,0,0xFFE6E6E8);g.disableScissor();
    }
    @Override public void onClick(MouseButtonEvent e,boolean doubled){if(doubled){moveCursorTo(0,false);moveCursorToEnd(true);}else moveCursorTo(multiline?multiPosition(e.x(),e.y()):position(e.x()),e.hasShiftDown());}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){return multiline&&e.button()==0&&bars.press(e.x(),e.y())||super.mouseClicked(e,doubled);}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return multiline&&bars.drag(e.y())||super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return multiline&&bars.release()||super.mouseReleased(e);}
    @Override protected void onDrag(MouseButtonEvent e,double dx,double dy){moveCursorTo(multiline?multiPosition(e.x(),e.y()):position(e.x()),true);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(!multiline)return false;firstLine=Math.max(0,Math.min(Math.max(0,lineAt(getValue().length())-(getHeight()-12)/14+1),firstLine-(int)(dy*3)));return true;}
    @Override public boolean keyPressed(KeyEvent e){
        if(multiline){
            int p=getCursorPosition();
            if(e.key()==GLFW.GLFW_KEY_ENTER||e.key()==GLFW.GLFW_KEY_KP_ENTER){insertText("|");return true;}
            if(e.key()==GLFW.GLFW_KEY_UP||e.key()==GLFW.GLFW_KEY_DOWN){int direction=e.key()==GLFW.GLFW_KEY_UP?-1:1;float x=getX()+22+advance(getValue().substring(lineStart(p),p))-offset;moveCursorTo(multiPosition(x,getY()+6+(lineAt(p)-firstLine+direction)*14),e.hasShiftDown());return true;}
            if((e.modifiers()&2)==0&&(e.key()==GLFW.GLFW_KEY_HOME||e.key()==GLFW.GLFW_KEY_END)){moveCursorTo(e.key()==GLFW.GLFW_KEY_HOME?lineStart(p):lineEnd(p),e.hasShiftDown());return true;}
        }
        if((e.modifiers()&2)==0){int cursor=getCursorPosition(),anchor=((EditBoxAccessor)(Object)this).envyworld$highlightPos();var points=boundaries();
            if(e.key()==GLFW.GLFW_KEY_LEFT){int next=points.stream().filter(p->p<cursor).mapToInt(Integer::intValue).max().orElse(0);moveCursorTo(next,e.hasShiftDown());return true;}
            if(e.key()==GLFW.GLFW_KEY_RIGHT){int next=points.stream().filter(p->p>cursor).mapToInt(Integer::intValue).min().orElse(getValue().length());moveCursorTo(next,e.hasShiftDown());return true;}
            if(cursor==anchor&&(e.key()==GLFW.GLFW_KEY_BACKSPACE||e.key()==GLFW.GLFW_KEY_DELETE)){
                int a=cursor,b=cursor;if(e.key()==GLFW.GLFW_KEY_BACKSPACE)a=points.stream().filter(p->p<cursor).mapToInt(Integer::intValue).max().orElse(cursor);else b=points.stream().filter(p->p>cursor).mapToInt(Integer::intValue).min().orElse(cursor);
                moveCursorTo(a,false);moveCursorTo(b,true);insertText("");return true;
            }
        }return super.keyPressed(e);
    }
}
