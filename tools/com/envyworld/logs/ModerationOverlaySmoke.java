package com.envyworld.logs;
import com.envyworld.client.gui.IpCheckPlan;
import java.util.List;
public final class ModerationOverlaySmoke {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void run(){
        check(IpCheckPlan.response("Сканирование Bob (203.0.113.5): [онлайн] [оффлайн] [заткнут] [заблокирован по айпи]","Bob","dupeip",false)==IpCheckPlan.Response.HEADER,"Captured CMI dupeip header");
        check(IpCheckPlan.response("История IP адресов игрока Bob (лимит: 1000):","Bob","iphistory",false)==IpCheckPlan.Response.HEADER,"Captured CMI history header");
        check(IpCheckPlan.response("- [03.06.2026] Bob: 203.0.113.5","Bob","iphistory",true)==IpCheckPlan.Response.LINE,"Captured CMI bracketed date and nickname row");
        check(IpCheckPlan.response("- [03.06.2026] Other: 203.0.113.5","Bob","iphistory",true)==IpCheckPlan.Response.IGNORE,"Other subject history excluded");
        var ips=IpCheckPlan.addresses(List.of("История: 127.0.0.1, 127.0.0.1:25565", "255.255.255.255 999.1.1.1 192.168.0.5", "1.2.3.4/ban someone"));
        check(ips.equals(List.of("127.0.0.1","255.255.255.255","192.168.0.5","1.2.3.4")),"IP validation and unique queue");
        check(!IpCheckPlan.valid("1.2.3.4 say test")&&!IpCheckPlan.valid("foo.example"),"No command injection or DNS lookup");
        check(IpCheckPlan.addresses(List.of("[2001:DB8::1]:25565, ::1, 2001:db8::1", "::ffff:192.0.2.4 8.8.8.8:25565")).equals(List.of("2001:db8::1","::1","::ffff:192.0.2.4","8.8.8.8")),"Numeric IPv6 including mapped addresses and bracketed ports");
        for(String invalid:List.of("1:2:3:4:5:6:7:8:9","1:2:3:4:5:6:7","1:::2","1::2::3","::ffff:999.1.2.3","fe80::1%eth0","2001:db8::1 /ban","1.2.3.4:65536"))check(!IpCheckPlan.valid(invalid),"Invalid address rejected: "+invalid);
        check(IpCheckPlan.addresses(List.of("x1.2.3.4 y2001:db8::1 fe80::1%eth0 999.1.2.3 1.2.3.4.5")).isEmpty(),"Do not extract a valid suffix from malformed or scoped addresses");
        for(String chat:List.of("Alice: iphistory Bob 203.0.113.5", "[Staff] Alice » История IP Bob 203.0.113.5", "Alice → IP Bob: 203.0.113.5", "Л | [Игрок] Alice ➥ История IP Bob 203.0.113.5", "<Alice> нет прав 203.0.113.5"))check(IpCheckPlan.response(chat,"Bob","iphistory",true)==IpCheckPlan.Response.IGNORE,"Player chat never enters active query: "+chat);
        check(IpCheckPlan.response("История IP игрока Bob:","Bob","iphistory",false)==IpCheckPlan.Response.HEADER,"Russian command header starts response");
        check(IpCheckPlan.response("IP history for Bob:","Bob","iphistory",false)==IpCheckPlan.Response.HEADER,"English command header starts response");
        check(IpCheckPlan.response("IP history for SomeoneElse:","Bob","iphistory",false)==IpCheckPlan.Response.IGNORE,"Different subject header cannot start query");
        check(IpCheckPlan.response("Alice says Bob has IP 203.0.113.5","Bob","iphistory",false)==IpCheckPlan.Response.IGNORE,"A matching subject in arbitrary prose is not a command header");
        check(IpCheckPlan.response("203.0.113.5 2026-10-26","Bob","iphistory",false)==IpCheckPlan.Response.IGNORE,"Unassociated address never starts history stream");
        check(IpCheckPlan.response("203.0.113.5 2026-10-26","Bob","iphistory",true)==IpCheckPlan.Response.LINE,"Associated numeric history row accepted");
        check(IpCheckPlan.response("Alice says my IP is 203.0.113.5","Bob","iphistory",true)==IpCheckPlan.Response.IGNORE,"Unstructured player prose cannot enqueue addresses after header");
        check(IpCheckPlan.response("2001:db8::1 2026-10-26","Bob","iphistory",true)==IpCheckPlan.Response.LINE,"IPv6 colon is not mistaken for player chat");
        check(IpCheckPlan.response("[LiteBans] У вас нет прав","Bob","iphistory",false)==IpCheckPlan.Response.DENIED,"Permission denial stops query immediately");
        check(IpCheckPlan.response("Слишком часто, подождите","Bob","dupeip",true)==IpCheckPlan.Response.DENIED,"Rate limit stops expansion");
        check(IpCheckPlan.response("Alice, Bob, Other (online)","203.0.113.5","dupeip",true)==IpCheckPlan.Response.LINE,"Account response continuation accepted");
        check(IpCheckPlan.delay(1)==250&&IpCheckPlan.delay(10)==250&&IpCheckPlan.delay(11)==500&&IpCheckPlan.delay(10000)==500,"Requested 250/500 ms history cadence");
        for(String until:List.of("", "Снято", "Истекло",Long.toString(System.currentTimeMillis()/1000-1))){var row=new PunishLogClient.Row("mutes","Alice","Mod","Rule","",until,"Хаб");check(!PunishmentClock.active(row),"Inactive or unknown punishment must not be shown: "+until);}
        for(String until:List.of("Навсегда",Long.toString(System.currentTimeMillis()/1000+120))){var row=new PunishLogClient.Row("mutes","Alice","Mod","Rule","",until,"Хаб");check(PunishmentClock.active(row),"Current punishment shown");}
        var year=new PunishLogClient.Row("mutes","YearTest","Mod","Rule","","29 августа в 18:48 2025 года","Хаб");
        check(!PunishmentClock.active(year),"Trailing API year must not be guessed as the current year");
        check(PunishmentClock.deadline(year).equals("До 29.08.2025 18:48 МСК"),"Exact expiry date/time and timezone");
        var future=new PunishLogClient.Row("mutes","FutureTest","Mod","Rule","","29 августа в 18:48 2030 года","Хаб");
        check(PunishmentClock.active(future)&&PunishmentClock.deadline(future).contains("2030"),"Future trailing year remains active");
        for(String until:List.of("4 октября в 14:08 | (наказание снято игроком Moderator)","Никогда | (наказание снято игроком Moderator)","21 ноября в 19:03 2025 года | (истёк)"))
            check(!PunishmentClock.active(new PunishLogClient.Row("bans","Vikakas","Mod","Rule","",until,"Хаб")),"Real API removed/expired history must not become a current nametag punishment");
        if(Boolean.getBoolean("envyworld.capturedLogFixtureTest"))try {
            var fixture=java.nio.file.Path.of("build/verification/nametag-active");
            for(String nick:List.of("Bodyaha","Vikakas")){
                int count=0;
                for(String type:List.of("bans","mutes","warnings")){
                    var file=fixture.resolve((nick.equals("Bodyaha")?"bodyaha-":"")+type+".json");
                    if(!java.nio.file.Files.exists(file))continue;
                    var batch=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();
                    for(var element:batch.getAsJsonArray("rows")){
                        var row=element.getAsJsonObject();if(!row.get("who_was_punished").getAsString().equalsIgnoreCase(nick))continue;
                        String until=row.get("until").getAsString().replaceAll("(?i)<br\\s*/?>"," | ");
                        var punishment=new PunishLogClient.Row(type,nick,"Mod","Rule","",until,"Выживание #1");
                        if(PunishmentClock.active(punishment)){count++;check(type.equals("warnings"),"Bodyaha historical ban/mute leaked into current overlay");check(PunishmentClock.deadline(punishment).equals("До 08.11.2026 12:29 МСК"),"Real warning expiry date");}
                    }
                }
                check(count==(nick.equals("Bodyaha")?3:0),"Exact live fixture count for "+nick+": "+count);
            }
            System.out.println("PASS: captured API fixtures: Bodyaha exactly 3 active warnings, Vikakas 0 current punishments");
        }catch(java.io.IOException e){throw new RuntimeException(e);}
        System.out.println("PASS: active-only punishments, numeric IP queue validation, deduplication and adaptive delay");
    }
}
