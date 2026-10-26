package com.envyworld.logs;
import com.envyworld.client.FreezeDetector;
final class FreezeGuardSmoke {
 static void run(){
  long second=1_000_000_000L;var d=new FreezeDetector();d.beat(second,true);
  for(int i=1;i<=19;i++)if(d.poll(i*second,20*second))throw new AssertionError("Short lag disconnected");
  if(!d.poll(21*second,20*second)||d.poll(22*second,20*second))throw new AssertionError("Must signal a stall only once");
  d.beat(23*second,false);if(d.poll(24*second,20*second))throw new AssertionError("Menu/loading watchdog armed");
  d.beat(25*second,true);d.poll(25*second,20*second);
  if(d.poll(100*second,20*second))throw new AssertionError("JVM/OS suspension caused a disconnect");
  for(int i=101;i<120;i++)if(d.poll(i*second,20*second))throw new AssertionError("No resume grace period");
  d.beat(120*second,true);if(d.poll(120*second,20*second))throw new AssertionError("Recovered client disconnected");
  // The faster guard must fire at the selected threshold, never at an earlier poll.
  for(int seconds:new int[]{3,5,8,15,30}){
   var fast=new FreezeDetector();long start=second,timeout=seconds*second,step=second/4;
   fast.beat(start,true);
   for(long elapsed=step;elapsed<timeout;elapsed+=step)
    if(fast.poll(start+elapsed,timeout))throw new AssertionError("Early disconnect at "+seconds+"s threshold");
   if(!fast.poll(start+timeout,timeout))throw new AssertionError("Missed selected "+seconds+"s threshold");
   if(fast.poll(start+timeout+step,timeout))throw new AssertionError("Repeated fast disconnect");
   long recovered=start+timeout+2*step;fast.beat(recovered,true);
   for(long elapsed=step;elapsed<timeout;elapsed+=step)
    if(fast.poll(recovered+elapsed,timeout))throw new AssertionError("Recovery heartbeat was ignored");
   if(!fast.poll(recovered+timeout,timeout))throw new AssertionError("Second independent freeze was ignored");
  }
  System.out.println("PASS: 3/5/8/15/30-second thresholds at 250ms polls; recovery, short lag, inactive/loading, repeated alerts and OS/JVM suspension excluded");
 }
}
