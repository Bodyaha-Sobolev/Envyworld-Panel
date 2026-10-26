package com.envyworld.client;

import com.envyworld.EnvyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import java.util.concurrent.*;

/** Bounded client rendering and a network-only escape from a stalled game thread. */
public final class ClientProtection {
    private record Watch(Connection connection,Thread thread,int seconds){}
    private static final FreezeDetector detector=new FreezeDetector();
    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet rendered=new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
    private static volatile Watch watch;
    private static ScheduledExecutorService worker;
    private static final BurstBudget particles=new BurstBudget(),sounds=new BurstBudget();
    private ClientProtection(){}
    public static synchronized void start(){
        if(worker!=null)return;
        worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"EnvyWorld freeze guard");t.setDaemon(true);return t;});
        worker.scheduleAtFixedRate(ClientProtection::check,250,250,TimeUnit.MILLISECONDS);
    }
    public static synchronized void close(){watch=null;detector.beat(System.nanoTime(),false);if(worker!=null){worker.shutdownNow();worker=null;}}
    public static void heartbeat(){
        var c=Minecraft.getInstance();var s=EnvyWorld.config().client;
        var listener=c.getConnection();var connection=listener==null?null:listener.getConnection();
        boolean active=s.clientProtection&&s.freezeDisconnect&&c.player!=null&&c.level!=null&&connection!=null&&connection.isConnected()&&!c.hasSingleplayerServer()&&c.getOverlay()==null;
        watch=active?new Watch(connection,Thread.currentThread(),Math.max(3,Math.min(120,s.freezeTimeoutSeconds))):null;
        detector.beat(System.nanoTime(),active);
    }
    public static void beginFrame(){rendered.clear();heartbeat();}
    private static void check(){
        var expected=watch;if(expected==null)return;
        if(!detector.poll(System.nanoTime(),TimeUnit.SECONDS.toNanos(expected.seconds)))return;
        if(watch!=expected||!expected.connection.isConnected())return;
        try{
            EnvyWorld.LOGGER.warn("Freeze protection: game thread stalled for at least {} seconds; closing server connection",expected.seconds);
            var trace=expected.thread.getStackTrace();StringBuilder report=new StringBuilder("EnvyWorld client freeze; "+java.time.Instant.now()+"; timeout "+expected.seconds+" seconds; thread "+expected.thread.getState()+"\n");
            for(int i=0;i<Math.min(40,trace.length);i++)report.append("at ").append(trace[i]).append('\n');
            // Connection.disconnect closes Netty's channel; no world/GUI work runs on this worker.
            expected.connection.disconnect(Component.literal("Защита EnvyWorld: клиент не отвечал более "+expected.seconds+" секунд."));
            Minecraft.getInstance().execute(()->{
                var c=Minecraft.getInstance();var listener=c.getConnection();
                if(listener!=null&&listener.getConnection()==expected.connection)c.disconnect(new net.minecraft.client.gui.screens.TitleScreen(),false);
            });
            // Disconnect first: slow disk diagnostics must not delay the escape path.
            try{var file=Minecraft.getInstance().gameDirectory.toPath().resolve("logs/envyworld-freeze.txt");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,report);}catch(java.io.IOException e){EnvyWorld.LOGGER.warn("Could not write freeze diagnostic",e);}
        }catch(RuntimeException e){EnvyWorld.LOGGER.warn("Freeze protection could not close the connection",e);}
    }
    public static boolean allowEntity(net.minecraft.world.entity.Entity entity){
        var c=Minecraft.getInstance();var s=EnvyWorld.config().client;
        if(!s.clientProtection||entity instanceof net.minecraft.world.entity.player.Player||c.level==null||c.level.getEntityCount()<1024)return true;
        if(rendered.contains(entity.getId()))return true;
        if(rendered.size()>=Math.max(64,Math.min(2048,s.entityRenderLimit)))return false;
        rendered.add(entity.getId());return true;
    }
    public static boolean allowParticle(){
        if(!EnvyWorld.config().client.clientProtection)return true;
        return particles.take(System.nanoTime(),2000);
    }
    public static boolean allowSound(net.minecraft.client.resources.sounds.SoundInstance sound){
        if(!EnvyWorld.config().client.clientProtection)return true;
        // AbstractSoundInstance volume/pitch depend on resolve(); do not read them at admission.
        return sound!=null&&sounds.take(System.nanoTime(),256);
    }
    public static boolean finiteSound(net.minecraft.client.resources.sounds.SoundInstance sound){
        return sound!=null&&Float.isFinite(sound.getVolume())&&Float.isFinite(sound.getPitch())
                &&Double.isFinite(sound.getX())&&Double.isFinite(sound.getY())&&Double.isFinite(sound.getZ());
    }
}
