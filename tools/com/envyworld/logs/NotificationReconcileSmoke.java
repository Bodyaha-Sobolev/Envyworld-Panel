package com.envyworld.logs;
public final class NotificationReconcileSmoke {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        long now=1792825672000L;var p=new ChatPunishmentParser();
        check(p.accept("Player: (EW) Игрок Admin заткнул игрока Victim.\nПричина: Флуд.",now).isEmpty(),"Player chat is never treated as a punishment");
        check(p.accept("Admin: /mute Victim 1h Флуд",now).isEmpty(),"Command echo is not a punishment");
        check(p.accept("(EW) Игрок Admin заткнул по IP игрока Victim.",now).isEmpty(),"Header waits for the reason");
        p.accept("    Время: 1 час.",now+10);var event=p.accept("    Причина: Флуд.",now+20);
        check(event.size()==1&&event.getFirst().type().equals("mutes")&&event.getFirst().target().equals("Victim")&&event.getFirst().reason().equals("Флуд"),"Actual multi-message mute IP template parses");
        check(p.accept("(EW) Игрок Admin предупредил игрока Victim.\n    Причина: Нарушение.\n",now).size()==1,"Multi-line warning parses");
        check(p.accept("(EW) Игрок Консоль заблокировал игрока Victim.\nПричина: Список предупреждений:\n - Предупреждён игроком Admin: Флуд\n - Предупреждён игроком Helper: Нецензурная речь\n",now).getFirst().reason().contains("Helper"),"Warning lists remain complete");
        var r=new NotificationReconciler();
        check(r.accept("mutes","Victim","Admin","Флуд.",now,now,true),"Chat alerts immediately");
        check(!r.accept("mutes","victim","admin","Флуд",now-32000,now+10000,false),"Matching API result does not alert again");
        check(r.accept("mutes","Victim","Admin","Флуд",now+60000,now+60000,false),"Another punishment still alerts");
        check(!r.accept("mutes","Victim","Admin","Флуд",now+60050,now+60050,true),"API first followed by chat also alerts once");
        check(r.accept("bans","Victim","Other","Иная причина",now,now,false),"Unseen API event alerts");
        check(r.accept("bans","Victim","Консоль","Warning list",now,now,true)&&!r.accept("bans","Victim","Console","Warning list",now,now,false),"Console aliases reconcile between chat and API");
        r.reset();check(r.accept("warnings","Victim","Admin","A",now,now,true),"Baseline test chat");r.acknowledge("warnings","Victim","Admin","A",now);check(r.accept("warnings","Victim","Admin","A",now+60000,now+60000,false),"Baseline confirmation cannot suppress a later event");
        System.out.println("PASS: server chat parser and bidirectional chat/API notification reconciliation");
    }
}
