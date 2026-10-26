package com.envyworld.logs;
import com.envyworld.client.ScoreboardRequestGate;
import com.envyworld.ranks.*;
import java.nio.file.*;
import java.util.*;
final class ScoreboardRankSmoke {
    static void run() throws Exception {
        var ranks=List.of(new com.google.gson.Gson().fromJson(Files.readString(Path.of("src/main/resources/envyworld/ranks.json")),Rank[].class));
        check("player",List.of("EnvyWorld","Донат: Игрок"),ranks);
        check("vip",List.of("§aПривилегия: §bВип"),ranks);
        check("envy",List.of("Донат: Envy"),ranks);
        check("envypremium",List.of("Донат: EnvyPremium"),ranks);
        check("president",List.of("Привилегия: ᴘʀᴇꜱɪᴅᴇɴᴛ"),ranks);
        check("president",List.of("Донат: &cᴘ&6ʀ&eᴇ&aꜱ&bɪ&9ᴅ&dᴇ&5ɴ&cᴛ"),ranks);
        check("envy",List.of("Донат: &#c24affᴇɴᴠʏ"),ranks);
        check("vip",List.of("Донат:","§x§6§6§d§9§e§fᴠɪᴘ"),ranks);
        check("vip",List.of("Донат:","〈§bВип§f〉"),ranks);
        check("elite",List.of("Донат:","(§6Элита§f)"),ranks);
        check("president",List.of("Донат:","[§cᴘʀᴇꜱɪᴅᴇɴᴛ§f]"),ranks);
        check("envy",List.of("Донат:","|§bEnvy§f|"),ranks);
        check("burjui",List.of("Донат:","〈§eБуржуй§f〉"),ranks);
        check(null,List.of("Донат: ᴘ"),ranks);
        if(!RankDetector.normaliseRankLabel("&cᴘ").equals("p"))throw new AssertionError("Small-cap example");
        check("sponsor",List.of("Донат:","Спонсор"),ranks);
        check("envy",List.of("~ EnvyWorld ~","Баланс: 119 тыс","» Донат:","§bEnvy","Магазин ~ envyworld.gg"),ranks);
        check(null,List.of("~ EnvyWorld ~","Никнейм: Bodyaha","Ваш остров:","Уровень: 120"),ranks);
        check(null,List.of("~ EnvyWorld ~","Ник: Bodyaha","Онлайн-магазин:","envyworld.gg","Наш айпи:","play.envyworld.gg"),ranks);
        check(null,List.of("Покупка доната: envyworld.gg","Folbe","EnvyWorld"),ranks);
        for(Rank r:ranks)if(r.isDonation()){
            check(r.id,List.of("Донат: "+r.name),ranks);
            if(r.commandsByMode==null||r.signaturesByMode==null||!r.commandsByMode.containsKey("survival"))throw new AssertionError("Missing official mode catalog "+r.id);
        }
        var gate=new ScoreboardRequestGate();Object session=new Object(),level=new Object();int sent=0;
        for(int i=0;i<500;i++)if(gate.tick(session,level,"Хаб",false,true))sent++;
        if(sent!=1)throw new AssertionError("Repeated /sb toggle");
        for(String mode:List.of("Выживание: Европа","Выживание: Ио","ОдинБлок"))for(int i=0;i<500;i++)if(gate.tick(session,level,mode,false,true))sent++;
        if(sent!=4)throw new AssertionError("Per-mode scoreboard request");
        for(int i=0;i<500;i++)if(gate.tick(session,newLevel,"Хаб",true,true))throw new AssertionError("Already visible scoreboard toggled");
        System.out.println("PASS: own scoreboard donation, fonts/aliases, player instead of false Envy, and one /sb request per mode");
    }
    private static final Object newLevel=new Object();
    private static void check(String id,List<String> lines,List<Rank> ranks){Rank r=RankDetector.donationFromSidebar(lines,ranks);if(!Objects.equals(id,r==null?null:r.id))throw new AssertionError(lines+" -> "+(r==null?null:r.id));}
}
