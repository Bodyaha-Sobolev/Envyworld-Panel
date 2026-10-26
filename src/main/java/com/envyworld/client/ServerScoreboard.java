package com.envyworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/** Keep server data enabled; the user's toggle changes only the local HUD. */
public final class ServerScoreboard {
    private static Object connection;
    private static final ScoreboardRequestGate requests=new ScoreboardRequestGate();
    private static boolean internalCommand,hidden;
    private ServerScoreboard(){}
    public static Objective sidebar(Minecraft client){
        if(client.player==null||client.level==null)return null;
        Scoreboard board=client.level.getScoreboard();
        var team=board.getPlayersTeam(client.player.getScoreboardName());
        var slot=team==null?null:DisplaySlot.teamColorToSlot(team.getColor());
        Objective objective=slot==null?null:board.getDisplayObjective(slot);
        return objective!=null?objective:board.getDisplayObjective(DisplaySlot.SIDEBAR);
    }
    private static boolean supported(Minecraft client){
        var server=client.getCurrentServer();
        return server!=null&&server.ip.toLowerCase(java.util.Locale.ROOT).matches("(?:[a-z0-9-]+\\.)*envyworld\\.gg(?::\\d+)?");
    }
    public static void tick(Minecraft client){
        if(client.getConnection()!=connection){connection=client.getConnection();hidden=false;}
        if(client.player==null||!supported(client))return;
        String mode=com.envyworld.ranks.RankDetector.detectedMode();
        if(!requests.tick(connection,client.level,mode,sidebar(client)!=null,connection!=null))return;
        internalCommand=true;
        try{client.getConnection().sendCommand("sb");}
        finally{internalCommand=false;}
    }
    public static boolean intercept(String command){
        var client=Minecraft.getInstance();
        if(internalCommand||!supported(client)||client.player==null)return false;
        if(!command.trim().equalsIgnoreCase("sb")&&!command.trim().equalsIgnoreCase("scoreboard"))return false;
        hidden=!hidden;
        client.player.displayClientMessage(Component.literal(hidden
                ?"Scoreboard визуально отключён через клиент; данные продолжают поступать."
                :"Scoreboard визуально включён через клиент; данные продолжают поступать."),false);
        return true;
    }
    public static boolean hidden(){return hidden;}
}
