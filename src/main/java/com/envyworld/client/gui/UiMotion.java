package com.envyworld.client.gui;

/** Time-based easing: the same response at 30, 60 and 144 frames per second. */
public final class UiMotion {
    private float value;private long last;
    public UiMotion(float initial){value=initial;}
    public float update(float target){
        long now=System.nanoTime();float seconds=last==0?1f/60:Math.min(.1f,(now-last)/1_000_000_000f);last=now;
        value=com.envyworld.EnvyWorld.config().client.menuAnimations?advance(value,target,seconds):target;
        if(Math.abs(value-target)<.001f)value=target;return value;
    }
    public void snap(float target){value=target;last=System.nanoTime();}
    public static float advance(float value,float target,float seconds){return target+(value-target)*(float)Math.exp(-Math.max(0,seconds)*22);}
}
