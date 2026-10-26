package com.envyworld.logs;
import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
public final class TelegramAuthSmoke {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static byte[] hmac(byte[] key,String text)throws Exception{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));}
 public static String sign(String token,String owner,long when)throws Exception{var fields=new TreeMap<String,String>();fields.put("auth_date",Long.toString(when));fields.put("query_id","test-session");fields.put("user","{\"id\":"+owner+",\"first_name\":\"Tester\"}");String check=fields.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("\n"));String hash=HexFormat.of().formatHex(hmac(hmac("WebAppData".getBytes(StandardCharsets.UTF_8),token),check));return fields.entrySet().stream().map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"))+"&hash="+hash;}
 public static void run()throws Exception{
  long now=1792830000;String token="LOCAL_TEST_ONLY";String valid=sign(token,"42",now);
  check(TelegramAuth.verify(valid,token,"42",now),"Signed owner Mini App accepted");
  check(!TelegramAuth.verify(valid,token,"99",now),"Another owner rejected");
  check(!TelegramAuth.verify(sign(token,"99",now),token,"42",now),"Correctly signed foreign user rejected");
  check(!TelegramAuth.verify(valid.replace("Tester","Intruder"),token,"42",now),"Tampered payload rejected");
  check(!TelegramAuth.verify(valid,token,"42",now+3601),"Expired authorization rejected");
  check(!TelegramAuth.verify(sign(token,"42",now+60),token,"42",now),"Future authorization rejected");
  check(!TelegramAuth.verify(valid+"&auth_date="+now,token,"42",now),"Duplicate fields rejected");
  check(ServerModes.label("Выживание #1").equals("Выживание 1: Ио")&&ServerModes.label("Выживание #5").equals("Выживание 5: Альматея"),"All server IDs have display names");
  check(ServerModes.label(ServerModes.label("Выживание #4")).equals("Выживание 4: Калипсо"),"Server label is idempotent");
  check(SessionState.vanishBar("§fНевидимка :\u00a0)")&&!SessionState.vanishBar("Невидимка выключена"),"Initial vanish title tolerates NBSP without matching disabled text");
  var notice=new SessionChat.Entry("12:34:56","Наказание","Новый мут\nВыдано: 24.10.2026 12:34:56\nПолучил: Victim\nВыдал: Mod\nРежим: Выживание #1\nПричина: Полная причина","Ио");
  String page=TelegramPanel.pages(List.of(notice),"punishments").getFirst();check(page.contains("\nПолучил:")&&page.contains("\nВыдал:")&&page.contains("Полная причина"),"Telegram punishment detail retains labeled lines");
  System.out.println("PASS: Mini App signed owner authentication, expiry/tamper rejection, initial vanish and full punishment details");
 }
}
