package com.envyworld.client;
import com.envyworld.client.gui.*;
import com.envyworld.items.ItemText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public final class CompatProbe {
 static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 static MouseButtonEvent mouse(double x,double y){return new MouseButtonEvent(x,y,new MouseButtonInfo(0,0));}
 static int[] colors(String text){var c=ItemText.parse(text,"LIGHT_PURPLE");var list=new java.util.ArrayList<Integer>();c.visit((style,value)->{for(int i=0;i<value.length();i++)list.add(style.getColor()==null?-1:style.getColor().getValue());return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY);return list.stream().mapToInt(Integer::intValue).toArray();}
 static void shot(Minecraft c,String name){net.minecraft.client.Screenshot.grab(c.gameDirectory,name,c.getMainRenderTarget(),1,msg->System.out.println("CLEAN_UI_SHOT "+name));}
 static void worldFrame(Minecraft c,String name)throws Exception{
  var f=UiRenderer.class.getDeclaredField("worldCopy");f.setAccessible(true);Object texture=f.get(null);check(texture!=null,"Transition world snapshot missing");
  var method=texture.getClass().getDeclaredMethod("getTextureId");method.setAccessible(true);int id=(int)method.invoke(texture);int w=c.getMainRenderTarget().width,h=c.getMainRenderTarget().height;
  var state=com.envyworld.graphics.util.GlRenderStateSnapshot.captureRenderState();var pixels=org.lwjgl.BufferUtils.createByteBuffer(w*h*3);
  try{org.lwjgl.opengl.GL33.glBindTexture(org.lwjgl.opengl.GL33.GL_TEXTURE_2D,id);org.lwjgl.opengl.GL33.glGetTexImage(org.lwjgl.opengl.GL33.GL_TEXTURE_2D,0,org.lwjgl.opengl.GL33.GL_RGB,org.lwjgl.opengl.GL33.GL_UNSIGNED_BYTE,pixels);}finally{state.restore();}
  var img=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=((h-1-y)*w+x)*3;img.setRGB(x,y,(pixels.get(i)&255)<<16|(pixels.get(i+1)&255)<<8|pixels.get(i+2)&255);}
  javax.imageio.ImageIO.write(img,"png",new java.io.File(c.gameDirectory,"screenshots/"+name));
 }
 public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
  int ticks,title,world=-1;long closingAt;boolean creating;EnvyScreen menu;ItemEditorScreen editor;ColorPickerPopup popup;String original="Альфа Бета Гамма";int fx,fy,px,py;
  void open(Minecraft c)throws Exception{editor.mouseClicked(mouse(fx+15,fy+42),false);popup=(ColorPickerPopup)field(editor,"colorPopup");check(popup!=null&&c.screen==editor,"Picker must stay inside editor");px=(int)field(popup,"x");py=(int)field(popup,"y");}
  public void onEndTick(Minecraft c){try{
   ticks++;if(ticks>1800)throw new AssertionError("CLEAN_UI_TIMEOUT");
   if(!creating&&c.screen instanceof net.minecraft.client.gui.screens.TitleScreen&&c.getOverlay()==null){title++;if(title==3){c.getWindow().setWindowed(1920,1080);c.options.guiScale().set(2);c.options.enableVsync().set(false);c.options.framerateLimit().set(120);c.resizeDisplay();}if(title==10){creating=true;net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.testWorld(c,()->c.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));}}
   if(creating&&world<0&&c.screen instanceof net.minecraft.client.gui.screens.worldselection.CreateWorldScreen create){create.getUiState().setName("EnvyCleanUI "+System.currentTimeMillis());create.getUiState().setAllowCommands(true);var m=create.getClass().getDeclaredMethod("onCreate");m.setAccessible(true);m.invoke(create);}
   if(creating&&world<0&&c.player!=null&&c.screen==null){world=ticks;System.out.println("CLEAN_UI_WORLD_READY");}
   if(world>0){int t=ticks-world;
    if(t==5){c.player.setXRot(0);menu=new EnvyScreen();c.setScreen(menu);}
    if(t==15){c.player.setXRot(-25);}
    if(t==20)shot(c,"menu-sky-in-world.png");
    if(t==21)c.player.setXRot(25);
    if(t==24)shot(c,"menu-ground-in-world.png");
    if(t==25){var preset=new com.envyworld.items.CustomItemPreset();preset.name=original;editor=new ItemEditorScreen(menu,preset,s->{});c.setScreen(editor);fx=(int)field(editor,"formX");fy=(int)field(editor,"formY");var form=field(editor,"textForm");var list=(java.util.List<?>)field(form,"fields");var name=(net.minecraft.client.gui.components.EditBox)list.getFirst();name.moveCursorTo(6,false);name.moveCursorTo(10,true);}
    if(t==30)shot(c,"editor-in-world.png");
    if(t==35)open(c);
    if(t==45){check(c.screen==editor,"Picker opened another screen");shot(c,"picker-in-editor-in-world.png");editor.mouseClicked(mouse(px+30,py+45),false);editor.mouseDragged(mouse(px+50,py+60),20,15);editor.mouseReleased(mouse(px+50,py+60));var cs=colors(editor.draft().name);check(ItemText.parse(editor.draft().name,"LIGHT_PURPLE").getString().equals(original)&&cs[0]==cs[11]&&cs[6]!=cs[0],"Picker drag must change only the selected word");System.out.println("CLEAN_UI_LIVE_SELECTION_PASS");}
    if(t==50){shot(c,"picker-live-selection.png");editor.keyPressed(new KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0));check(editor.draft().name.equals(original),"Escape did not restore the original selected text");}
    if(t==51)shot(c,"picker-closing-opaque.png");
    if(t==65){check(field(editor,"colorPopup")==null,"Popup did not finish closing");open(c);((net.minecraft.client.gui.components.EditBox)field(popup,"input")).setValue("#123456");var cs=colors(editor.draft().name);check(cs[6]==0x123456&&cs[0]==cs[11]&&cs[0]!=cs[6],"HEX live update lost selection");editor.mouseClicked(mouse(10,c.getWindow().getGuiScaledHeight()-50),false);}
    if(t==80){check(field(editor,"colorPopup")==null&&colors(editor.draft().name)[6]==0x123456,"Outside click must commit inline color");shot(c,"picker-committed-in-world.png");open(c);editor.mouseClicked(mouse(px+15,py+125),false);((net.minecraft.client.gui.components.EditBox)field(popup,"input")).setValue("220, 30, 90");check(colors(editor.draft().name)[6]==0xDC1E5A,"RGB live value");editor.mouseClicked(mouse(px+15,py+125),false);((net.minecraft.client.gui.components.EditBox)field(popup,"input")).setValue("120, 100%, 100%");check(colors(editor.draft().name)[6]==0x00FF00,"HSV live value");
      editor.mouseClicked(mouse(px+15,py+125),false);((net.minecraft.client.gui.components.EditBox)field(popup,"input")).setValue("240, 100%, 50%");check(colors(editor.draft().name)[6]==0x0000FF,"HSL live value");
      var clipboard=c.keyboardHandler.getClipboard();try{editor.mouseClicked(mouse(px+69,py+142),false);check(c.keyboardHandler.getClipboard().startsWith("240"),"Copy formatted color");}finally{c.keyboardHandler.setClipboard(clipboard);}
      editor.keyPressed(new KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0));System.out.println("CLEAN_UI_HEX_RGB_HSV_HSL_COPY_CANCEL_OUTSIDE_PASS");}
    if(t==95){c.setScreen(menu);menu.openSettings();}
    if(t==100){c.setScreen(new HudEditorScreen(menu));}
    if(t==105){var cfg=com.envyworld.EnvyWorld.config().client;boolean before=cfg.macros.visible;int x=c.getWindow().getGuiScaledWidth()-170;c.screen.mouseClicked(mouse(x+140,79),false);check(cfg.macros.visible!=before,"HUD selector toggle");c.screen.mouseClicked(mouse(x+80,171),false);c.screen.mouseDragged(mouse(x+150,171),70,0);c.screen.mouseReleased(mouse(x+150,171));check(cfg.hudOpacity==255,"HUD opacity drag");c.screen.mouseClicked(mouse(x+140,79),false);cfg.hudOpacity=185;cfg.hudBlur=true;
      c.screen.mouseClicked(mouse(x+70,255),false);check(cfg.macros.anchorX==1&&cfg.macros.anchorY==1,"Center HUD preset");
      var box=EnvyHud.boxes(c.getWindow().getGuiScaledWidth(),c.getWindow().getGuiScaledHeight(),true).stream().filter(b->b.id().equals("macros")).findFirst().orElseThrow();check(Math.abs(box.x()+box.width()/2-c.getWindow().getGuiScaledWidth()/2f)<1,"Preset centered by size");
      c.screen.mouseClicked(mouse(x+115,238),false);check(cfg.macros.anchorX==2&&cfg.macros.anchorY==0,"Top right HUD preset");
      c.screen.mouseClicked(mouse(x+70,255),false);System.out.println("CLEAN_UI_HUD_SELECTOR_PASS");}
    if(t==110)shot(c,"hud-selector-in-world.png");
    if(t==115){menu=new EnvyScreen();c.setScreen(menu);}
    if(t==116)shot(c,"menu-opening-composite.png");
    if(t==117)shot(c,"menu-opening-composite-2.png");
    if(t==118)shot(c,"menu-opening-composite-3.png");
    if(t==124)shot(c,"menu-fully-open-composite.png");
    if(t==125){closingAt=System.nanoTime();c.screen.keyPressed(new KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0));check(c.screen==menu&&(boolean)field(menu,"isClosing"),"Close must animate the complete menu");}
    if(t==126){shot(c,"menu-closing-composite-early.png");}
    if(t==128){shot(c,"menu-closing-composite-late.png");}
    if(t==140){if(c.screen!=null&&System.nanoTime()-closingAt<2_000_000_000L){world++;return;}check(c.screen==null,"Closing animation must finish without orphan controls");shot(c,"closed-menu-coloured-world.png");System.out.println("CLEAN_UI_COMPOSITE_ANIMATION_PASS");}
    if(t>=150&&t<195){int index=(t-150)/5;if((t-150)%5==0){menu=new EnvyScreen();c.setScreen(menu);var method=EnvyScreen.class.getDeclaredMethod("switchTab",int.class);method.setAccessible(true);method.invoke(menu,index);}
      if((t-150)%5==4)shot(c,"navigation-tab-"+index+".png");}
    if(t==197){System.out.println("CLEAN_UI_ALL_TABS_PASS");c.setScreen(null);var cfg=com.envyworld.EnvyWorld.config().client;cfg.hudEnabled=true;cfg.hudBlur=true;cfg.hudOpacity=185;cfg.online.visible=true;cfg.online.background=true;cfg.online.x=.55f;cfg.online.y=.25f;cfg.online.anchorX=-1;cfg.online.anchorY=-1;cfg.watchedPlayers.clear();cfg.watchedPlayers.add("FixturePlayer");cfg.notices.visible=true;cfg.notices.background=true;cfg.notices.x=.25f;cfg.notices.y=.25f;cfg.notices.anchorX=-1;cfg.notices.anchorY=-1;HudNotices.add("Проверка уведомления","Обычный HUD вне редактора",0xFF87D9B0);c.player.setXRot(-25);}
    if(t==205)shot(c,"ordinary-hud-sky.png");
    if(t==210)c.player.setXRot(25);
    if(t==218)shot(c,"ordinary-hud-ground.png");
    if(t==236)shot(c,"ordinary-hud-late.png");
    if(t==240){System.out.println("CLEAN_UI_ORDINARY_HUD_PASS");System.out.println("CLEAN_UI_ALL_PASS");c.stop();}
   }
  }catch(Throwable e){System.err.println("CLEAN_UI_FAILED");e.printStackTrace();c.stop();throw new RuntimeException(e);}}
 });}
}
