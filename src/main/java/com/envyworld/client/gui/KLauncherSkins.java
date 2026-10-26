package com.envyworld.client.gui;

import com.envyworld.mixin.SkinManagerInvoker;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

/** Read-only nickname lookup; textures use Minecraft's original loader and disk cache. */
public final class KLauncherSkins {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Pattern TEXTURE_VALUE = Pattern.compile("value:\\s*\"([A-Za-z0-9+/=]+)\"");
    private KLauncherSkins() {}
    private static final com.google.common.cache.Cache<String,CompletableFuture<String>> PREVIEWS=com.google.common.cache.CacheBuilder.newBuilder().maximumSize(128).expireAfterAccess(Duration.ofMinutes(10)).build();
    public static synchronized CompletableFuture<String> preview(String nick){
        if(!nick.matches("[A-Za-z0-9_]{1,16}"))return CompletableFuture.completedFuture("");
        var existing=PREVIEWS.getIfPresent(nick.toLowerCase(java.util.Locale.ROOT));if(existing!=null)return existing;
        var future=texture(nick).thenCompose(texture->{
            if(texture.isEmpty())return CompletableFuture.completedFuture("");
            return HTTP.sendAsync(HttpRequest.newBuilder(URI.create(texture.get().getUrl())).timeout(Duration.ofSeconds(8)).build(),HttpResponse.BodyHandlers.ofByteArray())
                    .thenApply(response->response.statusCode()==200&&response.body().length<=1048576?"data:image/png;base64,"+Base64.getEncoder().encodeToString(response.body()):"");
        }).exceptionally(e->"");PREVIEWS.put(nick.toLowerCase(java.util.Locale.ROOT),future);return future;
    }

    static CompletableFuture<Optional<PlayerSkin>> lookup(String nick) {
        if (!nick.matches("[A-Za-z0-9_]{1,16}")) return CompletableFuture.completedFuture(Optional.empty());
        return texture(nick).thenCompose(texture -> {
                    if (texture.isEmpty()) return CompletableFuture.completedFuture(Optional.<PlayerSkin>empty());
                    var client = Minecraft.getInstance();
                    var textures = new MinecraftProfileTextures(texture.get(), null, null, SignatureState.UNSIGNED);
                    UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8));
                    return CompletableFuture.supplyAsync(() -> ((SkinManagerInvoker) client.getSkinManager())
                                    .envyworld$registerTextures(id, textures), client)
                            .thenCompose(future -> future).thenApply(Optional::of);
                }).exceptionally(ignored -> Optional.empty());
    }

    private static final com.google.common.cache.Cache<String,CompletableFuture<Optional<MinecraftProfileTexture>>> TEXTURES=com.google.common.cache.CacheBuilder.newBuilder().maximumSize(256).expireAfterWrite(Duration.ofMinutes(5)).build();
    private static synchronized CompletableFuture<Optional<MinecraftProfileTexture>> texture(String nick){
        var cached=TEXTURES.getIfPresent(nick);if(cached!=null)return cached;
        var request=HttpRequest.newBuilder(URI.create("https://api.klaun.ch/v2/user/skin?nick="+nick)).timeout(Duration.ofSeconds(8)).GET().build();
        var future=HTTP.sendAsync(request,HttpResponse.BodyHandlers.ofString()).thenApply(r->r.statusCode()==200?parseOfficial(r.body()):Optional.<MinecraftProfileTexture>empty()).exceptionally(e->Optional.empty()).thenCompose(found->{
            if(found.isPresent())return CompletableFuture.completedFuture(found);
            var legacy=HttpRequest.newBuilder(URI.create("https://api.dehasher.net/site/dev/heads")).timeout(Duration.ofSeconds(8)).header("Content-Type","application/x-www-form-urlencoded").header("Origin","https://dehasher.net").header("Referer","https://dehasher.net/dev/heads").POST(HttpRequest.BodyPublishers.ofString("nick="+nick+"&launcher=klauncher")).build();
            return HTTP.sendAsync(legacy,HttpResponse.BodyHandlers.ofString()).thenApply(r->r.statusCode()==200?parse(r.body()):Optional.<MinecraftProfileTexture>empty()).exceptionally(e->Optional.empty());
        });TEXTURES.put(nick,future);return future;
    }
    static Optional<MinecraftProfileTexture> parseOfficial(String json){
        return parsePayload(JsonParser.parseString(json).getAsJsonObject());
    }
    private static Optional<MinecraftProfileTexture> parsePayload(JsonObject payload){
        var textures=payload.getAsJsonObject("textures");if(textures==null||!textures.has("SKIN"))return Optional.empty();
        var skin=textures.getAsJsonObject("SKIN");if(!skin.has("url"))return Optional.empty();URI url=URI.create(skin.get("url").getAsString());
        if(!"skin.klaun.ch".equalsIgnoreCase(url.getHost())||!url.getPath().startsWith("/storage/skins/"))return Optional.empty();
        String model=skin.has("metadata")&&skin.getAsJsonObject("metadata").has("model")?skin.getAsJsonObject("metadata").get("model").getAsString():"default";
        return Optional.of(new MinecraftProfileTexture("https://skin.klaun.ch"+url.getPath(),Map.of("model",model)));
    }

    static Optional<MinecraftProfileTexture> parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("success") || !root.get("success").getAsBoolean() || !root.has("rows")) return Optional.empty();
        for (var element : root.getAsJsonArray("rows")) {
            String command = element.getAsJsonObject().get("mc_value").getAsString();
            var match = TEXTURE_VALUE.matcher(command);
            if (!match.find()) continue;
            var payload = JsonParser.parseString(new String(Base64.getDecoder().decode(match.group(1)), StandardCharsets.UTF_8)).getAsJsonObject();
            return parsePayload(payload);
        }
        return Optional.empty();
    }
}
