package com.envyworld.client.gui;
/** A nameplate stays attached to its player, including at viewport edges and in crowds. */
public final class NameplateLayout {
    private NameplateLayout(){}
    public static float[] place(float headCenter,float headTop,float width,float height,float gap){
        return new float[]{headCenter-width/2,headTop-height-gap,width,height};
    }
}
