package com.envyworld.client.gui;

import com.envyworld.logs.*;
import java.util.*;

/** Small local view of the shared log cache; at most one player query every two seconds. */
public final class NameplatePunishments {
    private static final Map<String,Long> fetched=new LinkedHashMap<>();
    private static long next;
    private NameplatePunishments(){}
    public static void reset(){fetched.clear();next=0;}
    public static void request(List<String> visible){
        long now=System.currentTimeMillis();if(now<next)return;
        String nick=visible.stream().min(Comparator.comparingLong(n->fetched.getOrDefault(n,0L))).orElse(null);
        if(nick!=null&&now-fetched.getOrDefault(nick,0L)>=10000){
            fetched.put(nick,now);next=now+2000;PunishLogClient.request(nick,"all");
        }
        while(fetched.size()>256)fetched.remove(fetched.keySet().iterator().next());
    }
    public static List<PunishLogClient.Row> active(String nick){
        return PunishLogClient.snapshot(nick,"all").rows().stream()
            .filter(r->r.punished().equalsIgnoreCase(nick)&&PunishmentClock.active(r)).distinct().toList();
    }
}
