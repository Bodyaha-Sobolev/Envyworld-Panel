package com.envyworld.client;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.player.Input;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryUtil;

/** Owner-initiated Minecraft framebuffer capture and bounded game-only input pulses. */
public final class RemoteGame {
    private static volatile boolean enabled;
    public record Frame(long id,byte[] bytes,long at){}
    private static volatile Frame latest;
    private static final Object frames=new Object();
    private static final java.util.concurrent.atomic.AtomicInteger inFlight=new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicLong sequence=new java.util.concurrent.atomic.AtomicLong();
    private static com.mojang.blaze3d.pipeline.TextureTarget scaled;
    private static float yawPending,pitchPending;
    private static long lookAt,lastCompleted;
    private static long fpsAt;private static int fpsCount;private static volatile int fps;
    public static int fps(){return fps;}
    public static Frame packet(){return enabled?latest:null;}
    public static Frame awaitFrame(long after)throws InterruptedException{synchronized(frames){var f=packet();if(f==null||f.id()<=after)frames.wait(1000);return packet();}}
    public static boolean streaming(){return enabled&&RemoteMiniApp.viewing();}
    private static long captureAt,keyUntil;private static String direction="";
    private static boolean oldPause;
    private static volatile String status="Выключен";
    private static java.util.Set<String> held=java.util.Set.of();
    private static long heldUntil,nextAction;
    private static volatile boolean menuOpen;
    private static volatile long generation;
    private static volatile String position="";
    public static String position(){return position;}
    public static boolean menuOpen(){return menuOpen;}
    private static double pointerX,pointerY;
    private static long pointerUntil;
    public static boolean pointerActive(){return enabled&&menuOpen&&System.currentTimeMillis()<pointerUntil;}
    public static double pointerX(){return pointerX;}
    public static double pointerY(){return pointerY;}
    public static String status(){return status;}
    private static final ExecutorService ENCODE=Executors.newFixedThreadPool(2,r->{var t=new Thread(r,"EnvyWorld remote frame");t.setDaemon(true);return t;});
    private RemoteGame(){}
    public static byte[] frame(){return enabled&&latest!=null?latest.bytes():null;}
    public static void start(){Minecraft.getInstance().execute(()->{var c=Minecraft.getInstance();if(enabled)return;generation++;oldPause=c.options.pauseOnLostFocus;c.options.pauseOnLostFocus=false;enabled=true;latest=null;inFlight.set(0);yawPending=pitchPending=0;captureAt=0;lastCompleted=System.currentTimeMillis();status="Ожидаем первый кадр Minecraft";});}
    public static void stop(){Minecraft.getInstance().execute(()->{generation++;if(enabled)Minecraft.getInstance().options.pauseOnLostFocus=oldPause;enabled=false;direction="";held=java.util.Set.of();heldUntil=0;latest=null;inFlight.set(0);yawPending=pitchPending=0;status="Выключен";synchronized(frames){frames.notifyAll();}if(scaled!=null){scaled.destroyBuffers();scaled=null;}});}
    public static Input input(){if(!enabled)return null;if(Minecraft.getInstance().screen!=null)return Input.EMPTY;if(System.currentTimeMillis()<heldUntil)return new Input(held.contains("w"),held.contains("s"),held.contains("a"),held.contains("d"),held.contains("jump"),held.contains("sneak"),held.contains("sprint"));if(System.currentTimeMillis()>=keyUntil)return null;return new Input(direction.equals("w"),direction.equals("s"),direction.equals("a"),direction.equals("d"),direction.equals("jump"),direction.equals("sneak"),false);}
    public static void tick(){
        if(!enabled)return;var c=Minecraft.getInstance();long now=System.currentTimeMillis();menuOpen=c.screen!=null;
        if(c.player==null||c.getConnection()==null){RemoteMiniApp.stop();return;}
        if(menuOpen){held=java.util.Set.of();heldUntil=0;direction="";keyUntil=0;if(c.player.isSpectator())c.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}
        position=String.format(java.util.Locale.ROOT,"%.3f,%.3f,%.3f",c.player.getX(),c.player.getY(),c.player.getZ());
        if(now>=heldUntil)held=java.util.Set.of();
        if(c.screen==null&&now>=nextAction){nextAction=now+150;if(held.contains("attack"))action("attack");if(held.contains("use"))action("use");}
    }
    public static void inputEvent(com.google.gson.JsonObject event){Minecraft.getInstance().execute(()->{
        if(!enabled)return;var c=Minecraft.getInstance();if(c.player==null)return;
        String type=event.has("type")?event.get("type").getAsString():"";
        switch(type){
            case "keys"->{var keys=new java.util.HashSet<String>();for(var e:event.getAsJsonArray("keys"))if(java.util.Set.of("w","a","s","d","jump","sneak","sprint","attack","use").contains(e.getAsString()))keys.add(e.getAsString());held=java.util.Set.copyOf(keys);heldUntil=System.currentTimeMillis()+700;}
            case "look"->{if(c.screen==null){float dx=event.get("x").getAsFloat(),dy=event.get("y").getAsFloat();if(Float.isFinite(dx)&&Float.isFinite(dy)){yawPending=Math.max(-180,Math.min(180,yawPending+dx));pitchPending=Math.max(-180,Math.min(180,pitchPending+dy));}}}
            case "action"->action(event.get("action").getAsString());
            case "slot"->{int slot=event.get("slot").getAsInt();if(slot>=0&&slot<9)c.player.getInventory().setSelectedSlot(slot);}
            case "key"->{int key=event.get("key").getAsInt();if(c.screen!=null&&(key>=32&&key<=90||java.util.Set.of(256,257,259,261,262,263,264,265,268,269,258).contains(key)))c.screen.keyPressed(new net.minecraft.client.input.KeyEvent(key,0,0));else if(key==256)c.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));}
            case "text"->{if(c.screen!=null){String value=event.get("text").getAsString();if(value.length()<=256)value.codePoints().filter(cp->cp>=32&&cp!=127).forEach(cp->c.screen.charTyped(new net.minecraft.client.input.CharacterEvent(cp,0)));}}
            case "pointer"->{if(c.screen!=null){double x=event.get("x").getAsDouble(),y=event.get("y").getAsDouble();if(!Double.isFinite(x)||!Double.isFinite(y))return;x=Math.max(0,Math.min(1,x))*c.getWindow().getGuiScaledWidth();y=Math.max(0,Math.min(1,y))*c.getWindow().getGuiScaledHeight();int button=event.has("button")?Math.max(0,Math.min(1,event.get("button").getAsInt())):0;var e=new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,event.has("shift")&&event.get("shift").getAsBoolean()?1:0));String phase=event.get("phase").getAsString();if(phase.equals("down"))c.screen.mouseClicked(e,false);else if(phase.equals("up"))c.screen.mouseReleased(e);else if(phase.equals("drag"))c.screen.mouseDragged(e,x-pointerX,y-pointerY);pointerX=x;pointerY=y;pointerUntil=System.currentTimeMillis()+2000;}}
            case "chat"->{if(c.screen==null)c.setScreen(new net.minecraft.client.gui.screens.ChatScreen("",false));}
            default->{}
        }
    });}
    public static com.google.gson.JsonObject profile(String nick)throws Exception{
        var future=new CompletableFuture<com.google.gson.JsonObject>();Minecraft.getInstance().execute(()->{var c=Minecraft.getInstance();var out=new com.google.gson.JsonObject();out.addProperty("nick",nick);var info=c.getConnection()==null?null:c.getConnection().getPlayerInfo(nick);out.addProperty("online",info!=null);out.addProperty("mode",info==null?"—":info.getGameMode().getName());var player=c.level==null?null:c.level.players().stream().filter(p->p.getGameProfile().name().equalsIgnoreCase(nick)).findFirst().orElse(null);out.addProperty("hp",player==null?"—":String.format(java.util.Locale.ROOT,"%.1f / %.1f",player.getHealth(),player.getMaxHealth()));out.addProperty("uuid",info==null?"—":info.getProfile().id().toString());out.add("dupeip",new com.google.gson.Gson().toJsonTree(com.envyworld.client.gui.ProfileChecks.snapshot(nick,"dupeip")));out.add("iphistory",new com.google.gson.Gson().toJsonTree(com.envyworld.client.gui.ProfileChecks.snapshot(nick,"iphistory")));future.complete(out);});return future.get(2,TimeUnit.SECONDS);
    }
    public static void action(String action){Minecraft.getInstance().execute(()->{
        var c=Minecraft.getInstance();if(!enabled||c.player==null||c.getConnection()==null)return;
        switch(action){
            case "w","a","s","d","jump","sneak"->{if(c.screen!=null)c.setScreen(null);direction=action;keyUntil=System.currentTimeMillis()+400;}
            case "stop"->{direction="";keyUntil=0;held=java.util.Set.of();heldUntil=0;}
            case "left"->c.player.setYRot(c.player.getYRot()-20);
            case "right"->c.player.setYRot(c.player.getYRot()+20);
            case "up"->c.player.setXRot(Math.max(-90,c.player.getXRot()-15));
            case "down"->c.player.setXRot(Math.min(90,c.player.getXRot()+15));
            case "attack"->{if(c.screen==null)((com.envyworld.mixin.MinecraftRemoteInvoker)c).envyworld$attack();}
            case "use"->{if(c.screen==null)((com.envyworld.mixin.MinecraftRemoteInvoker)c).envyworld$use();}
            case "inventory"->{if(c.screen==null)c.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.player));else c.setScreen(null);}
            case "slotprev"->c.player.getInventory().setSelectedSlot(Math.floorMod(c.player.getInventory().getSelectedSlot()-1,9));
            case "slotnext"->c.player.getInventory().setSelectedSlot((c.player.getInventory().getSelectedSlot()+1)%9);
            default->{}
        }
    });}
    public static void smoothLook(){
        long now=System.nanoTime();double dt=lookAt==0?1.0/60:Math.min(.05,(now-lookAt)/1e9);lookAt=now;
        var c=Minecraft.getInstance();if(!enabled||c.player==null||c.screen!=null){yawPending=pitchPending=0;return;}
        float amount=(float)(1-Math.exp(-dt/.045));float yaw=yawPending*amount,pitch=pitchPending*amount;
        yawPending-=yaw;pitchPending-=pitch;c.player.setYRot(c.player.getYRot()+yaw);
        float desired=c.player.getXRot()+pitch;c.player.setXRot(Math.max(-90,Math.min(90,desired)));if(desired<=-90||desired>=90)pitchPending=0;
    }
    public static void capture(){
        var c=Minecraft.getInstance();long now=System.currentTimeMillis();if(!enabled)return;if(c.player==null||c.getConnection()==null){stop();return;}
        if(inFlight.get()>0&&now-lastCompleted>5000){generation++;inFlight.set(0);lastCompleted=now;status="Повторный захват кадра…";}
        long nano=System.nanoTime(),period=RemoteMiniApp.viewing()?16666667L:1800000000L;
        if(nano<captureAt||inFlight.get()>=3)return;
        captureAt=captureAt==0||nano-captureAt>period? nano+period:captureAt+period;
        long ticket=generation,id=sequence.incrementAndGet();inFlight.incrementAndGet();
        try{
            var source=c.getMainRenderTarget();int width=Math.min(960,source.width),height=Math.max(1,Math.round((float)source.height*width/source.width));
            if(scaled==null)scaled=new com.mojang.blaze3d.pipeline.TextureTarget("EnvyWorld remote",width,height,false);else if(scaled.width!=width||scaled.height!=height)scaled.resize(width,height);
            source.blitAndBlendToTexture(scaled.getColorTextureView());
            // GPU resize works for odd window sizes too; Screenshot's integer downscale rejects those.
            Screenshot.takeScreenshot(scaled,1,image->ENCODE.execute(()->{
                try(image){
                    var bytes=new ByteArrayOutputStream(65536);try(var writer=STBIWriteCallback.create((context,data,size)->{byte[] chunk=new byte[size];MemoryUtil.memByteBuffer(data,size).get(chunk);bytes.writeBytes(chunk);})){
                        if(STBImageWrite.nstbi_write_jpg_to_func(writer.address(),0,image.getWidth(),image.getHeight(),4,image.getPointer(),65)==0)throw new IllegalStateException("JPEG encode failed");
                    }
                    synchronized(frames){
                        if(enabled&&generation==ticket&&(latest==null||id>latest.id())){
                            long at=System.currentTimeMillis();latest=new Frame(id,bytes.toByteArray(),at);lastCompleted=at;
                            if(at-fpsAt>=1000){fps=fpsCount;fpsCount=0;fpsAt=at;}fpsCount++;
                            status="Экран передаётся: "+fps+" кадр/с";frames.notifyAll();
                        }
                    }
                }catch(Exception e){if(generation==ticket)status="Ошибка кодирования кадра: "+e.getClass().getSimpleName();}
                finally{if(generation==ticket)inFlight.decrementAndGet();}
            }));
        }catch(RuntimeException e){if(generation==ticket)inFlight.decrementAndGet();status="Не удалось захватить кадр: "+e.getMessage();}
    }
}
