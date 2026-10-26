package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;
import com.envyworld.ranks.RankDetector;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** One in-flight server query. IP expansion only follows an explicitly requested history. */
public final class ProfileChecks {
    public record Snapshot(String state,List<String> lines,List<DupeIpResult.Account> accounts,List<String> rawLines){}
    private static final class Query {
        final String nick,command;final List<String> lines=new ArrayList<>();
        final Map<String,DupeIpResult.Account> accounts=new LinkedHashMap<>();
        final long start=System.nanoTime();final boolean expansion;
        long lastLine;String state="loading";boolean failed,headerSeen;
        Query(String nick,String command,boolean expansion){this.nick=nick;this.command=command;this.expansion=expansion;}
    }
    private static final Map<String,Query> cache=new LinkedHashMap<>();
    private static final ArrayDeque<String> addresses=new ArrayDeque<>();
    private static Query active,history;private static int total,done;
    private static final int MAX_ADDRESSES=1024,MAX_LINES=4096;
    private static long next;private static Object connection;
    private ProfileChecks(){}
    private static String key(String nick,String command){return (nick==null?"":nick).toLowerCase(Locale.ROOT)+":"+command;}
    public static synchronized String request(String nick,String command){
        var c=Minecraft.getInstance();
        if(!"dupeip".equals(command)&&!"iphistory".equals(command))return "Неизвестная проверка";
        if(nick==null||!nick.matches("[A-Za-z0-9_]{1,16}"))return "Сначала выберите игрока";
        if(c.getConnection()==null)return "Нет подключения к серверу";
        if(c.gameMode==null||c.gameMode.getPlayerMode()!=net.minecraft.world.level.GameType.SPECTATOR)return "Проверка доступна только в режиме наблюдателя (GM 3)";
        if(!RankDetector.canProfileCheck(command))return "Для /"+command+" недостаточно прав";
        if(active!=null||!addresses.isEmpty())return "Дождитесь проверки или отмените её в настройках";
        if(System.nanoTime()<next)return "Проверка доступна через "+Math.max(1,(next-System.nanoTime()+999_999_999L)/1_000_000_000L)+" с";
        connection=c.getConnection();history=null;total=done=0;
        active=new Query(nick,command,false);cache.put(key(nick,command),active);
        while(cache.size()>64)cache.remove(cache.keySet().iterator().next());
        if(!send(c,active))return "Не удалось отправить проверку";
        return command.equals("dupeip")?"Проверяем аккаунты игрока "+nick:"Проверяем историю IP игрока "+nick;
    }
    public static synchronized Snapshot snapshot(String nick,String command){
        var q=cache.get(key(nick,command));if(q==null)return new Snapshot("idle",List.of(),List.of(),List.of());
        var lines=new ArrayList<>(q.lines);
        if(q.command.equals("dupeip")&&!q.accounts.isEmpty()){
            lines.clear();lines.add(q.state.equals("loading")?"Проверка выполняется":DupeIpResult.summary(q.nick,q.accounts.values()));
            for(var account:q.accounts.values()){
                lines.add(account.nick()+" | "+account.status());
                for(String detail:ProfileAccountLogs.details(account))lines.add("  "+detail);
            }
        }
        if(q==history&&!q.accounts.isEmpty()){
            lines.add("Найденные аккаунты и логи:");
            for(var account:q.accounts.values().stream().limit(512).toList()){
                lines.add(account.nick()+" | "+account.status());
                for(String detail:ProfileAccountLogs.details(account))lines.add("  "+detail);
            }
            if(q.accounts.size()>512)lines.add("Ещё аккаунтов: "+(q.accounts.size()-512));
        }
        if(q==history&&total>0)lines.add("Проверка IP: "+done+" / "+total+(!addresses.isEmpty()||active!=null?" (по очереди)":""));
        if(!q.accounts.isEmpty())lines.add(ProfileAccountLogs.progress(q.accounts.values()));
        return new Snapshot(q.state,List.copyOf(lines),List.copyOf(q.accounts.values()),List.copyOf(q.lines));
    }
    /** Compact overlay lines; full unabridged responses remain available in the profile/MiniApp. */
    public static synchronized List<String> summary(String nick){
        var result=new ArrayList<String>(3);
        for(String command:List.of("dupeip","iphistory")){
            var q=cache.get(key(nick,command));if(q==null)continue;
            String label=command.equals("dupeip")?"Аккаунты по IP":"История IP";
            String detail=switch(q.state){case "loading"->"проверка выполняется";case "denied"->"нет доступа / ограничение";case "timeout"->"нет ответа";case "cancelled"->"остановлено";default->"";};
            if(detail.isEmpty()&&q==history&&total>0)detail=done+" / "+total+" IP"+(!addresses.isEmpty()||active!=null?" · проверка":" · готово");
            if(detail.isEmpty()&&command.equals("iphistory"))detail=IpCheckPlan.addresses(q.lines).size()+" IP";
            if(detail.isEmpty()&&command.equals("dupeip"))detail=q.accounts.isEmpty()?"ответ получен, аккаунты не распознаны":DupeIpResult.summary(q.nick,q.accounts.values());
            if(detail.isEmpty())detail=q.lines.isEmpty()?"нет данных":q.lines.get(q.lines.size()-1);
            result.add(label+": "+shorten(detail,90));
            for(String related:ProfileAccountLogs.related(q.accounts.values(),nick))if(result.size()<6)result.add(shorten(related,110));
        }
        return List.copyOf(result);
    }
    private static String shorten(String text,int limit){return text.length()<=limit?text:text.substring(0,limit-1)+"…";}
    public record Tag(String text,int color){}
    /** Related accounts only; the inspected player's own penalties have their existing card. */
    public static synchronized List<Tag> tags(String nick){
        var accounts=new LinkedHashMap<String,DupeIpResult.Account>();
        for(String command:List.of("dupeip","iphistory")){
            var q=cache.get(key(nick,command));if(q!=null)accounts.putAll(q.accounts);
        }
        if(accounts.isEmpty())return summary(nick).stream().map(line->new Tag(line,0xFFD1D3DB)).toList();
        accounts.remove(nick.toLowerCase(Locale.ROOT));
        var tags=new ArrayList<Tag>();
        for(var account:accounts.values().stream().limit(64).toList()){
            var statuses=new LinkedHashSet<String>();int color=account.color();
            if(account.status().startsWith("бан")){statuses.add(account.status().equals("бан по IP")?"Бан по IP":"Бан");}
            else if(account.status().equals("мут"))statuses.add("Мут");
            for(var row:ProfileAccountLogs.punishments(account.nick())){
                String status=switch(row.type()){case "bans"->account.status().equals("бан по IP")?"Бан по IP":"Бан";case "mutes"->"Мут";case "warnings"->"Предупреждение";default->row.typeLabel();};
                statuses.add(status);
            }
            if(!statuses.isEmpty())color=statuses.contains("Бан по IP")?0xFFAA0000:statuses.contains("Бан")?0xFFFF5555:0xFFFFAA00;
            tags.add(new Tag(account.nick()+(statuses.isEmpty()?"":" — "+String.join(", ",statuses)),color));
        }
        if(accounts.size()>64)tags.add(new Tag("Ещё: "+(accounts.size()-64),0xFFD1D3DB));
        return List.copyOf(tags);
    }
    public static List<List<Tag>> tagRows(List<Tag> tags,double maxWidth,java.util.function.ToDoubleFunction<String> measure){
        var rows=new ArrayList<List<Tag>>();var row=new ArrayList<Tag>();double width=0,gap=measure.applyAsDouble(" · ");
        for(var tag:tags){
            double next=measure.applyAsDouble(tag.text());
            if(!row.isEmpty()&&(row.size()==7||width+gap+next>maxWidth)){rows.add(List.copyOf(row));row.clear();width=0;}
            width+=(row.isEmpty()?0:gap)+next;row.add(tag);
        }
        if(!row.isEmpty())rows.add(List.copyOf(row));return List.copyOf(rows);
    }
    public static synchronized void observe(Component message){
        if(active==null||message==null)return;
        String rawText=message.getString();
        rawText=rawText.substring(0,Math.min(rawText.length(),65536));
        var matcher=java.util.regex.Pattern.compile("[^\\r\\n]+").matcher(rawText);
        while(matcher.find()){
            String line=shorten(matcher.group().replaceAll("(?i)§[0-9a-fk-orx]","").trim(),4096);
            if(line.isBlank())continue;
            var response=IpCheckPlan.response(line,active.nick,active.command,active.headerSeen);
            if(response==IpCheckPlan.Response.IGNORE)continue;
            if(active.lines.size()<2048)active.lines.add(line);
            active.headerSeen|=response==IpCheckPlan.Response.HEADER;
            if(active.command.equals("dupeip")&&response==IpCheckPlan.Response.LINE){
                for(var account:DupeIpResult.accounts(DupeIpResult.slice(message,matcher.start(),matcher.end())))
                    if(active.accounts.size()<1024)active.accounts.put(account.nick().toLowerCase(Locale.ROOT),account);
            }
            active.failed|=response==IpCheckPlan.Response.DENIED;
            active.lastLine=System.nanoTime();
            if(active.failed){finish(active.lastLine,false);break;}
        }
    }

