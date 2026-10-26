package com.envyworld.client;

/** Tick-driven repeat timing; no threads, sleeps or catch-up bursts. */
public final class MacroRepeatState {
    private int remaining;
    private final int delay;
    private final boolean unlimited;
    private long next;
    public MacroRepeatState(String mode, int count, int delay, long now) {
        unlimited = !"PRESS".equals(mode) && count == 0;
        remaining = Math.max(1, Math.min(10_000, count));
        this.delay = Math.max(50, Math.min(3_600_000, delay));
        next = now;
    }
    public boolean due(long now) { return !done() && now >= next; }
    public void ran(long now) { if (!unlimited) remaining--; next = now + delay; }
    public boolean done() { return !unlimited && remaining <= 0; }
}
