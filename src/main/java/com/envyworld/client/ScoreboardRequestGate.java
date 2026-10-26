package com.envyworld.client;

/** One delayed request per connection/world/mode, never a repeating toggle. */
public final class ScoreboardRequestGate {
    private Object connection,level;
    private String mode="";
    private int absentTicks;
    private boolean requested;
    public boolean tick(Object connection,Object level,String mode,boolean present,boolean commandAvailable){
        if(this.connection!=connection||this.level!=level||!this.mode.equals(mode)){
            this.connection=connection;this.level=level;this.mode=mode;absentTicks=0;requested=false;
        }
        if(present){absentTicks=0;return false;}
        if(++absentTicks<100||requested||!commandAvailable)return false;
        requested=true;return true;
    }
}
