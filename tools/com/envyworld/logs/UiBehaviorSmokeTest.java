package com.envyworld.logs;

import com.envyworld.client.MacroRepeatState;
import com.envyworld.client.gui.ScrollBars;
import java.util.List;

/** Checks behavior that can silently spam commands or replay old alerts. */
public final class UiBehaviorSmokeTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static PunishLogClient.Row row(String name, long when, String reason) {
        return new PunishLogClient.Row("warnings", name, "Mod", reason, Long.toString(when), "", "ОдинБлок");
    }
    public static void run() {
        float[] motion=new float[3];int[] rates={30,60,144};
        for(int r=0;r<rates.length;r++)for(int i=0;i<rates[r];i++)motion[r]=com.envyworld.client.gui.UiMotion.advance(motion[r],1,1f/rates[r]);
        check(Math.abs(motion[0]-motion[2])<.0001f&&Math.abs(motion[1]-motion[2])<.0001f,"Menu motion is independent of FPS");
        check(com.envyworld.client.gui.UiMotion.advance(.5f,0,.1f)>0&&com.envyworld.client.gui.UiMotion.advance(.5f,1,.1f)<1,"Menu motion never overshoots");

        String styled=com.envyworld.items.ItemText.format("&aA&bB","&l");
        boolean[] bold={true};java.util.List<Integer> colors=new java.util.ArrayList<>();
        com.envyworld.items.ItemText.parse(styled,"WHITE").visit((style,value)->{if(!value.isEmpty()){bold[0]&=style.isBold();colors.add(style.getColor().getValue());}return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        check(bold[0]&&colors.contains(0x55FF55)&&colors.contains(0x55FFFF),"Bold preserves different colors in a selection");
        String gradient=com.envyworld.items.ItemText.gradient(styled,"#123456","#ABCDEF");
        check(com.envyworld.items.ItemText.parse(gradient,"WHITE").getString().equals("AB"),"Gradient preserves text");
        com.envyworld.items.ItemText.parse(gradient,"WHITE").visit((style,value)->{if(!value.isEmpty())check(style.isBold(),"Gradient must retain bold on every character");return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        check(com.envyworld.client.MacroKeys.matches(3,19)&&!com.envyworld.client.MacroKeys.matches(2,3),"Key chords ignore Caps Lock but require exact Ctrl/Shift/Alt modifiers");
        for(int mods:new int[]{1,2,3,7,15,19})check(com.envyworld.client.MacroKeys.matches(0,mods),"Plain macro key remains usable while moving with Ctrl/Shift");
        var hotkey=new com.envyworld.bind.Binding("Hotkey",com.envyworld.bind.BindTrigger.KEY,"minecraft:stick","say fixture");hotkey.keyCode=86;
        check(com.envyworld.client.ClientMacros.usesItemFilter(hotkey),"Dedicated hotkey respects its configured item at start and during repeats");
        hotkey.item="any";check(!com.envyworld.client.ClientMacros.usesItemFilter(hotkey),"Unrestricted hotkey works with any held item");
        hotkey.customItem="fixture";check(com.envyworld.client.ClientMacros.usesItemFilter(hotkey),"Custom preset restricts a dedicated hotkey even with any base item");
        hotkey.customItem="";hotkey.customModelData=7;check(com.envyworld.client.ClientMacros.usesItemFilter(hotkey),"Custom model filter also applies to dedicated hotkeys");
        hotkey.customModelData=0;hotkey.item="minecraft:stick";
        hotkey.keyCode=-1;check(com.envyworld.client.ClientMacros.usesItemFilter(hotkey),"Global item macro key retains its item filter");
        hotkey.keyCode=86;hotkey.trigger=com.envyworld.bind.BindTrigger.ATTACK_ENTITY;check(com.envyworld.client.ClientMacros.usesItemFilter(hotkey),"Attack macros retain item matching");
        var finite = new MacroRepeatState("PRESS", 3, 500, 1000);
        check(finite.due(1000), "Immediate first macro execution"); finite.ran(1000);
        check(!finite.due(1499) && finite.due(1500), "Configured repeat interval"); finite.ran(1500);
        finite.ran(5000); check(finite.done(), "Exactly the configured number of executions");
        var forever = new MacroRepeatState("TOGGLE", 0, 100, 0);
        for (int i = 0; i < 20; i++) forever.ran(i * 100L);
        check(!forever.done() && !forever.due(1950) && forever.due(2000), "Unlimited Toggle repeats");
        forever.ran(90_000); check(!forever.due(90_000), "Delayed ticks do not catch up in a burst");
        var zero = new MacroRepeatState("PRESS", 0, 0, 0); zero.ran(0); check(zero.done(), "PRESS with count zero cannot loop forever");
        var hold = new MacroRepeatState("HOLD", 0, 0, 0); hold.ran(0); check(!hold.due(49) && hold.due(50), "Repeat delay has a one-tick minimum");
        var tracker = new NewLogTracker(); long now = 1_792_800_000;
        var original = row("Alice", now - 120, "Rule");
        check(tracker.accept("warnings", List.of(original), now).isEmpty(), "Initial history is silent");
        check(tracker.accept("warnings", List.of(original), now).isEmpty(), "Repeated snapshot is silent");
        check(tracker.accept("warnings", List.of(row("Alice", now - 120, "Edited reason")), now).isEmpty(), "Editing a reason is not a new warning");
        check(tracker.accept("warnings", List.of(original, row("Bob", now - 180, "Backfill")), now).isEmpty(), "Older backfill is silent");
        var fresh = row("Carol", now, "New warning");
        check(tracker.accept("warnings", List.of(original, fresh), now).equals(List.of(fresh)), "New warning alerts once");
        check(tracker.accept("warnings", List.of(fresh), now).isEmpty(), "Removals do not replay other rows");
        check(tracker.accept("warnings", List.of(fresh, row("Carol", now, "Second warning")), now).size() == 1, "Two warnings in the same minute still count separately");
        check(tracker.accept("warnings", List.of(fresh), now).isEmpty(), "Shrinking warning list is silent");
        tracker.reset(); check(tracker.accept("warnings", List.of(fresh), now).isEmpty(), "Reconnect establishes a silent baseline");
        var bar = new ScrollBars.Bar(100, 10, 200, 150, 200, 800, value -> {});
        check(bar.maximum() == 600 && bar.thumbHeight() == 50, "Scrollbar reflects the full viewport");
        check(bar.at(210, 50) == 600 && bar.at(-100, 0) == 0, "Dragging reaches exact bounds");
        int[] scroll = {0}; var bars = new ScrollBars(); bars.add(100, 10, 200, 0, 200, 800, v -> scroll[0] = v);
        check(bars.press(100, 20), "Thumb receives LMB"); bars.drag(400); check(scroll[0] == 600, "LMB drag reaches last content pixel");
        check(bars.release() && !bars.drag(10), "Release stops dragging");
        System.out.println("PASS: repeats, no catch-up bursts, new-log baselines/edits/backfill/counts and draggable scrolling");
    }
}
