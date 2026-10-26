package com.envyworld.logs;
import com.envyworld.client.gui.*;
import com.envyworld.ranks.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.*;
import java.lang.reflect.*;
final class ProfileChecksSmoke {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object query(String nick,String command)throws Exception{
        var type=Class.forName("com.envyworld.client.gui.ProfileChecks$Query");var constructor=type.getDeclaredConstructor(String.class,String.class,boolean.class);constructor.setAccessible(true);
        Object q=constructor.newInstance(nick,command,false);field(ProfileChecks.class,"active").set(null,q);
        ((Map)field(ProfileChecks.class,"cache").get(null)).put(nick.toLowerCase()+":"+command,q);return q;
    }
    static void finish()throws Exception{var method=ProfileChecks.class.getDeclaredMethod("finish",long.class,boolean.class);method.setAccessible(true);method.invoke(null,System.nanoTime(),false);}
    static void run()throws Exception{
        var config=field(com.envyworld.EnvyWorld.class,"config");Object saved=config.get(null);
        var current=field(RankDetector.class,"current");Object savedRank=current.get(null);
        try{
            Object q=query("Target","dupeip");
            ProfileChecks.observe(Component.literal("Сканирование Target. [онлайн] [оффлайн] [заткнут] [заблокирован по айпи]"));
            check(field(q.getClass(),"headerSeen").getBoolean(q),"Header without a numeric IP must start the response");
            var row=Component.empty().append(Component.literal("Target").withStyle(ChatFormatting.GREEN)).append(", ")
                .append(Component.literal("Muted").withStyle(ChatFormatting.GOLD)).append(", ")
                .append(Component.literal("Banned").withStyle(ChatFormatting.RED)).append(", ")
                .append(Component.literal("IpBanned").withStyle(ChatFormatting.DARK_RED));
            ProfileChecks.observe(row);finish();
            var result=ProfileChecks.snapshot("Target","dupeip");check(result.state().equals("ready")&&result.accounts().size()==4,"Account reply completes instead of hanging on request: "+result);
            check(result.accounts().get(1).status().equals("мут")&&result.accounts().get(2).status().equals("бан")&&result.accounts().get(3).status().equals("бан по IP"),"Component colours retain mute/ban/IP-ban meanings");
            check(!ProfileChecks.summary("Target").toString().contains("запрос…"),"No decorative request ellipsis");
            query("Solo","dupeip");ProfileChecks.observe(Component.literal("Сканирование Solo. [онлайн] [оффлайн]"));ProfileChecks.observe(Component.literal("Solo").withStyle(ChatFormatting.GREEN));finish();
            check(ProfileChecks.snapshot("Solo","dupeip").lines().getFirst().equals("Другие аккаунты не найдены"),"Only the queried account is a clear empty result");
            var cfg=new com.envyworld.config.RulesConfig();cfg.client.checkIpAddresses=true;config.set(null,cfg);var staff=new Rank();staff.kind="staff";current.set(null,staff);
            query("Target","iphistory");ProfileChecks.observe(Component.literal("История IP адресов игрока Target (лимит: 10):\n- [03.06.2026] Target: 203.0.113.5\n- [03.06.2026] Target: 203.0.113.5\n- [02.06.2026] Target: 203.0.113.6"));finish();
            check(((Collection)field(ProfileChecks.class,"addresses").get(null)).size()==2,"History queues each unique address exactly once");
            check(IpCheckPlan.delay(10)==250&&IpCheckPlan.delay(11)==500,"250/500 ms threshold");
            var ban=new PunishLogClient.Row("bans","IpBanned","Mod","Rule test","2026-10-01","Никогда","Выживание #2");
            var removed=new PunishLogClient.Row("bans","IpBanned","Mod","Old","2026-10-01","(наказание снято игроком Mod)","Выживание #2");
            var actor=new PunishLogClient.Row("bans","Other","IpBanned","Not their punishment","2026-10-01","Никогда","Выживание #2");
            check(ProfileAccountLogs.active("IpBanned",List.of(ban,removed,actor)).equals(List.of(ban)),"Only active punishments against the exact account are annotated");
            ((Map)field(ProfileAccountLogs.class,"results").get(null)).put("ipbanned",new PunishLogClient.Snapshot(List.of(ban,removed,actor),"ok","",0,false));
            var linked=new DupeIpResult.Account("IpBanned",0xFFAA0000,"бан по IP");
            check(ProfileAccountLogs.related(List.of(linked),"Target").getFirst().contains("IpBanned — бан по IP: Rule test"),"Related IP-banned account is annotated with its active log reason");
            var warning=new PunishLogClient.Row("warnings","Muted","Mod","Rule warning","2026-10-01","Никогда","Выживание #2");
            ((Map)field(ProfileAccountLogs.class,"results").get(null)).put("muted",new PunishLogClient.Snapshot(List.of(warning),"ok","",0,false));
            var tags=ProfileChecks.tags("Target");
            check(tags.stream().anyMatch(t->t.text().equals("Muted — Мут, Предупреждение")&&t.color()==0xFFFFAA00),"Related active warnings appear inline in orange");
            check(tags.stream().noneMatch(t->t.text().equals("Target")||t.text().startsWith("Target —")),"Inspected account is not duplicated above its own nameplate");
            check(tags.stream().anyMatch(t->t.text().equals("IpBanned — Бан по IP")&&t.color()==0xFFAA0000),"Related account and punishment share one coloured entry");
            var shortNames=java.util.stream.IntStream.range(0,15).mapToObj(i->new ProfileChecks.Tag("N"+i,0xFFFFFFFF)).toList();
            var packed=ProfileChecks.tagRows(shortNames,30,String::length);
            check(packed.getFirst().size()==6&&packed.stream().allMatch(r->r.size()<=7),"Rows greedily fit multiple accounts within width and seven-account cap");
            for(var packedRow:packed)check(packedRow.stream().mapToInt(t->t.text().length()).sum()+(packedRow.size()-1)*3<=30,"Account row stays within warning-card width");
            check(ProfileChecks.tags("Solo").isEmpty(),"Single current account does not create a redundant tag");
            System.out.println("PASS: real dupeip header, styled statuses, single-account empty result, deduplicated history queue and exact active log cross-checks");
        }finally{ProfileChecks.cancel();((Map)field(ProfileChecks.class,"cache").get(null)).clear();config.set(null,saved);current.set(null,savedRank);}
    }
}