    public static synchronized void cancel(){
        ProfileAccountLogs.clear();addresses.clear();if(active!=null)active.state="cancelled";if(history!=null)history.state="cancelled";active=null;next=System.nanoTime()+1_500_000_000L;
    }
    private static boolean send(Minecraft c,Query query){
        try{c.getConnection().sendCommand(query.command+" "+query.nick);return true;}
        catch(RuntimeException failure){query.failed=true;query.state="denied";active=null;addresses.clear();if(history!=null)history.state="denied";next=System.nanoTime()+5_000_000_000L;return false;}
    }
    private static void finish(long now,boolean timeout){
        var finished=active;if(finished==null)return;active=null;
        finished.state=finished.failed?"denied":timeout||finished.lines.isEmpty()?"timeout":"ready";
        if(!finished.state.equals("ready")){
            addresses.clear();if(history!=null){
                history.state=finished.state;
                if(finished.expansion&&history.lines.size()<MAX_LINES)history.lines.add("Проверка IP "+finished.nick+" остановлена: "+(finished.lines.isEmpty()?"нет ответа":shorten(finished.lines.get(finished.lines.size()-1),200)));
            }
        }else if(finished.command.equals("iphistory")&&EnvyWorld.config().client.checkIpAddresses&&RankDetector.canProfileCheck("dupeip")){
            history=finished;var found=IpCheckPlan.addresses(finished.lines);addresses.addAll(found.subList(0,Math.min(found.size(),MAX_ADDRESSES)));total=addresses.size();done=0;
            if(found.size()>MAX_ADDRESSES)history.lines.add("История ограничена первыми "+MAX_ADDRESSES+" адресами");
        }else if(finished.expansion&&history!=null){
            done++;if(history.lines.size()<MAX_LINES)history.lines.add("IP "+finished.nick+":");
            if(finished.accounts.size()==1&&finished.accounts.values().iterator().next().nick().equalsIgnoreCase(history.nick)){
                if(history.lines.size()<MAX_LINES)history.lines.add("Другие аккаунты не найдены");
            }else for(var line:finished.lines)if(history.lines.size()<MAX_LINES)history.lines.add(line);
            for(var account:finished.accounts.values())if(history.accounts.size()<4096)history.accounts.putIfAbsent(account.nick().toLowerCase(Locale.ROOT),account);
        }
        if(finished.state.equals("ready"))ProfileAccountLogs.enqueue(finished.accounts.values());
        next=finished.failed?now+5_000_000_000L:Math.max(now,finished.lastLine+IpCheckPlan.delay(total)*1_000_000L);
    }
    public static synchronized void tick(Minecraft c){
        if(c.getConnection()==null||connection!=null&&connection!=c.getConnection()){
            cancel();cache.clear();history=null;connection=c.getConnection();return;
        }
        long now=System.nanoTime();
        if(c.gameMode==null||c.gameMode.getPlayerMode()!=net.minecraft.world.level.GameType.SPECTATOR){
            if(active!=null||!addresses.isEmpty()){
                addresses.clear();if(active!=null)active.state="cancelled";if(history!=null)history.state="cancelled";active=null;next=now+1_500_000_000L;
            }
            return;
        }
        if(active!=null){
            long quiet=active.command.equals("dupeip")&&!active.accounts.isEmpty()?100_000_000L:350_000_000L;
            if(now-active.start>12_000_000_000L||!active.lines.isEmpty()&&now-active.lastLine>quiet)finish(now,now-active.start>12_000_000_000L);
        }
        ProfileAccountLogs.tick();
        if(!EnvyWorld.config().client.checkIpAddresses&&history!=null&&(!addresses.isEmpty()||active!=null&&active.expansion))cancel();
        if(active==null&&!addresses.isEmpty()&&now>=next){
            if(!RankDetector.canProfileCheck("dupeip")){cancel();return;}
            String address=addresses.removeFirst();active=new Query(address,"dupeip",true);send(c,active);
        }
    }
}
