package com.envyworld.logs;

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Telegram Mini App initData signature, freshness and exact owner verification. */
public final class TelegramAuth {
    private TelegramAuth(){}
    private static byte[] hmac(byte[] key,String data)throws Exception{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));}
    public static boolean verify(String data,String token,String owner,long now){
        try{
            var fields=new TreeMap<String,String>();
            for(String pair:data.split("&")){String[] parts=pair.split("=",2);if(parts.length!=2)return false;String key=URLDecoder.decode(parts[0],StandardCharsets.UTF_8);if(fields.put(key,URLDecoder.decode(parts[1],StandardCharsets.UTF_8))!=null)return false;}
            String hash=fields.remove("hash");if(hash==null||!hash.matches("[0-9a-fA-F]{64}"))return false;
            long when=Long.parseLong(fields.get("auth_date"));if(when>now+30||now-when>3600)return false;
            String check=fields.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("\n"));
            byte[] key=hmac("WebAppData".getBytes(StandardCharsets.UTF_8),token);
            if(!MessageDigest.isEqual(hmac(key,check),HexFormat.of().parseHex(hash)))return false;
            return owner.equals(com.google.gson.JsonParser.parseString(fields.get("user")).getAsJsonObject().get("id").getAsString());
        }catch(Exception invalid){return false;}
    }
}
