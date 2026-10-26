package com.envyworld.client.gui;

import com.envyworld.graphics.misc.ThemePalette;

/** Neutral glass theme; white accents keep action colors separate from navigation. */
public final class MenuTheme {
    public static final ThemePalette PALETTE = new ThemePalette(
            "white", "White", 0xB9000000, 0xB9000000, 0x60000000, 0x80303030,
            0x60000000, 0xFF080808, 0xB0000000, 0xB0000000,
            0x38FFFFFF, 0x14FFFFFF, 0x0CFFFFFF, 0x50FFFFFF,
            0xFFF4F6FA, 0xFFB5BDC9, 0xFF87919F, 0xFF68717E, 0xFF555E6B,
            0xFFF1F4FA, 0x26FFFFFF, 0xFFFF8293, 0x66000000,
            0x16FFFFFF, 0x4019191B, 0x40FFFFFF, 0x401B1B1D,
            0xB9000000, 0xFFF4F6FA, 0xFF87919F, 0xFFB5BDC9, 0x66000000, true, true);

    private MenuTheme() {}
}
