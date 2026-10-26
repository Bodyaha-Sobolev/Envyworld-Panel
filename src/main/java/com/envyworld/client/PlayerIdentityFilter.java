package com.envyworld.client;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
/** Conservative NPC evidence; absent TAB, zero ping or immobility alone never hide real vanished staff. */
public final class PlayerIdentityFilter {
    private PlayerIdentityFilter(){}
    public static boolean npc(Player p,Minecraft c){
        String name=p.getGameProfile().name();
        if(!name.matches("[A-Za-z0-9_]{1,16}"))return true;
        if(p.getTags().stream().anyMatch(t->t.equalsIgnoreCase("npc")||t.equalsIgnoreCase("citizens")||t.equalsIgnoreCase("bot")))return true;
        var info=c.getConnection()==null?null:c.getConnection().getPlayerInfo(p.getUUID());
        return info==null&&p.getUUID().version()==2;
    }
}
