package com.envyworld.logs;

/** Display names only; never changes the API's server keys. */
public final class ServerModes {
    private ServerModes(){}
    public static String label(String raw){
        return raw.replaceAll("(?iu)Выживание\\s*#?1\\b(?!\\s*:)", "Выживание 1: Ио")
                .replaceAll("(?iu)Выживание\\s*#?2\\b(?!\\s*:)", "Выживание 2: Европа")
                .replaceAll("(?iu)Выживание\\s*#?3\\b(?!\\s*:)", "Выживание 3: Лотус")
                .replaceAll("(?iu)Выживание\\s*#?4\\b(?!\\s*:)", "Выживание 4: Калипсо")
                .replaceAll("(?iu)Выживание\\s*#?5\\b(?!\\s*:)", "Выживание 5: Альматея");
    }
}
