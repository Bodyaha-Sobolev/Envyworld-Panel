package com.envyworld.client;

import java.util.List;
import java.util.function.Consumer;

/** Tick-driven per-command delays. A late frame never catches up timed commands in a burst. */
public final class MacroSequence<T> {
    public record Step<T>(T value,int delayBeforeMs){}
    private final List<Step<T>> steps;
    private int index;
    private long nextAt;
    public MacroSequence(List<Step<T>> steps,long now){this.steps=List.copyOf(steps);nextAt=now+(steps.isEmpty()?0:delay(steps.getFirst().delayBeforeMs()));}
    private static int delay(int value){return Math.max(0,Math.min(3600000,value));}
    public boolean done(){return index>=steps.size();}
    public void tick(long now,Consumer<T> send){
        while(!done()&&now>=nextAt){send.accept(steps.get(index++).value());if(!done())nextAt=now+delay(steps.get(index).delayBeforeMs());}
    }
}
