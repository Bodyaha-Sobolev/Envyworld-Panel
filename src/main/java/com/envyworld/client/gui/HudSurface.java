package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;

/** The same translucent black glass for HUD cards and the full-screen menu. */
public final class HudSurface {
    private HudSurface() {}
    public static void draw(float x,float y,float width,float height,float radius,int opacity){
        if(EnvyWorld.config().client.hudBlur)UiRenderer.submit((draw,matrix)->draw.drawBlurredRoundedRectangle(matrix,x,y,width,height,radius));
        UiGraphics.rounded(x,y,width,height,radius,Math.clamp(opacity,0,255)<<24);
    }
    public static void fullscreen(int width,int height){
        draw(0,0,width,height,0,Math.clamp(EnvyWorld.config().client.hudOpacity,120,255));
    }
}
