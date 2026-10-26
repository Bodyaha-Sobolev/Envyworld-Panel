package com.envyworld.client.gui;

import java.util.List;

/** Common server command variants, shown explicitly instead of silently changing their type. */
public final class PunishmentTypes {
    public record Type(String command, String label, boolean timed) {}
    public static final List<Type> ALL = List.of(
            new Type("ban", "Бан", true), new Type("mute", "Мут", true),
            new Type("banip", "Бан по IP", true), new Type("muteip", "Мут по IP", true),
            new Type("tempban", "Временный бан", true), new Type("tempmute", "Временный мут", true),
            new Type("tempbanip", "Временный бан по IP", true), new Type("tempmuteip", "Временный мут по IP", true),
            new Type("warn", "Предупреждение", false), new Type("kick", "Кик", false));

    private PunishmentTypes() {}

    public static Type get(String command) {
        return ALL.stream().filter(type -> type.command().equals(command)).findFirst().orElse(ALL.getFirst());
    }

    public static String buttonLabel(String command) { return get(command).label() + "  v"; }
}
