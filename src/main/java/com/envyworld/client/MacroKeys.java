package com.envyworld.client;

import com.envyworld.bind.Binding;
import org.lwjgl.glfw.GLFW;

public final class MacroKeys {
    private MacroKeys() {}
    public static boolean matches(int required, int actual) { return (required & 15)==0 || (required & 15)==(actual & 15); }
    public static int modifierForKey(int key){return switch(key){
        case GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_KEY_RIGHT_SHIFT->GLFW.GLFW_MOD_SHIFT;
        case GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_KEY_RIGHT_CONTROL->GLFW.GLFW_MOD_CONTROL;
        case GLFW.GLFW_KEY_LEFT_ALT,GLFW.GLFW_KEY_RIGHT_ALT->GLFW.GLFW_MOD_ALT;
        case GLFW.GLFW_KEY_LEFT_SUPER,GLFW.GLFW_KEY_RIGHT_SUPER->GLFW.GLFW_MOD_SUPER;
        default->0;
    };}
    public static boolean down(Binding binding, long window, boolean fallback) {
        if (binding.keyCode == 0) return false;
        if (binding.keyCode == -1) return (binding.trigger==com.envyworld.bind.BindTrigger.KEY || binding.trigger==com.envyworld.bind.BindTrigger.RADIUS || binding.trigger==com.envyworld.bind.BindTrigger.COMMAND) && fallback;
        boolean down = binding.keyCode <= -2 ? GLFW.glfwGetMouseButton(window, -binding.keyCode - 2) == GLFW.GLFW_PRESS
                : binding.keyCode >= 32 && binding.keyCode <= GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window, binding.keyCode) == GLFW.GLFW_PRESS;
        int mods = 0;
        if (held(window, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT)) mods |= GLFW.GLFW_MOD_SHIFT;
        if (held(window, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL)) mods |= GLFW.GLFW_MOD_CONTROL;
        if (held(window, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT)) mods |= GLFW.GLFW_MOD_ALT;
        if (held(window, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER)) mods |= GLFW.GLFW_MOD_SUPER;
        int own=modifierForKey(binding.keyCode);
        return down && matches(binding.keyModifiers&~own, mods&~own);
    }
    private static boolean held(long window, int a, int b) { return GLFW.glfwGetKey(window, a) == 1 || GLFW.glfwGetKey(window, b) == 1; }
    public static String label(int code, int mods) {
        String key = code == 0 ? "Не назначена" : code == -1 ? "Общая: "+EnvyWorldClient.macroKeyName() : code <= -2 ? "Мышь " + (-code - 1) : switch (code) {
            case GLFW.GLFW_KEY_SPACE -> "Space"; case GLFW.GLFW_KEY_ENTER -> "Enter"; case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "Alt";
            default -> code>=GLFW.GLFW_KEY_A&&code<=GLFW.GLFW_KEY_Z?Character.toString((char)code):code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25 ? "F" + (code - GLFW.GLFW_KEY_F1 + 1)
                    : GLFW.glfwGetKeyName(code, 0) == null ? "Key " + code : GLFW.glfwGetKeyName(code, 0).toUpperCase(java.util.Locale.ROOT);
        };
        return ((mods & 2) != 0 && !key.equals("Ctrl") ? "Ctrl + " : "") + ((mods & 1) != 0 && !key.equals("Shift") ? "Shift + " : "")
                + ((mods & 4) != 0 && !key.equals("Alt") ? "Alt + " : "") + ((mods & 8) != 0 ? "Win + " : "") + key;
    }
}
