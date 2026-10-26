package com.envyworld.logs;

import java.util.*;

/** One-to-one reconciliation in either arrival order; repeated identical punishments keep their multiplicity. */
public final class NotificationReconciler {
    private record Seen(String key,long time,boolean chat){}
    private final List<Seen> seen=new ArrayList<>();
    public void reset(){seen.clear();}
    public void acknowledge(String type,String target,String issuer,String reason,long time){String key=type+'|'+normalize(target)+'|'+issuer(issuer)+'|'+normalize(reason);for(int i=0;i<seen.size();i++){var s=seen.get(i);if(s.chat()&&s.key().equals(key)&&Math.abs(s.time()-time)<=120000){seen.remove(i);return;}}}
    private static String normalize(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("§[0-9a-fk-or]","").replaceAll("[\\s.]+"," ").trim();}
    private static String issuer(String s){return switch(normalize(s)){case "консоль","[консоль]","console","[console]","server","сервер"->"console";default->normalize(s);};}
    public boolean accept(String type,String target,String issuer,String reason,long eventTime,long now,boolean chat){
        seen.removeIf(s->now-s.time()>600000);String key=type+'|'+normalize(target)+'|'+issuer(issuer)+'|'+normalize(reason);
        for(int i=0;i<seen.size();i++){var s=seen.get(i);if(s.chat()!=chat&&s.key().equals(key)&&Math.abs(s.time()-eventTime)<=120000){seen.remove(i);return false;}}
        seen.add(new Seen(key,eventTime,chat));if(seen.size()>4096)seen.removeFirst();return true;
    }
}
