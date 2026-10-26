package com.envyworld.logs;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;

/** Local observations only: no vanish guesses based on missing players or TAB. */
public final class SessionState {
    public record Snapshot(String nick,String context,boolean connected,long onlineMs,long invisibleMs,String visibility,Map<String,Long> modes){}
    private static final long START=System.currentTimeMillis();
    private static long previous=START,online,invisible,nextSave;
    private static Object connection;
    private static volatile String destination="offline";
    public static String destination(){return destination;}
    public static String destinationNow(Minecraft client){return client.player==null||client.getConnection()==null?"offline":System.identityHashCode(client.getConnection())+":"+System.identityHashCode(client.level);}
    private static Boolean serverVanish;
    private static boolean effect;
    private static boolean hadVanishBar;
    public static boolean vanishBar(String title){
        String normalized=java.text.Normalizer.normalize(title,java.text.Normalizer.Form.NFKC)
                .replaceAll("§[0-9a-fk-orxA-FK-ORX]", "").replaceAll("[\\p{Cf}\\p{Z}\\s]", "").toLowerCase(Locale.ROOT);
        return normalized.matches("невидимка[^\\p{L}\\p{N}]*");
    }
    private static final Map<String,Long> MODES=new LinkedHashMap<>();
    private static volatile Snapshot snapshot=new Snapshot("Игрок","Вне сервера",false,0,0,"Видимость неизвестна",Map.of());
    private SessionState(){}
    public static long startedAt(){return START;}
    public static Snapshot snapshot(){return snapshot;}
    public static synchronized void observe(String raw){
        var current=Minecraft.getInstance().getConnection();
        if(connection!=current){connection=current;serverVanish=null;hadVanishBar=false;}
        String text=raw.replaceAll("§[0-9a-fk-orxA-FK-ORX]","").strip();
        if(text.matches("(?iu)^\\(EW\\) Вы исчезли[.!]?$"))serverVanish=true;
        if(text.matches("(?iu)^\\(EW\\) Вы появились[.!]?$"))serverVanish=false;
    }
    public static synchronized void tick(Minecraft client){
        long now=System.currentTimeMillis(),elapsed=Math.max(0,now-previous);previous=now;
        var old=snapshot;if(old.connected()){online+=elapsed;if(old.visibility().startsWith("Невидимка"))invisible+=elapsed;MODES.merge(old.context(),elapsed,Long::sum);}
        boolean connected=client.player!=null&&client.getConnection()!=null;
        destination=destinationNow(client);
        if(connection!=client.getConnection()){connection=client.getConnection();serverVanish=null;hadVanishBar=false;}
        boolean bar=connected&&((com.envyworld.mixin.BossHealthOverlayAccessor)client.gui.getBossOverlay()).envyworld$events().values().stream().anyMatch(e->vanishBar(e.getName().getString()));
        if(bar){serverVanish=true;hadVanishBar=true;}else if(hadVanishBar){serverVanish=false;hadVanishBar=false;}
        effect=connected&&client.player.hasEffect(MobEffects.INVISIBILITY);
        String visibility=bar?"Невидимка (полоса сервера)":Boolean.TRUE.equals(serverVanish)?"Невидимка (сервер)":effect?"Эффект невидимости (ваниш не подтверждён)":Boolean.FALSE.equals(serverVanish)?"Видим":"Видимость неизвестна";
        snapshot=new Snapshot(client.getUser().getName(),SessionChat.context(),connected,online,invisible,visibility,Map.copyOf(MODES));
        if(now>=nextSave){nextSave=now+30000;SessionChat.saveSummary(snapshot);}
    }
    public static String duration(long ms){long seconds=Math.max(0,ms/1000);return String.format(java.util.Locale.ROOT,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60);}
    public static String summary(){var s=snapshot;return s.nick()+"\n"+s.context()+"\n"+s.visibility()+"\nВ игре: "+duration(s.onlineMs())+"\nНевидимость за сессию: "+duration(s.invisibleMs());}
}
