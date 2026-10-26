package com.envyworld.client;
/** Fixed monotonic one-second admission window, bounded even under a packet storm. */
public final class BurstBudget {
    private long at;private boolean initialized;private int count;
    public synchronized boolean take(long now,int maximum){
        if(!initialized||now-at>=1_000_000_000L||now<at){at=now;count=0;initialized=true;}
        if(count>=maximum)return false;count++;return true;
    }
    public synchronized int count(){return count;}
}
