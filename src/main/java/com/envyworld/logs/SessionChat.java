package com.envyworld.logs;

import com.envyworld.EnvyWorld;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Paged live viewport; the complete current session is appended off the render thread. */
public final class SessionChat {
    public record Entry(String time,String kind,String text,String context){}
    private static final ArrayList<Entry> ENTRIES=new ArrayList<>();
    private static final ChatFilter FILTER=new ChatFilter();
    private static long filtered,revision;
    private static final ThreadPoolExecutor IO=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1024),r->{var t=new Thread(r,"EnvyWorld session chat");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String SESSION=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))+"-"+UUID.randomUUID().toString().substring(0,8);
    private static volatile String status="Сессия пуста";
    private static long generation;
    private static volatile String context="Неизвестный режим";
    private SessionChat(){}
    public static Path file(){return EnvyWorld.dataDir().resolve("chat-sessions").resolve(SESSION+".txt");}
    public static synchronized List<Entry> entries(){return List.copyOf(ENTRIES);}
    public static synchronized long revision(){return revision;}
    public static synchronized long filtered(){return filtered;}
    public static synchronized int size(){return ENTRIES.size();}
    public static synchronized long generation(){return generation;}
    public static synchronized List<Entry> since(int index){return List.copyOf(ENTRIES.subList(Math.min(index,ENTRIES.size()),ENTRIES.size()));}
    public static void context(String value){context=value;}
    public static String context(){return context;}
    public static String status(){return status;}
    public static synchronized void accept(String kind,String raw){
        if(raw==null||raw.isBlank())return;
        var message=FILTER.classify(kind,raw,System.currentTimeMillis());if(message==null){filtered++;return;}kind=message.category();String text=message.text();
        if(text.length()>16000)text=text.substring(0,16000);

        var entry=new Entry(TIME.format(LocalTime.now()),kind,text,context);ENTRIES.add(entry);revision++;
        String saved="["+entry.time()+"] ["+entry.context()+"] ["+kind+"] "+text+"\n";
        write(()->{Files.createDirectories(file().getParent());Files.writeString(file(),saved,StandardOpenOption.CREATE,StandardOpenOption.APPEND);});

    }
    public static void saveSummary(SessionState.Snapshot snapshot){write(()->{Files.createDirectories(file().getParent());Files.writeString(file().resolveSibling(SESSION+".summary.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(snapshot));});}
    public static void flush(){saveSummary(SessionState.snapshot());try{IO.submit(()->{}).get(3,TimeUnit.SECONDS);}catch(Exception ignored){}}
    private interface Write {void run()throws Exception;}
    private static void write(Write action){try{IO.execute(()->{try{action.run();status="Сохранено за текущую сессию";}catch(Exception e){status="Не удалось сохранить чат";}});}catch(RejectedExecutionException e){status="Сохранение чата не успевает за потоком сообщений";}}
    public static synchronized void clear(){ENTRIES.clear();generation++;revision++;filtered=0;write(()->{Files.createDirectories(file().getParent());Files.writeString(file(),"");});status="Чат очищен";}
    public static void tick(){}
}
