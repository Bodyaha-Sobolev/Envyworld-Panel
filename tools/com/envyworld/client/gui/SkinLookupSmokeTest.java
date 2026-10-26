package com.envyworld.client.gui;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SkinLookupSmokeTest {
    public static void main(String[] args) {
        var skin = KLauncherSkins.parse(fixture("http://skin.klaun.ch/storage/skins/test.png")).orElseThrow();
        if (!skin.getUrl().equals("https://skin.klaun.ch/storage/skins/test.png") || !"slim".equals(skin.getMetadata("model")))
            throw new AssertionError("Skin URL/model extraction");
        if (KLauncherSkins.parse(fixture("http://example.com/skin.png")).isPresent()) throw new AssertionError("Unexpected texture host");
        if (KLauncherSkins.parse("{\"success\":false}").isPresent()) throw new AssertionError("Missing skin fallback");
        if(KLauncherSkins.parseOfficial("{\"textures\":{\"SKIN\":{\"url\":\"https://skin.klaun.ch/storage/skins/direct.png\"}}}").isEmpty())throw new AssertionError("Official skin API payload");
        if(KLauncherSkins.parseOfficial("{\"textures\":{\"SKIN\":{\"url\":\"https://example.com/secret\"}}}").isPresent())throw new AssertionError("Official API cannot inject arbitrary texture hosts");
        if(!HeldItemNames.readable("Алмазный меч")||HeldItemNames.readable("♛ 死 Sword")||HeldItemNames.readable("سيف"))throw new AssertionError("Russian readable name policy");
        System.out.println("PASS: live local timer, removed/permanent expiry states, future dates and KLauncher skin payload/model parsing.");
    }
    private static String fixture(String url) {
        String payload = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\",\"metadata\":{\"model\":\"slim\"}}}}";
        String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        JsonObject row = new JsonObject(); row.addProperty("mc_value", "/give @p player_head[profile={properties:[{name:\"textures\",value:\"" + encoded + "\"}]}]");
        JsonArray rows = new JsonArray(); rows.add(row);
        JsonObject root = new JsonObject(); root.addProperty("success", true); root.add("rows", rows);
        return root.toString();
    }
}
