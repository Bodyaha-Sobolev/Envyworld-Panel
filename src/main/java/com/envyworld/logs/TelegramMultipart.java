package com.envyworld.logs;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class TelegramMultipart {
    private TelegramMultipart(){}
    public static byte[] encode(JsonObject fields,byte[] png,String boundary)throws IOException{
        var out=new ByteArrayOutputStream();
        for(var entry:fields.entrySet()){
            if(!entry.getKey().matches("[a-z_]+"))throw new IllegalArgumentException("field");
            String value=entry.getValue().isJsonPrimitive()?entry.getValue().getAsString():entry.getValue().toString();
            out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+entry.getKey()+"\"\r\n\r\n"+value+"\r\n").getBytes(StandardCharsets.UTF_8));
        }
        boolean jpeg=png.length>2&&(png[0]&255)==255&&(png[1]&255)==216;
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"frame\"; filename=\"minecraft."+(jpeg?"jpg":"png")+"\"\r\nContent-Type: image/"+(jpeg?"jpeg":"png")+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(png);out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));return out.toByteArray();
    }
}
