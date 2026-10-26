package com.envyworld.client.gui;

import java.util.ArrayDeque;
import java.util.List;

public final class HudNotices {
    public record Notice(String title, String body, int color, long created) {}
    private static final ArrayDeque<Notice> notices = new ArrayDeque<>();
    private HudNotices() {}
    public static void add(String title, String body, int color) {
        notices.addFirst(new Notice(title, body, color, System.currentTimeMillis()));
        while (notices.size() > 8) notices.removeLast();
    }
    public static List<Notice> current() {
        long now = System.currentTimeMillis();
        notices.removeIf(notice -> now - notice.created() > 9000);
        return List.copyOf(notices);
    }
}
