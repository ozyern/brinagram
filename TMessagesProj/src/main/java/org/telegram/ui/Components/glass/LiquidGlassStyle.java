package org.telegram.ui.Components.glass;

/**
 * BrinaGram's iOS / liquid glass design tokens, shared across components so radii and
 * colors stay consistent.
 *
 * <p>The popup and sheet nine-patches ({@code popup_fixed_alert2}, {@code popup_fixed_alert4},
 * {@code sheet_shadow_round}) are pre-rendered with {@link #MENU_RADIUS_DP} and
 * {@link #SHEET_RADIUS_DP}. If you change those values, regenerate the PNGs too.
 */
public final class LiquidGlassStyle {

    private LiquidGlassStyle() {}

    /** Corner radius of context menus, popups and alert dialogs. */
    public static final int MENU_RADIUS_DP = 20;
    /** Top corner radius of bottom sheets. */
    public static final int SHEET_RADIUS_DP = 28;

    /** iOS system colors (light / dark). */
    public static final int SYSTEM_GREEN = 0xFF34C759;
    public static final int SYSTEM_GREEN_DARK = 0xFF30D158;
    public static final int SYSTEM_RED = 0xFFFA2D48;
    public static final int SYSTEM_RED_DARK = 0xFFFF375F;
    public static final int SYSTEM_FILL_LIGHT = 0x29787880; // iOS tertiarySystemFill, used for switch tracks
    public static final int SYSTEM_FILL_DARK = 0x52787880;
}
