package com.envyworld.client;

/** Monotonic heartbeat detector. A suspended JVM/OS grants a fresh grace period. */
public final class FreezeDetector {
    private volatile long heartbeat;
    private volatile boolean armed;
    private long previousPoll,graceUntil,reportedHeartbeat=Long.MIN_VALUE;
    public void beat(long now,boolean active){heartbeat=now;armed=active;}
    public synchronized boolean poll(long now,long timeout){
        long previous=previousPoll;previousPoll=now;
        if(previous!=0&&now-previous>3_000_000_000L){graceUntil=now+timeout;return false;}
        long beat=heartbeat;
        if(!armed||now<graceUntil||now-beat<timeout||reportedHeartbeat==beat)return false;
        reportedHeartbeat=beat;return true;
    }
}
