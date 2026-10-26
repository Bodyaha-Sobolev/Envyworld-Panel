package com.envyworld.logs;
import com.envyworld.client.MiniAppLifecycle;
import com.envyworld.client.gui.NameplateLayout;
final class LifecycleLayoutSmoke {
 static void run(){
  // Crossing the viewport edge must never pin a label to the screen or detach it from its head.
  for(int center=-200;center<=2120;center+=5)for(int top=-80;top<=1160;top+=5){
   var r=NameplateLayout.place(center,top,160,34,2);
   if(r[0]+r[2]/2!=center||r[1]+r[3]+2!=top)throw new AssertionError("Detached nameplate");
  }
  var first=NameplateLayout.place(960,540,160,34,2);
  for(int i=0;i<100;i++)if(!java.util.Arrays.equals(first,NameplateLayout.place(960,540,160,34,2)))throw new AssertionError("Crowded names moved");
  var life=new MiniAppLifecycle();var connection=new Object();int[] counts={0,0};
  Runnable start=()->counts[0]++,stop=()->counts[1]++;
  for(int i=0;i<100;i++)life.tick(connection,"42",true,start,stop);
  if(counts[0]!=1||counts[1]!=0)throw new AssertionError("Repeated automatic start");
  life.tick(connection,"43",true,start,stop);
  life.tick(null,"43",true,start,stop);
  life.tick(null,"43",true,start,stop);
  life.tick(new Object(),"43",true,start,stop);
  life.tick(connection,"43",false,start,stop);
  if(counts[0]!=3||counts[1]!=3)throw new AssertionError("Connection lifecycle");
 }
}
