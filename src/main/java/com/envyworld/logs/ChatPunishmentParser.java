package com.envyworld.logs;

import java.util.*;
import java.util.regex.*;

/** Only recognises the server's anchored EW punishment broadcast, never player chat/command echoes. */
public final class ChatPunishmentParser {
    public record Event(String type,String target,String issuer,String reason,long time,String duration){}
    private static final Pattern HEADER=Pattern.compile("^(?:\\(EW\\)|\\[EW\\])\\s+Игрок\\s+(\\S+)\\s+(заткнул|заблокировал|предупредил)\\s+(?:по IP\\s+)?игрока\\s+([A-Za-z0-9_]{1,16})\\.?(?:\\s|$)");
    private String pending="";private long started;
    public void reset(){pending="";started=0;}
    public List<Event> accept(String text,long now){
        var out=new ArrayList<Event>();String clean=text.replaceAll("§[0-9a-fk-or]","");
        for(String line:clean.split("\\R",-1)){
            String s=line.strip();
            if(s.startsWith("(EW) Игрок ")||s.startsWith("[EW] Игрок ")){if(!pending.isBlank())finish(out,now);pending=s;started=now;}
            else if(!pending.isBlank()&&(s.startsWith("Время:")||s.startsWith("Причина:")||s.startsWith("- Предупреждён")))pending+="\n"+s;
            else if(!pending.isBlank()&&(s.isBlank()||!s.startsWith("Время:"))){if(!s.isBlank()||pending.contains("Причина:"))finish(out,now);}
        }
        if(pending.contains("Причина:")&&!pending.contains("Список предупреждений:"))finish(out,now);
        return out;
    }
    public List<Event> flush(long now){var out=new ArrayList<Event>();if(!pending.isBlank()&&now-started>=(pending.contains("Причина:")?300:2000))finish(out,now);return out;}
    private static String duration(String text){var match=Pattern.compile("(?m)^Время:\\s*(.+)$").matcher(text);return match.find()?match.group(1).strip():"Не указан";}
    private void finish(List<Event> out,long now){
        Matcher h=HEADER.matcher(pending);int reason=pending.indexOf("Причина:");
        if(h.find()&&reason>=0){String r=pending.substring(reason+8).strip();if(r.endsWith("."))r=r.substring(0,r.length()-1);if(!r.isBlank())out.add(new Event(switch(h.group(2)){case "заткнул"->"mutes";case "предупредил"->"warnings";default->"bans";},h.group(3),h.group(1),r,started,duration(pending)));}
        reset();
    }
}
