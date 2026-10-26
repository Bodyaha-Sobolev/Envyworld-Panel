package com.envyworld.logs;

import com.envyworld.EnvyWorld;
import com.envyworld.client.gui.HudNotices;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class OnlineWatch {
    public record Presence(boolean known, boolean online, long changedAt) {}
    private static final Map<String, Presence> states = new HashMap<>();
    private static Object session;
    private static long nextCheck;
    private OnlineWatch() {}
    public static boolean watched(String name) { return EnvyWorld.config().client.watchedPlayers.stream().anyMatch(value -> value.equalsIgnoreCase(name)); }
    public static boolean toggle(String name) {
        if (!name.matches("[A-Za-z0-9_]{1,16}")) return false;
        var values = EnvyWorld.config().client.watchedPlayers;
        boolean removing = watched(name);
        if (removing) values.removeIf(value -> value.equalsIgnoreCase(name)); else values.add(name);
        EnvyWorld.saveConfig(); return !removing;
    }
    public static Presence presence(String name) { return states.getOrDefault(name.toLowerCase(Locale.ROOT), new Presence(false, false, 0)); }
    public static void tick(Minecraft client) {
        if (client.getConnection() != session) { session = client.getConnection(); states.clear(); nextCheck = 0; }
        if (client.player == null || client.getConnection() == null || System.currentTimeMillis() < nextCheck) return;
        nextCheck = System.currentTimeMillis() + 500;
        var visible = new HashSet<String>();
        for (var info : client.getConnection().getListedOnlinePlayers()) visible.add(info.getProfile().name().toLowerCase(Locale.ROOT));
        var network = new HashSet<String>();
        for (String value : OnlineScanner.online()) network.add(value.toLowerCase(Locale.ROOT));
        network.addAll(visible);
        for (String name : EnvyWorld.config().client.watchedPlayers) {
            String key = name.toLowerCase(Locale.ROOT);
            if (!visible.contains(key) && !OnlineScanner.hasChecked(name)) continue;
            boolean online = network.contains(key);
            Presence before = states.get(key);
            if (before != null && before.known() && before.online() == online) continue;
            states.put(key, new Presence(true, online, System.currentTimeMillis()));
            if (before != null && before.known() && EnvyWorld.config().client.onlineNotifications) {
                HudNotices.add(name + (online ? " вошёл в сеть" : " вышел из сети"), "Отслеживаемый игрок", online ? 0xFF87D9B0 : 0xFFE9B5C1);
                if (EnvyWorld.config().client.notificationSound) client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), online ? 1.3f : .8f, EnvyWorld.config().client.notificationVolume * .5f));
            }
        }
    }
}
