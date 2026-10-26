package com.envyworld.client;
import com.envyworld.EnvyWorld;
import com.envyworld.client.gui.ProfileChecks;
import java.util.List;
import net.minecraft.client.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
/** Targeted query bindings; results belong above the player's nametag, never in a separate HUD. */
public final class TargetInspector {
 private static KeyMapping toggle,dupe,history,profile;
 private TargetInspector(){}
 public static List<String> lines(){return List.of();}
 public static void register(){toggle=key("key.envyworld.inspect");dupe=key("key.envyworld.dupeip");history=key("key.envyworld.iphistory");profile=key("key.envyworld.target_profile");}
 private static KeyMapping key(String name){return KeyBindingHelper.registerKeyBinding(new KeyMapping(name,com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,-1,KeyMapping.Category.MISC));}
 public static String bindingName(boolean ips){return keyName(ips?history:dupe);}
 public static void bind(boolean ips,int code){
  var key=ips?history:dupe;if(key==null)return;
  key.setKey(code==0||code==-1?com.mojang.blaze3d.platform.InputConstants.UNKNOWN:code<=-2?com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(-code-2):com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(code));
  KeyMapping.resetMapping();Minecraft.getInstance().options.save();
 }
 public static void tick(Minecraft c){
  while(toggle.consumeClick()){EnvyWorld.config().client.espChecks=!EnvyWorld.config().client.espChecks;EnvyWorld.saveConfig();}
  if(c.player==null||c.getConnection()==null||c.screen!=null||c.gameMode==null||c.gameMode.getPlayerMode()!=net.minecraft.world.level.GameType.SPECTATOR){drain(dupe);drain(history);drain(profile);return;}
  Player player=com.envyworld.client.gui.SpectatorOverlay.active()?com.envyworld.client.gui.SpectatorOverlay.target():c.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof Player p?p:null;
  if(player!=null&&PlayerIdentityFilter.npc(player,c))player=null;
  if(player==null){drain(dupe);drain(history);drain(profile);return;}
  String nick=player.getGameProfile().name();
  while(dupe.consumeClick())ProfileChecks.request(nick,"dupeip");
  while(history.consumeClick())ProfileChecks.request(nick,"iphistory");
  if(profile.consumeClick()){var menu=new com.envyworld.client.gui.EnvyScreen();c.setScreen(menu);menu.openPlayer(nick);}
 }
 private static void drain(KeyMapping key){if(key!=null)while(key.consumeClick()){} }
 private static String keyName(KeyMapping key){return key==null||key.isUnbound()?"—":key.getTranslatedKeyMessage().getString();}
}
