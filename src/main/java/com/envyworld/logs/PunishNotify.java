package com.envyworld.logs;

import com.envyworld.EnvyWorld;
import com.envyworld.client.gui.HudNotices;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

/** Authoritative API events only; initial history, edits and repeated snapshots are silent. */
public final class PunishNotify {
    private static final Identifier SOUND_ID = Identifier.fromNamespaceAndPath(EnvyWorld.MOD_ID, "punish_notify");
    private static final NewLogTracker TRACKER = new NewLogTracker();
    private static final ChatPunishmentParser CHAT=new ChatPunishmentParser();
    private static final NotificationReconciler RECONCILER=new NotificationReconciler();
    private static final java.util.Set<PunishLogClient.Row> acknowledged=new java.util.HashSet<>();
    private static Object session;
    private static int nextLightningId = -2_000_000;
    private PunishNotify() {}
    public static void registerSound() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, SOUND_ID, SoundEvent.createVariableRangeEvent(SOUND_ID));
    }
    public static void tick(Minecraft client) {
        Object connection = client.getConnection();
        if (connection != session) { session = connection; TRACKER.reset();CHAT.reset();RECONCILER.reset();acknowledged.clear(); }
        for(var event:CHAT.flush(System.currentTimeMillis()))chatEvent(client,event);
    }
    public static void observeChat(net.minecraft.network.chat.Component message){var c=Minecraft.getInstance();if(c.player==null||c.getConnection()==null)return;for(var e:CHAT.accept(message.getString(),System.currentTimeMillis()))chatEvent(c,e);}
    private static void chatEvent(Minecraft c,ChatPunishmentParser.Event e){if(RECONCILER.accept(e.type(),e.target(),e.issuer(),e.reason(),e.time(),System.currentTimeMillis(),true))fire(c,e.type(),e.target(),e.issuer(),e.reason(),java.time.Instant.ofEpochMilli(e.time()).atZone(java.time.ZoneId.of("Europe/Moscow")).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),e.duration(),SessionChat.context());}
    public static void onApiBatch(String type, List<PunishLogClient.Row> rows) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player == null || client.getConnection() == null || client.getConnection() != session) return;
            List<PunishLogClient.Row> fresh = TRACKER.accept(type, rows, System.currentTimeMillis() / 1000);
            for (var row : fresh) {
                long when=PunishLogClient.sortTimestamp(row.date())*1000;
                if(RECONCILER.accept(row.type(),row.punished(),row.punisher(),row.reason(),when,System.currentTimeMillis(),false))fire(client, row.type(), row.punished(), row.punisher(), row.reason(),row.date(),row.until(),row.server());
            }
            for(var row:rows)if(acknowledged.add(row)&&!fresh.contains(row))RECONCILER.acknowledge(row.type(),row.punished(),row.punisher(),row.reason(),PunishLogClient.sortTimestamp(row.date())*1000);
            if(acknowledged.size()>50000)acknowledged.clear();
        });
    }
    private static void fire(Minecraft client, String type, String punished, String punisher, String reason, String date, String until, String server) {
        String name = switch (type) { case "mutes" -> "Новый мут"; case "warnings" -> "Новое предупреждение"; default -> "Новый бан"; };
        int color = switch (type) { case "mutes" -> 0xFF9BC6F4; case "warnings" -> 0xFFDAC18C; default -> 0xFFF195A5; };
        String full=name+"\nВыдано: "+date+"\nПолучил: "+punished+"\nВыдал: "+punisher+"\nРежим: "+ServerModes.label(server)+"\nСрок / до: "+(until.isBlank()?"Не указан":until)+"\nПричина: "+reason;
        SessionChat.accept("Наказание",full);
        TelegramNotify.punishment(full);
        if (!EnvyWorld.config().client.logNotifications) return;
        HudNotices.add(name + " · " + punished, "Выдал " + punisher + " · " + reason.replaceAll("\\s+", " "), color);
        playSound(client);
        if (EnvyWorld.config().client.nearbyLightning && client.level != null && client.player != null) {
            var target = client.level.players().stream().filter(player -> player.getName().getString().equalsIgnoreCase(punished)
                    && player.distanceToSqr(client.player) <= 64 * 64).findFirst();
            target.ifPresent(player -> {
                // Client-only visual: no vanilla tick, extra thunder, fire or damage.
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, client.level) {
                    @Override public void tick() { if (++tickCount > 10) discard(); }
                };
                bolt.setVisualOnly(true);
                bolt.setId(nextLightningId--);
                bolt.setPos(player.position());
                client.level.addEntity(bolt);
            });
        }
    }
    public static void playSound(Minecraft client) {
        if (client == null) return;
        var settings = EnvyWorld.config().client;
        float volume = Math.max(0, Math.min(1, settings.notificationVolume));
        if (settings.notificationSound) {
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(SOUND_ID);
            if (event != null) client.getSoundManager().play(SimpleSoundInstance.forUI(event, 1, volume));
        }
        if (settings.thunderSound) client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 1, volume * .6f));
    }
}
