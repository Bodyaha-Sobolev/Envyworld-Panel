package com.envyworld.client.gui;

import com.envyworld.logs.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** A single bounded API lookup at a time, independent of server command replies. */
public final class ProfileAccountLogs {
    private static final LinkedHashSet<String> pending=new LinkedHashSet<>();
    private static final Map<String,PunishLogClient.Snapshot> results=new LinkedHashMap<>();
    private static final Set<String> requested=new HashSet<>();
    private static CompletableFuture<PunishLogClient.Snapshot> inFlight;
    private static long next,generation;
    private ProfileAccountLogs(){}
    public static synchronized void enqueue(Collection<DupeIpResult.Account> accounts){
        for(var account:accounts){String nick=account.nick().toLowerCase(Locale.ROOT);if(requested.size()<4096&&requested.add(nick))pending.add(nick);}
    }
    public static synchronized void clear(){pending.clear();results.clear();requested.clear();generation++;}
    public static synchronized void tick(){
        if(inFlight!=null||pending.isEmpty()||System.nanoTime()<next)return;
        String nick=pending.iterator().next();pending.remove(nick);long round=generation;
        var request=PunishLogClient.request(nick,"all");inFlight=request;
        request.whenComplete((result,failure)->{synchronized(ProfileAccountLogs.class){
            if(round==generation)results.put(nick,failure==null?result:new PunishLogClient.Snapshot(List.of(),"error","Логи недоступны",0,false));
            inFlight=null;next=System.nanoTime()+(failure!=null||result!=null&&result.state().equals("error")?5_000_000_000L:500_000_000L);
        }});
    }
    public static synchronized List<String> details(DupeIpResult.Account account){
        var result=results.get(account.nick().toLowerCase(Locale.ROOT));
        if(result==null)return List.of("Логи: проверка выполняется");
        if(result.state().equals("error"))return List.of("Логи недоступны");
        var active=active(account.nick(),result.rows());
        if(active.isEmpty())return List.of(result.state().equals("partial")||result.limited()?"Логи: неполные данные, активных наказаний не найдено":"Логи: активных наказаний нет");
        return active.stream().limit(6).map(row->"Логи: "+row.typeLabel()
                +": "+row.reason()+" | "+ServerModes.label(row.server())).toList();
    }
    public static synchronized String progress(Collection<DupeIpResult.Account> accounts){
        long count=accounts.stream().filter(a->results.containsKey(a.nick().toLowerCase(Locale.ROOT))).count();
        return "Логи: "+count+" / "+accounts.size();
    }
    public static synchronized List<PunishLogClient.Row> punishments(String nick){
        var result=results.get(nick.toLowerCase(Locale.ROOT));
        return result==null?List.of():active(nick,result.rows());
    }
    public static List<PunishLogClient.Row> active(String nick,List<PunishLogClient.Row> rows){
        return rows.stream().filter(r->r.punished().equalsIgnoreCase(nick)&&PunishmentClock.active(r)).distinct().toList();
    }
    public static synchronized List<String> related(Collection<DupeIpResult.Account> accounts,String subject){
        var lines=new ArrayList<String>();
        for(var account:accounts){
            if(account.nick().equalsIgnoreCase(subject))continue;
            var result=results.get(account.nick().toLowerCase(Locale.ROOT));if(result==null)continue;
            for(var row:active(account.nick(),result.rows()))if(row.type().equals("bans")){
                lines.add("Общий IP: "+account.nick()+" — "+(account.status().equals("бан по IP")?"бан по IP":"бан")+": "+row.reason());
                if(lines.size()==2)return List.copyOf(lines);
            }
        }
        return List.copyOf(lines);
    }
}
