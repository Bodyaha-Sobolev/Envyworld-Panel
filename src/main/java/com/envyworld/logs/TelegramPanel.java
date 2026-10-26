package com.envyworld.logs;

import com.google.gson.*;
import java.util.*;
import java.util.function.Consumer;

/** One private message, edited in place. All navigation and input are owner-scoped. */
public final class TelegramPanel {
    public interface Transport {JsonElement call(String method,JsonObject body)throws Exception;}
    public interface Remote {void send(String text,String context,Consumer<String> feedback);}
    public interface Live {void start();void stop();void action(String action);boolean ready();default String url(){return "";}default String status(){return "";}default String playerInfo(String nick){return "";}}
    private Live live=new Live(){public void start(){}public void stop(){}public void action(String a){}public boolean ready(){return false;}};
    public void setLive(Live value){live=value;}
    private String screenId="";
    public record Sharing(java.util.function.BooleanSupplier enabled,Runnable toggle){}
    public static final class ApiFailure extends Exception {
        public final int code;public ApiFailure(int code,String message){super(message);this.code=code;}
    }
    private final String owner,nonce=UUID.randomUUID().toString().substring(0,8);
    private final Transport transport;private final Remote remote;private final Consumer<String> remember;
    private final Sharing sharing;
    private final long started=System.currentTimeMillis()/1000;
    private long offset;
    private String messageId,section="profile",lastPayload="",draft="",draftContext="",draftDestination="";
    private volatile String feedback="";
    private boolean follow=true,awaiting;
    private int page;
    private String logQuery="",playerNick="";
    private boolean receivedOnly;
    public TelegramPanel(String owner,String messageId,Transport transport,Remote remote,Consumer<String> remember){this(owner,messageId,transport,remote,remember,new Sharing(()->true,()->{}));}
    public TelegramPanel(String owner,String messageId,Transport transport,Remote remote,Consumer<String> remember,Sharing sharing){this.owner=owner;this.messageId=messageId==null?"":messageId;this.transport=transport;this.remote=remote;this.remember=remember;this.sharing=sharing;}
    public static boolean owned(JsonObject from,JsonObject chat,String owner){return from!=null&&chat!=null&&from.has("id")&&chat.has("id")&&chat.has("type")&&chat.get("type").getAsString().equals("private")&&owner.equals(from.get("id").getAsString())&&owner.equals(chat.get("id").getAsString());}
    private static String text(JsonObject object,String key){return object.has(key)?object.get(key).getAsString():"";}
    private JsonObject body(){var body=new JsonObject();body.addProperty("chat_id",owner);return body;}
    public void feedback(String value){feedback=value;}
    public void tick()throws Exception{
        var query=new JsonObject();query.addProperty("offset",offset);query.addProperty("limit",100);query.addProperty("timeout",0);query.add("allowed_updates",JsonParser.parseString("[\"message\",\"callback_query\"]"));
        for(var element:transport.call("getUpdates",query).getAsJsonArray()){
            var update=element.getAsJsonObject();if(update.has("update_id"))offset=Math.max(offset,update.get("update_id").getAsLong()+1);
            if(update.has("callback_query"))callback(update.getAsJsonObject("callback_query"));
            if(update.has("message"))message(update.getAsJsonObject("message"));
        }
        publish();
        if(section.equals("live")&&live.ready()&&live.url().isBlank())try{publishScreen();}catch(ApiFailure e){if(e.code!=400||!e.getMessage().contains("not modified"))feedback="Кадр не доставлен: "+e.getMessage();}
    }
    private void callback(JsonObject callback)throws Exception{
        if(!callback.has("message"))return;var message=callback.getAsJsonObject("message");
        if(!owned(callback.getAsJsonObject("from"),message.getAsJsonObject("chat"),owner)||!(messageId.equals(text(message,"message_id"))||!screenId.isBlank()&&screenId.equals(text(message,"message_id"))))return;
        String data=text(callback,"data"),prefix="ew:"+nonce+":";if(!data.startsWith(prefix)){
            var stale=new JsonObject();stale.addProperty("callback_query_id",text(callback,"id"));stale.addProperty("text","Панель обновилась. Нажмите кнопку в обновлённом сообщении");
            try{transport.call("answerCallbackQuery",stale);}catch(Exception ignored){}return;
        }
        String action=data.substring(prefix.length());
        if(action.startsWith("game:")){if(section.equals("live"))live.action(action.substring(5));}
        else switch(action){
            case "chat","punishments","commands","events","profile","modes"->{closeScreen();section=action;follow=true;page=0;awaiting=false;draft="";feedback="";}
            case "prev"->{follow=false;page=Math.max(0,page-1);}
            case "next"->{follow=false;page++;}
            case "follow"->{follow=true;}
            case "logs"->{closeScreen();section="logs";logQuery="";receivedOnly=false;page=0;follow=true;feedback="";}
            case "findlogs"->{section="findlogs";feedback="";}
            case "findplayer"->{closeScreen();section="findplayer";feedback="";}
            case "playerhistory"->{logQuery=playerNick;receivedOnly=true;section="logs";page=0;follow=true;}
            case "live"->{section="live";live.start();feedback="";}
            case "endLive"->{closeScreen();section="profile";}
            case "compose"->{closeScreen();section="compose";awaiting=true;draft="";feedback="";}
            case "cancel"->{awaiting=false;draft="";section="profile";feedback="Отправка отменена";}
            case "send"->{if(!draft.isEmpty()&&!awaiting){String value=draft,context=draftDestination;draft="";section="profile";feedback="Отправляем в игру…";remote.send(value,context,this::feedback);}}
            case "refresh"->{}
            case "share"->{sharing.toggle().run();}
            default->{if(action.startsWith("player:")&&action.substring(7).matches("[A-Za-z0-9_]{1,16}")){playerNick=action.substring(7);section="person";feedback="";}else return;}
        }
        var answer=new JsonObject();answer.addProperty("callback_query_id",text(callback,"id"));try{transport.call("answerCallbackQuery",answer);}catch(Exception ignored){}
    }
    private void message(JsonObject message)throws Exception{
        if(!owned(message.getAsJsonObject("from"),message.getAsJsonObject("chat"),owner)||!message.has("date")||message.get("date").getAsLong()<started)return;
        String value=text(message,"text").strip();
        if(value.equals("/start")||value.equals("/panel")){closeScreen();section="profile";return;}
        if(value.equals("/logs")||value.startsWith("/logs ")||section.equals("findlogs")){
            String nick=value.startsWith("/logs")?value.substring(5).strip():value;
            if(!nick.matches("[A-Za-z0-9_]{0,16}")){feedback="Ник: до 16 латинских букв, цифры и _";return;}
            closeScreen();logQuery=nick;receivedOnly=false;section="logs";page=0;follow=true;return;
        }
        if(value.startsWith("/player ")||section.equals("findplayer")){
            String nick=value.startsWith("/player ")?value.substring(8).strip():value;
            if(!nick.matches("[A-Za-z0-9_]{1,16}")){feedback="Введите ник игрока: латинские буквы, цифры и _";return;}
            closeScreen();playerNick=nick;section="person";return;
        }
        if(value.isEmpty())return;
        if(value.length()>256||value.contains("\n")||value.contains("\r")){feedback="Введите одну строку до 256 символов";return;}
        if(!ChatFilter.redact(value).equals(value)){feedback="Команды с паролями здесь не принимаются";return;}
        closeScreen();section="compose";draft=value;draftContext=SessionState.snapshot().context();draftDestination=SessionState.destination();awaiting=false;
    }
    public static List<String> pages(List<SessionChat.Entry> entries,String section){
        var result=new ArrayList<String>();var page=new StringBuilder();int count=0;
        for(var entry:entries){
            boolean keep=switch(section){case "chat"->List.of("Чат","Личные","Отправлено").contains(entry.kind());case "punishments"->entry.kind().equals("Наказание");case "commands"->List.of("Команда","Команды игроков").contains(entry.kind());case "events"->List.of("События","Друзья").contains(entry.kind());default->false;};
            if(!keep)continue;
            String mode=entry.context().substring(entry.context().lastIndexOf('·')+1).strip().replace("Выживание: ","");
            String line="["+entry.time()+"] ["+ServerModes.label(mode)+"] "+(section.equals("punishments")?entry.text():entry.text().replaceAll("\\s+"," "));int limit=section.equals("punishments")?2600:400;if(line.length()>limit)line=line.substring(0,limit-1)+"…";
            if(section.equals("punishments")&&!page.isEmpty()){result.add(page.toString());page.setLength(0);count=0;}
            if(count>=90||page.length()+line.length()+1>3100){result.add(page.toString());page.setLength(0);count=0;}
            page.append(line).append('\n');count++;
        }
        if(!page.isEmpty()||result.isEmpty())result.add(page.isEmpty()?"Пока нет записей":page.toString());return result;
    }
    private JsonObject key(String label,String action){var key=new JsonObject();key.addProperty("text",label);key.addProperty("callback_data","ew:"+nonce+":"+action);return key;}
    private JsonObject markup(){
        if(section.equals("compose")){
            var composeRows=new JsonArray();var controls=new JsonArray();
            if(!awaiting&&!draft.isEmpty())controls.add(key("Отправить в игру","send"));
            controls.add(key("Отмена","cancel"));composeRows.add(controls);
            var result=new JsonObject();result.add("inline_keyboard",composeRows);return result;
        }
        var rows=new JsonArray();var row=new JsonArray();row.add(key("Чат","chat"));row.add(key("Наказания","punishments"));row.add(key("Команды","commands"));rows.add(row);
        row=new JsonArray();row.add(key("События","events"));row.add(key("Профиль","profile"));row.add(key("Режимы","modes"));rows.add(row);
        row=new JsonArray();row.add(key("Все логи","logs"));row.add(key("Найти игрока","findplayer"));rows.add(row);
        if(List.of("chat","punishments","commands","events","logs").contains(section)){row=new JsonArray();row.add(key("←","prev"));row.add(key("К последним","follow"));row.add(key("→","next"));rows.add(row);}
        if(section.equals("logs")){
            row=new JsonArray();row.add(key("Поиск по нику","findlogs"));rows.add(row);
            var list=apiRows();if(!list.isEmpty()){var selected=list.get(Math.min(page,list.size()-1));row=new JsonArray();if(selected.punished().matches("[A-Za-z0-9_]{1,16}"))row.add(key("Получил: "+selected.punished(),"player:"+selected.punished()));if(selected.punisher().matches("[A-Za-z0-9_]{1,16}"))row.add(key("Выдал: "+selected.punisher(),"player:"+selected.punisher()));if(!row.isEmpty())rows.add(row);}
        }
        if(section.equals("person")){row=new JsonArray();row.add(key("Наказания игрока","playerhistory"));rows.add(row);}
        if(!live.url().isBlank()){
            row=new JsonArray();var app=new JsonObject();app.addProperty("text","Открыть приложение");var web=new JsonObject();web.addProperty("url",live.url());app.add("web_app",web);row.add(app);rows.add(row);
            row=new JsonArray();var browse=new JsonObject();browse.addProperty("text","Логи и профили игроков");var browser=new JsonObject();browser.addProperty("url",live.url()+"/#logs");browse.add("web_app",browser);row.add(browse);rows.add(row);
        }
        row=new JsonArray();row.add(key(section.equals("live")?"Закрыть экран":"В эфир: экран и управление",section.equals("live")?"endLive":"live"));rows.add(row);
        row=new JsonArray();if(section.equals("compose")&&!draft.isEmpty()){row.add(key("Отправить в игру","send"));row.add(key("Отмена","cancel"));}else{row.add(key("Написать в игру","compose"));row.add(key("Обновить","refresh"));}rows.add(row);
        row=new JsonArray();row.add(key(sharing.enabled().getAsBoolean()?"Передача чата: вкл":"Передача чата: выкл","share"));rows.add(row);
        var result=new JsonObject();result.add("inline_keyboard",rows);return result;
    }
    private void publish()throws Exception{
        var state=SessionState.snapshot();String title=switch(section){case "logs","findlogs"->"Логи сервера";case "person","findplayer"->"Профиль игрока";case "chat"->"Чат";case "punishments"->"Наказания";case "commands"->"Команды";case "events"->"События";case "modes"->"Время по режимам";case "compose"->"Отправка в игру";case "live"->"Экран Minecraft";default->"Профиль";};
        String content;
        if(section.equals("profile"))content=SessionState.summary()+"\nНаказаний за сессию: "+SessionChat.entries().stream().filter(e->e.kind().equals("Наказание")).count()+"\nСкрыто рекламы: "+SessionChat.filtered();
        else if(section.equals("modes")){var lines=new StringBuilder();state.modes().entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(16).forEach(e->lines.append(e.getKey()).append(": ").append(SessionState.duration(e.getValue())).append('\n'));content=lines.isEmpty()?"Пока нет времени в режимах":lines.toString();}
        else if(section.equals("compose"))content=awaiting?"Напишите следующим сообщением текст или /команду.\nПеред отправкой будет предпросмотр и кнопка подтверждения.":"Будет отправлено в "+draftContext+":\n\n"+draft;
        else if(section.equals("findlogs"))content="Отправьте ник для поиска по всем логам. Также доступна команда /logs Ник";
        else if(section.equals("findplayer"))content="Отправьте ник игрока. Также доступна команда /player Ник";
        else if(section.equals("person"))content=person();
        else if(section.equals("logs")){
            PunishLogClient.tickAll(logQuery,"all");var snapshot=PunishLogClient.snapshot(logQuery,"all");var list=apiRows();page=follow?0:Math.max(0,Math.min(page,Math.max(0,list.size()-1)));
            content="Логи API"+(logQuery.isBlank()?"":" | "+logQuery)+"\n"+(list.isEmpty()?snapshot.error().isBlank()?"Загрузка или нет записей":snapshot.error():"Запись "+(page+1)+" / "+list.size()+"\n\n"+details(list.get(page)))+(snapshot.limited()?"\nAPI ограничило выборку":"");
        }
        else if(section.equals("live"))content=live.status()+"\n"+(live.url().isBlank()?"Ниже доступны кадры игры и кнопки. Подключение Mini App: статус выше. Приложение появится после проверки HTTPS-подключения.":"Нажмите «Открыть приложение». На ПК: WASD, мышь, E, 1–9. На телефоне: кнопки и свайп камеры.")+"\nДоступ есть только у владельца бота. Закрыть эфир можно кнопкой ниже.";
        else if(!section.equals("punishments")&&!sharing.enabled().getAsBoolean())content="Передача чата выключена. Включите её кнопкой ниже.";
        else{var pages=pages(SessionChat.entries(),section);page=follow?pages.size()-1:Math.max(0,Math.min(page,pages.size()-1));content="Страница "+(page+1)+" / "+pages.size()+"\n"+pages.get(page);}
        String header="EnvyWorld | "+state.nick()+"\n"+state.visibility()+" | В игре: "+SessionState.duration(state.onlineMs())+"\n"+state.context()+"\nОбновлено: "+java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))+"\n\n"+title+"\n";
        String value=header+content+(feedback.isBlank()?"":"\n\n"+feedback);if(value.length()>3900){int end=Character.isHighSurrogate(value.charAt(3898))?3898:3899;value=value.substring(0,end)+"…";}
        var data=body();data.addProperty("text",value);data.add("reply_markup",markup());var preview=new JsonObject();preview.addProperty("is_disabled",true);data.add("link_preview_options",preview);
        String payload=data.toString();if(payload.equals(lastPayload))return;
        if(messageId.isBlank())create(data);
        else{data.addProperty("message_id",Long.parseLong(messageId));try{transport.call("editMessageText",data);}catch(ApiFailure e){if(e.code==400&&e.getMessage().toLowerCase(Locale.ROOT).contains("not modified")){}else if(e.code==400&&e.getMessage().toLowerCase(Locale.ROOT).contains("not found")){data.remove("message_id");create(data);}else throw e;}}
        lastPayload=payload;
    }
    private void create(JsonObject data)throws Exception{data.addProperty("disable_notification",true);var result=transport.call("sendMessage",data).getAsJsonObject();messageId=result.get("message_id").getAsString();remember.accept(messageId);}
    private List<PunishLogClient.Row> apiRows(){var rows=PunishLogClient.snapshot(logQuery,"all").rows();return receivedOnly?rows.stream().filter(r->r.punished().equalsIgnoreCase(logQuery)).toList():rows;}
    private static String details(PunishLogClient.Row row){String reason=row.reason();if(reason.length()>2500)reason=reason.substring(0,2499)+"…";return row.typeLabel()+"\nВыдано: "+row.date()+"\nПолучил: "+row.punished()+"\nВыдал: "+row.punisher()+"\nРежим: "+ServerModes.label(row.server())+"\nДо: "+row.until()+"\n\nПричина:\n"+reason;}
    private String person(){
        PunishLogClient.tickAll(playerNick,"all");var snapshot=PunishLogClient.snapshot(playerNick,"all");long received=snapshot.rows().stream().filter(r->r.punished().equalsIgnoreCase(playerNick)).count(),issued=snapshot.rows().stream().filter(r->r.punisher().equalsIgnoreCase(playerNick)).count();
        return "Игрок: "+playerNick+"\n"+live.playerInfo(playerNick)+"\nПолучено наказаний: "+received+"\nВыдано: "+issued+(snapshot.state().equals("loading")?"\nИстория загружается…":"")+"\n\nСкин и профиль: https://klauncher.gg/user/"+playerNick+"\nПредпросмотр скина и HP также доступны в Mini App";
    }
    private void closeScreen()throws Exception{live.stop();if(!screenId.isBlank()){var data=body();data.addProperty("message_id",Long.parseLong(screenId));try{transport.call("deleteMessage",data);}catch(Exception ignored){}screenId="";}}
    private JsonObject gameMarkup(){var rows=new JsonArray();String[][] keys={{"Вперёд","w","Стоп","stop","Прыжок","jump"},{"Влево","a","Назад","s","Вправо","d"},{"Камера ←","left","Камера ↑","up","Камера →","right"},{"Камера ↓","down","Присесть","sneak","Инвентарь","inventory"},{"Удар","attack","Использовать","use","Слот +","slotnext"}};for(var labels:keys){var row=new JsonArray();for(int i=0;i<labels.length;i+=2)row.add(key(labels[i],"game:"+labels[i+1]));rows.add(row);}var row=new JsonArray();row.add(key("Закрыть экран","endLive"));rows.add(row);var markup=new JsonObject();markup.add("inline_keyboard",rows);return markup;}
    private void publishScreen()throws Exception{var data=body();data.add("reply_markup",gameMarkup());data.addProperty("_frame",true);if(screenId.isBlank()){data.addProperty("photo","attach://frame");data.addProperty("caption","Экран Minecraft. Только ваш личный чат.");data.addProperty("disable_notification",true);screenId=transport.call("sendPhoto",data).getAsJsonObject().get("message_id").getAsString();}else{data.addProperty("message_id",Long.parseLong(screenId));var media=new JsonObject();media.addProperty("type","photo");media.addProperty("media","attach://frame");media.addProperty("caption","Minecraft | "+java.time.LocalTime.now().withNano(0));data.add("media",media);transport.call("editMessageMedia",data);}}
}
