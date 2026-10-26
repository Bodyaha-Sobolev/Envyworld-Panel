package com.envyworld.logs;

import com.envyworld.EnvyWorld;
import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;

/** Personal credentials stay local; all Telegram traffic uses one owner-scoped panel. */
public final class TelegramNotify {
 private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
 private static final ThreadPoolExecutor WORK=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),r->{var t=new Thread(r,"EnvyWorld Telegram");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 private static class Settings {String token="",username="",chat="",panelId="",ownerLabel="",alertId="";boolean enabled,relayChat;}
 private static volatile Settings settings;
 private record Notice(String text){}
 private static volatile Notice pendingNotice;
 public static void punishment(String text){if(enabled()){pendingNotice=new Notice(text);refreshPanel();}}
 private static volatile TelegramPanel panel;private static Settings panelSettings;
 private static volatile String status="Не подключён",nonce="";
 private static volatile long lastAlive,nextPoll;
 private static final com.envyworld.client.MiniAppLifecycle appLifecycle=new com.envyworld.client.MiniAppLifecycle();
 private static String menuUrl="uninitialized";
 private static final java.util.concurrent.atomic.AtomicBoolean polling=new java.util.concurrent.atomic.AtomicBoolean();
 private TelegramNotify(){}
 private static Path file(){return EnvyWorld.dataDir().resolve("telegram-private.json");}
 private static synchronized Settings settings(){if(settings==null){try{settings=JSON.fromJson(Files.readString(file()),Settings.class);}catch(Exception ignored){}if(settings==null||settings.token==null||settings.username==null||settings.chat==null)settings=new Settings();if(settings.panelId==null)settings.panelId="";if(settings.ownerLabel==null)settings.ownerLabel="";if(settings.enabled&&!settings.chat.isBlank())status="Подключён: @"+settings.username;}return settings;}
 public static String status(){settings();return status;}
 public static boolean enabled(){return settings().enabled;}
 public static boolean relayEnabled(){return settings().relayChat;}
 public static void relayEnabled(boolean value){settings().relayChat=value;save();refreshPanel();}
 public static String ownerLabel(){return settings().ownerLabel.isBlank()?(enabled()?"Личный чат подключён":"Не привязан"):settings().ownerLabel;}
 public static String botName(){return settings().username.isBlank()?"Не подключён":"@"+settings().username;}
 public static boolean alive(){return lastAlive>0&&System.currentTimeMillis()-lastAlive<90000;}
 public static String health(){return lastAlive==0?"Связь ещё не проверена":alive()?"Бот отвечает: "+java.time.Instant.ofEpochMilli(lastAlive).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")):"Нет свежего подтверждения связи";}
 public static void disable(){settings().enabled=false;com.envyworld.client.RemoteMiniApp.stop();pendingNotice=null;panel=null;lastAlive=0;save();status="Отключён";}
 private static synchronized void save(){try{Files.createDirectories(file().getParent());Files.writeString(file(),JSON.toJson(settings()));}catch(Exception e){status="Не удалось сохранить настройки Telegram";}}
 private static boolean work(Runnable task){try{WORK.execute(()->{try{task.run();}catch(Exception e){status="Telegram недоступен · проверьте соединение";}});return true;}catch(RejectedExecutionException e){status="Очередь Telegram заполнена";return false;}}
 private static JsonElement request(String token,String method,JsonObject data)throws Exception{
  if(!token.matches("[0-9]{5,}:[A-Za-z0-9_-]{20,}"))throw new IllegalArgumentException("token");
  var builder=HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot"+token+"/"+method)).timeout(Duration.ofSeconds(12));
  HttpRequest req;
  if(data.has("_frame")){var fields=data.deepCopy();fields.remove("_frame");byte[] frame=com.envyworld.client.RemoteGame.frame();if(frame==null)throw new IllegalStateException("frame pending");String boundary="envyworld"+java.util.UUID.randomUUID().toString().replace("-","");req=builder.header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(TelegramMultipart.encode(fields,frame,boundary))).build();}
  else req=builder.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(data))).build();
  var response=HTTP.send(req,HttpResponse.BodyHandlers.ofString());var json=JsonParser.parseString(response.body()).getAsJsonObject();
  if(response.statusCode()!=200||!json.get("ok").getAsBoolean())throw new TelegramPanel.ApiFailure(json.has("error_code")?json.get("error_code").getAsInt():response.statusCode(),json.has("description")?json.get("description").getAsString():"Telegram request failed");return json.get("result");
 }
 public static void prepare(String token){disable();status="Проверяем токен…";work(()->{try{var result=request(token.trim(),"getMe",new JsonObject()).getAsJsonObject();var next=new Settings();next.token=token.trim();next.username=result.get("username").getAsString();settings=next;lastAlive=System.currentTimeMillis();nonce=java.util.UUID.randomUUID().toString().replace("-","");save();status="Токен принят. Откройте бота и нажмите Start";}catch(Exception e){status="Токен не принят или Telegram недоступен";}});}
 public static String botLink(){return settings().username.isBlank()?"":"https://t.me/"+settings().username+(nonce.isBlank()?"":"?start="+nonce);}
 public static void pair(){var s=settings();if(s.token.isBlank()||nonce.isBlank()){status="Сначала проверьте токен";return;}String expected=nonce;status="Проверяем Start…";
  work(()->{try{var data=new JsonObject();data.addProperty("offset",-100);data.addProperty("limit",100);data.addProperty("timeout",0);data.add("allowed_updates",JsonParser.parseString("[\"message\",\"callback_query\"]"));
   for(var item:request(s.token,"getUpdates",data).getAsJsonArray()){var u=item.getAsJsonObject();if(!u.has("message"))continue;var m=u.getAsJsonObject("message");if(!m.has("text")||!m.get("text").getAsString().equals("/start "+expected))continue;var chat=m.getAsJsonObject("chat");var from=m.getAsJsonObject("from");String id=chat.get("id").getAsString();if(!TelegramPanel.owned(from,chat,id))continue;s.chat=id;s.ownerLabel=from.has("username")?"@"+from.get("username").getAsString():from.has("first_name")?from.get("first_name").getAsString():id;s.enabled=true;save();nonce="";panel=null;nextPoll=0;status="Подключён: @"+s.username;return;}status="Start не найден. Откройте бота кнопкой и нажмите Start";
  }catch(Exception e){status="Не удалось связать личный чат. Бот должен быть без webhook";}});
 }
 public static void refreshPanel(){nextPoll=0;}
 public static void send(String text){if(panel!=null)panel.feedback(text);refreshPanel();}
 public static void testMessage(){if(!enabled()){status="Сначала подключите личный чат";return;}send("Проверка связи: панель работает. Это сообщение обновляется на месте.");}
 public static void checkConnection(){var s=settings();if(s.token.isBlank()){status="Сначала проверьте токен";return;}work(()->{try{request(s.token,"getMe",new JsonObject());lastAlive=System.currentTimeMillis();status="Бот отвечает: @"+s.username;refreshPanel();}catch(Exception e){lastAlive=0;status="Бот не отвечает";}});}
 public static void tick(){var s=settings();var client=net.minecraft.client.Minecraft.getInstance();
  appLifecycle.tick(client.player==null?null:client.getConnection(),s.chat,s.enabled&&!s.chat.isBlank()&&!s.token.isBlank(),()->{com.envyworld.client.RemoteGame.start();com.envyworld.client.RemoteMiniApp.start(s.chat,s.token);refreshPanel();},()->com.envyworld.client.RemoteMiniApp.stop());
  long now=System.currentTimeMillis();if(!s.enabled||s.chat.isBlank()||now<nextPoll||!polling.compareAndSet(false,true))return;nextPoll=now+2000;
  if(!work(()->{try{
   if(settings()!=s||!s.enabled)return;
   if(panel==null||panelSettings!=s){if(s.ownerLabel.isBlank()){try{var query=new JsonObject();query.addProperty("chat_id",s.chat);var owner=request(s.token,"getChat",query).getAsJsonObject();s.ownerLabel=owner.has("username")?"@"+owner.get("username").getAsString():owner.has("first_name")?owner.get("first_name").getAsString():s.chat;save();}catch(Exception ignored){}}panelSettings=s;panel=new TelegramPanel(s.chat,s.panelId,(method,data)->{if(settings()!=s||!s.enabled)throw new IllegalStateException("disabled");return request(s.token,method,data);},(value,context,feedback)->{
    if(settings()!=s||!s.enabled){feedback.accept("Telegram отключён");return;}
    com.envyworld.client.TelegramGameChat.send(value,context,()->settings()==s&&s.enabled,result->{feedback.accept(result);refreshPanel();});
   },id->{s.panelId=id;save();},new TelegramPanel.Sharing(()->s.relayChat,()->{s.relayChat=!s.relayChat;save();}));panel.setLive(new TelegramPanel.Live(){public void start(){com.envyworld.client.RemoteGame.start();com.envyworld.client.RemoteMiniApp.start(s.chat,s.token);}public void stop(){com.envyworld.client.RemoteMiniApp.stop();}public String url(){return com.envyworld.client.RemoteMiniApp.url();}public String playerInfo(String nick){try{var profile=com.envyworld.client.RemoteGame.profile(nick);return "HP: "+profile.get("hp").getAsString()+"\nРежим: "+profile.get("mode").getAsString()+"\nUUID: "+profile.get("uuid").getAsString();}catch(Exception ignored){return "HP: —\nРежим: —";}}public String status(){return com.envyworld.client.RemoteMiniApp.status()+"\n"+com.envyworld.client.RemoteGame.status();}public void action(String action){com.envyworld.client.RemoteGame.action(action);}public boolean ready(){return com.envyworld.client.RemoteGame.frame()!=null;}});}
   panel.tick();
   String appUrl=com.envyworld.client.RemoteMiniApp.url();
   if(!appUrl.equals(menuUrl)){try{var menu=new JsonObject();menu.addProperty("chat_id",s.chat);var button=new JsonObject();if(appUrl.isBlank())button.addProperty("type","default");else{button.addProperty("type","web_app");button.addProperty("text","Minecraft");var web=new JsonObject();web.addProperty("url",appUrl);button.add("web_app",web);}menu.add("menu_button",button);request(s.token,"setChatMenuButton",menu);menuUrl=appUrl;}catch(Exception ignored){}}
   var notice=pendingNotice;if(notice!=null){s.alertId=TelegramAlert.replace((method,data)->{if(settings()!=s||!s.enabled)throw new IllegalStateException("disabled");return request(s.token,method,data);},s.chat,s.alertId,notice.text());save();if(pendingNotice==notice)pendingNotice=null;}lastAlive=System.currentTimeMillis();status="Личная панель подключена";
  }catch(Exception e){nextPoll=System.currentTimeMillis()+10000;status=e instanceof TelegramPanel.ApiFailure api?(api.code==409?"Этот бот уже запущен в другом приложении или экземпляре Minecraft":api.code==429?"Telegram ограничил частоту запросов. Повторим позже":"Ошибка Telegram "+api.code):"Нет соединения с Telegram. Повторим подключение";}finally{polling.set(false);}}))polling.set(false);
 }
}
