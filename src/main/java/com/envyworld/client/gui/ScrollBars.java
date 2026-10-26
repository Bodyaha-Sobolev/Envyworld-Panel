package com.envyworld.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Shared draggable scrollbar geometry, with thumb grab offset and track paging. */
public final class ScrollBars {
    public static final int WIDTH=5;
    /** Both shapes use the same bounds and their own symmetric end caps. */
    public static void drawInside(Bar bar,int x,int y,int width,int height,float radius){
        UiRenderer.submit((draw,matrix)->{
            // Envyworld stores radii/border thickness in physical pixels, while
            // positions are transformed from GUI coordinates by the matrix.
            float scale=Math.abs(matrix.m00()),inset=.75f/scale;
            float left=bar.x(),top=y+inset,bottom=y+height-inset;
            float stripWidth=x+width-inset-left,cap=stripWidth*scale/2f;
            draw.drawRoundedRectangle(matrix,left,top,stripWidth,bottom-top,cap,0x18FFFFFF);
            if(bar.maximum()>0){
                float thumbTop=top+(bottom-top-bar.thumbHeight())*bar.value()/Math.max(1,bar.maximum());
                draw.drawRoundedRectangle(matrix,left,thumbTop,stripWidth,bar.thumbHeight(),cap,0xA6B8B8B8);
            }
        });
    }
    public record Bar(int x, int y, int height, int value, int visible, int total, IntConsumer setter) {
        public int maximum() { return Math.max(0, total - visible); }
        public int thumbHeight() { return Math.min(height, Math.max(12, (int) ((long) height * visible / Math.max(1, total)))); }
        public int thumbY() { return y + (int) ((long) (height - thumbHeight()) * value / Math.max(1, maximum())); }
        public int at(double mouseY, double grab) {
            return Math.max(0, Math.min(maximum(), (int) Math.round((mouseY - y - grab) * maximum() / Math.max(1, height - thumbHeight()))));
        }
    }
    private final List<Bar> bars = new ArrayList<>();
    private Bar dragging;
    private double grab;
    public void beginFrame() { bars.clear(); }
    public void add(int x, int y, int height, int value, int visible, int total, IntConsumer setter) {
        bars.add(new Bar(x, y, height, value, visible, total, setter));
    }
    public boolean press(double x, double y) {
        for (int i = bars.size() - 1; i >= 0; i--) {
            Bar bar = bars.get(i);
            if (x >= bar.x() - 1 && x < bar.x() + WIDTH + 1 && y >= bar.y() - 1 && y <= bar.y() + bar.height() + 1) {
                if(bar.maximum()==0)return true;
                dragging = bar;
                grab = y >= bar.thumbY() && y <= bar.thumbY() + bar.thumbHeight() ? y - bar.thumbY() : bar.thumbHeight() / 2.0;
                bar.setter().accept(bar.at(y, grab));
                return true;
            }
        }
        return false;
    }
    public boolean drag(double y) { if (dragging == null) return false; dragging.setter().accept(dragging.at(y, grab)); return true; }
    public boolean release() { boolean active = dragging != null; dragging = null; return active; }
}
