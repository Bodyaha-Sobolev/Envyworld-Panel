package com.envyworld.client.gui;

import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;

/** Account rows retain the server's text colours; the legend is never an account. */
public final class DupeIpResult {
    public record Account(String nick,int color,String status){}
    private static final Pattern NICK=Pattern.compile("[A-Za-z0-9_]{1,16}");
    private DupeIpResult(){}
    public static Component slice(Component component,int start,int end){
        var out=Component.empty();int[] offset={0};
        component.visit((style,text)->{int from=Math.max(0,start-offset[0]),to=Math.min(text.length(),end-offset[0]);if(to>from)out.append(Component.literal(text.substring(from,to)).setStyle(style));offset[0]+=text.length();return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        return out;
    }
    public static List<Account> accounts(Component component){
        String raw=component.getString();
        String body=raw.replaceAll("\\([^)]*\\)"," ").trim().replaceFirst("^(?:[-•*]\\s*|[0-9]{1,5}[.)]\\s*)","").replaceFirst("[.,]$","");
        if(!body.matches("[A-Za-z0-9_]{1,16}(?:\\s*[,;|]\\s*[A-Za-z0-9_]{1,16})*"))return List.of();
        var colours=new ArrayList<Integer>(raw.length());
        component.visit((style,text)->{int color=style.getColor()==null?0xB5BDC9:style.getColor().getValue();for(int i=0;i<text.length();i++)colours.add(0xFF000000|color);return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        var result=new ArrayList<Account>();var matcher=NICK.matcher(body);int search=0;
        while(matcher.find()){
            String nick=matcher.group();int at=raw.indexOf(nick,search);if(at<0)continue;search=at+nick.length();
            int color=at<colours.size()?colours.get(at):0xFFB5BDC9;
            result.add(new Account(nick,color,status(color)));
        }
        return List.copyOf(result);
    }
    private static String status(int color){
        int rgb=color&0xFFFFFF;
        return switch(rgb){case 0xFFAA00->"мут";case 0xFF5555,0xFF0000->"бан";case 0xAA0000->"бан по IP";case 0x55FF55,0x00AA00,0x00FF00->"в сети";case 0xAAAAAA,0x555555->"не в сети";default->"аккаунт";};
    }
    public static String summary(String subject,Collection<Account> accounts){
        long others=accounts.stream().filter(a->!a.nick().equalsIgnoreCase(subject)).count();
        if(others==0)return "Другие аккаунты не найдены";
        long muted=accounts.stream().filter(a->a.status().equals("мут")).count();
        long banned=accounts.stream().filter(a->a.status().startsWith("бан")).count();
        return "Других аккаунтов: "+others+(muted>0?" · мут: "+muted:"")+(banned>0?" · бан: "+banned:"");
    }
}
