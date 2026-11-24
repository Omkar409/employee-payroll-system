package payroll.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Enterprise design tokens & styling palette for SRM University AP Payroll System.
 */
public final class UITheme {

    private UITheme() {}

    // University Brand Palette
    public static final Color NAVY_PRIMARY     = new Color(0x0F, 0x29, 0x4A); // #0F294A
    public static final Color NAVY_DARK        = new Color(0x09, 0x18, 0x2B); // #09182B
    public static final Color NAVY_HOVER       = new Color(0x1A, 0x3E, 0x6D); // #1A3E6D
    public static final Color GOLD_ACCENT      = new Color(0xC5, 0x9B, 0x27); // #C59B27
    public static final Color GOLD_LIGHT       = new Color(0xE0, 0xB7, 0x4E); // #E0B74E
    public static final Color BG_OFFWHITE      = new Color(0xF4, 0xF6, 0xF9); // #F4F6F9
    public static final Color CARD_WHITE       = new Color(0xFF, 0xFF, 0xFF); // #FFFFFF
    public static final Color BORDER_SUBTLE    = new Color(0xD1, 0xD9, 0xE6); // #D1D9E6
    public static final Color TABLE_STRIPE     = new Color(0xF8, 0xFA, 0xFC); // #F8FAFC
    public static final Color TABLE_SELECTION  = new Color(0xDB, 0xEA, 0xFE); // #DBEAFE
    public static final Color TEXT_DARK        = new Color(0x0F, 0x17, 0x2A); // #0F172A
    public static final Color TEXT_MUTED       = new Color(0x64, 0x74, 0x8B); // #64748B

    // Button Categories
    public static final Color BTN_PRIMARY      = new Color(0x0F, 0x29, 0x4A); // Deep Navy (#0F294A)
    public static final Color BTN_POSITIVE     = new Color(0x25, 0x63, 0xEB); // Slate Blue (#2563EB)
    public static final Color BTN_DESTRUCTIVE  = new Color(0xDC, 0x26, 0x26); // Crimson Red (#DC2626)
    public static final Color BTN_UTILITY      = new Color(0xE2, 0xE8, 0xF0); // Clean Light Gray (#E2E8F0)
    public static final Color BTN_UTILITY_TEXT = new Color(0x0F, 0x29, 0x4A); // Dark navy text

    // Status Dots
    public static final Color STATUS_GREEN     = new Color(0x10, 0xB9, 0x81);
    public static final Color STATUS_AMBER     = new Color(0xF5, 0x9E, 0x0B);
    public static final Color STATUS_RED       = new Color(0xEF, 0x44, 0x44);

    // Typography
    private static final String FONT_FAMILY = determineFontFamily();

    public static final Font FONT_HEADER       = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_TITLE        = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font FONT_BOLD_13      = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_REGULAR_13   = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_REGULAR_12   = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_BUTTON       = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_MONO_11      = new Font("Consolas", Font.PLAIN, 11);
    public static final Font FONT_MONO_12      = new Font("Consolas", Font.PLAIN, 12);

    private static String determineFontFamily() {
        Set<String> fonts = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        if (fonts.contains("Segoe UI")) return "Segoe UI";
        if (fonts.contains("Helvetica Neue")) return "Helvetica Neue";
        if (fonts.contains("Arial")) return "Arial";
        return Font.SANS_SERIF;
    }
}
