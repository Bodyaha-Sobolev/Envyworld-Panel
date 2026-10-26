package com.envyworld.client;
/** One automatic start per connected world/account; an explicit close remains closed until reconnect. */
public final class MiniAppLifecycle {
    private Object connection;private String owner="";
    public void tick(Object current,String account,boolean enabled,Runnable start,Runnable stop){
        if(current==null||!enabled){if(connection!=null)stop.run();connection=null;owner="";return;}
        if(connection!=current||!owner.equals(account)){if(connection!=null)stop.run();connection=current;owner=account;start.run();}
    }
}
