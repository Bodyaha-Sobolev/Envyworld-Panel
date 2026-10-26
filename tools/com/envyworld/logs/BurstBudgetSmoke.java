package com.envyworld.logs;
import com.envyworld.client.BurstBudget;
final class BurstBudgetSmoke {
 static void run(){
  var b=new BurstBudget();int admitted=0;
  for(int i=0;i<100000;i++)if(b.take(0,2000))admitted++;
  if(admitted!=2000||b.count()!=2000)throw new AssertionError("Unbounded particle burst");
  if(b.take(999999999L,2000)||!b.take(1000000000L,2000))throw new AssertionError("Wrong reset boundary");
  var sound=new BurstBudget();for(int i=0;i<256;i++)if(!sound.take(1,256))throw new AssertionError("Ordinary sound rejected");
  if(sound.take(2,256))throw new AssertionError("Sound flood admitted");
  System.out.println("PASS: particle/sound flood budgets, exact window boundary, no counter overflow");
 }
}
