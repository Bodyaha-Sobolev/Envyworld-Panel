package com.envyworld.logs;

import java.util.regex.Pattern;

/** Server formats observed in the local EnvyWorld session journal. */
public final class ChatFilter {
    public record Message(String category,String text){}
    private static final Pattern SPY=Pattern.compile("^([A-Za-z0-9_]{1,16}):\\s*(/.*)$");
    private static final Pattern AD=Pattern.compile("(?iu)^\\(EW\\)\\s*(?:хочешь найти|мешает табло|какими способами|приобрести донат|у нас на сервере|вам нужна помощь|покупая привилегию|с помощью команды /prefix|не ходите на сервера|на нашем сервере|покупай донат|не забудь|напоминаем).*");
    private long adUntil;
    public Message classify(String kind,String raw,long now){
        String text=raw.replaceAll("§[0-9a-fk-orA-FK-OR]","").replace("\r","");String flat=text.strip();
        if(flat.isEmpty())return null;
        if(kind.equals("Наказание"))return new Message(kind,flat);
        if(kind.equals("Команда"))return new Message(kind,redact(flat));
        if(kind.equals("Отправлено"))return new Message(kind,flat);
        if(AD.matcher(flat).matches()){adUntil=now+1800;return null;}
        if(now<adUntil&&Character.isWhitespace(text.charAt(0)))return null;
        if(flat.matches("(?isu)^\\(EW\\) Игрок .* (?:заткнул|заблокировал|предупредил) .*"))return null;
        if(flat.matches("(?isu)^(?:Время:|Причина:|Список предупреждений:|- Предупрежд[её]н).*$"))return null;
        var spy=SPY.matcher(flat);if(spy.matches())return new Message("Команды игроков",spy.group(1)+": "+redact(spy.group(2).replaceFirst("^/cmi\\s+","/")));
        if(flat.startsWith("Друзья »"))return new Message("Друзья",flat.substring("Друзья »".length()).strip());
        if(flat.matches("^\\[[^\\]]+ (?:->|→) [^\\]]+\\].*"))return new Message("Личные",flat);
        int arrow=flat.indexOf('➥');if(arrow>0){String prefix=flat.substring(0,arrow).strip();String nick=prefix.substring(prefix.lastIndexOf(' ')+1);return new Message("Чат",nick+": "+flat.substring(arrow+1).strip());}
        return new Message(kind.equals("Чат")?"Чат":"События",flat.replaceFirst("^\\(EW\\)\\s*",""));
    }
    public static String redact(String text){return text.matches("(?isu)^/(?:login|l|reg|register|changepassword|changepass|cp|password|telegram)\\b.*")?text.split("\\s+",2)[0]+" [скрыто]":text;}
}
