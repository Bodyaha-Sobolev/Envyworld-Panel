package com.envyworld.client.gui;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

/** Uses the connection's actual skin first, then Minecraft's asynchronous profile/skin cache. */
public final class PlayerProfileView {
    private static final com.google.common.cache.Cache<String, PlayerProfileView> CACHE = com.google.common.cache.CacheBuilder.newBuilder()
            .maximumSize(512).expireAfterAccess(java.time.Duration.ofMinutes(10)).build();
    private final String nick;
    private final ResolvableProfile profile;
    private final CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> lookup;
    private final CompletableFuture<Optional<PlayerSkin>> launcher;

    public static PlayerProfileView cached(String nick) {
        String key = nick.toLowerCase(java.util.Locale.ROOT);
        PlayerProfileView view = CACHE.getIfPresent(key);
        if (view == null) { view = new PlayerProfileView(nick); CACHE.put(key, view); }
        return view;
    }

    public PlayerProfileView(String nick) {
        this.nick = nick;
        this.profile = ResolvableProfile.createUnresolved(nick);
        this.launcher = KLauncherSkins.lookup(nick);
        this.lookup = launcher.thenCompose(custom -> custom.isPresent()
                ? CompletableFuture.completedFuture(Optional.<PlayerSkinRenderCache.RenderInfo>empty())
                : Minecraft.getInstance().playerSkinRenderCache().lookup(profile)).exceptionally(ignored -> Optional.empty());
    }

    public String nick() { return nick; }

    public PlayerSkin skin() {
        var client = Minecraft.getInstance();
        if (client.getConnection() != null) {
            var info = client.getConnection().getPlayerInfo(nick);
            if (info != null && !info.getSkin().body().equals(DefaultPlayerSkin.get(info.getProfile()).body())) return info.getSkin();
        }
        Optional<PlayerSkin> custom = launcher.getNow(Optional.empty());
        if (custom.isPresent()) return custom.get();
        return lookup.getNow(Optional.empty()).map(PlayerSkinRenderCache.RenderInfo::playerSkin)
                .orElseGet(() -> DefaultPlayerSkin.get(profile.partialProfile()));
    }

    public String skinStatus() {
        var client = Minecraft.getInstance();
        if (client.getConnection() != null && client.getConnection().getPlayerInfo(nick) != null) return "Скин игрока";
        if (launcher.getNow(Optional.empty()).isPresent()) return "Скин KLauncher";
        if (!lookup.isDone() || !launcher.isDone()) return "Скин загружается...";
        return lookup.getNow(Optional.empty()).isPresent() ? "Скин игрока" : "Стандартный скин";
    }

    public PlayerSkinWidget createWidget(int x, int y, int width, int height) {
        PlayerSkinWidget widget = new PlayerSkinWidget(width, height, Minecraft.getInstance().getEntityModels(), this::skin);
        widget.setPosition(x, y);
        return widget;
    }
}
