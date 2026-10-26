package com.envyworld.logs;

import com.google.gson.JsonObject;

/** A single audible punishment notice, separate from the silently edited dashboard. */
public final class TelegramAlert {
    private TelegramAlert(){}
    public static String replace(TelegramPanel.Transport transport,String owner,String previous,String notice)throws Exception{
        if(previous!=null&&!previous.isBlank()){var old=new JsonObject();old.addProperty("chat_id",owner);old.addProperty("message_id",Long.parseLong(previous));try{transport.call("deleteMessage",old);}catch(TelegramPanel.ApiFailure e){if(e.code!=400)throw e;}}
        var body=new JsonObject();body.addProperty("chat_id",owner);body.addProperty("parse_mode","HTML");
        if(notice.length()>1800)notice=notice.substring(0,1800)+"…";String clean=notice.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        body.addProperty("text","<a href=\"tg://user?id="+owner+"\">Пришло новое наказание</a>\n"+clean);body.addProperty("disable_notification",false);
        return transport.call("sendMessage",body).getAsJsonObject().get("message_id").getAsString();
    }
}
