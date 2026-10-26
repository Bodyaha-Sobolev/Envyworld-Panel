package com.envyworld.config;

import java.util.ArrayList;
import java.util.List;

/** Client-only preferences; existing configs inherit these defaults on upgrade. */
public class ClientSettings {
    public boolean menuAnimations = true;
    public boolean clientProtection = true;
    public boolean freezeDisconnect = true;
    public int freezeTimeoutSeconds = 5;
    public int entityRenderLimit = 384;
    public boolean hudEnabled = true;
    public boolean hudGrid = true;
    public boolean hudSnap = true;
    public int hudOpacity = 185;
    public boolean hudBlur = true;
    public boolean logNotifications = true;
    public boolean notificationSound = true;
    public boolean thunderSound = true;
    public boolean nearbyLightning = true;
    public boolean onlineNotifications = true;
    public boolean guidedReasons = true;
    public boolean spectatorCreativeUi = true;
    public boolean spectatorEsp = true;
    public boolean espNames = true, espBoxes = true, espHealth = true, espEquipment = true, espPreventOverlap = true;
    public int espDistance = 128;
    public boolean espBlur = true;
    public boolean targetInspector = false;
    public boolean espPunishments = true;
    public boolean espChecks = true;
    /** Only an explicit IP-history request starts the sequential address checks. */
    public boolean checkIpAddresses = true;
    public float notificationVolume = .35f;
    public List<String> watchedPlayers = new ArrayList<>();
    public HudBox macros = new HudBox(.015f, .19f);
    public HudBox online = new HudBox(.015f, .37f);
    public HudBox notices = new HudBox(.73f, .12f);
    public HudBox target = new HudBox(.73f, .40f);
    public static class HudBox {
        public float x, y, scale = 1;
        public int anchorX = -1, anchorY = -1;
        public boolean visible = true, background = true;
        public HudBox() {}
        public HudBox(float x, float y) { this.x = x; this.y = y; }
    }
}
