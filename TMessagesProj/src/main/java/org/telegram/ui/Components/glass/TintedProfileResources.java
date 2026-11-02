package org.telegram.ui.Components.glass;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;

/**
 * Colors for profile pages on dark themes: a deep, muted backdrop that only hints at the photo's
 * hue, and cards that read as a pane of glass lying on it. Text is white so it reads on any hue.
 * Light themes pass through unchanged.
 */
public final class TintedProfileResources implements Theme.ResourcesProvider {

    /** How much white a card adds to the backdrop beneath it. */
    private static final float CARD_LIGHT = 0.09f;

    private int backdropTop = 0xFF1C1C1E;
    private int backdropBottom = 0xFF000000;
    private int backdropMiddle = 0xFF0E0E0F;
    private int cardTop = 0xFF2A2A2C;
    private int cardBottom = 0xFF17171A;
    private int card = 0xFF1C1C1E;

    /** Derives the palette from the photo's average color. */
    public void setTint(int color) {
        final float[] hsv = new float[3];
        Color.colorToHSV(color | 0xFF000000, hsv);
        final float saturation = Math.max(0.08f, Math.min(0.32f, hsv[1] * 0.55f));
        backdropTop = Color.HSVToColor(new float[] {hsv[0], saturation, 0.2f});
        backdropMiddle = Color.HSVToColor(new float[] {hsv[0], saturation, 0.12f});
        backdropBottom = Color.HSVToColor(new float[] {hsv[0], saturation, 0.05f});
        cardTop = ColorUtils.blendARGB(backdropTop, 0xFFFFFFFF, CARD_LIGHT);
        cardBottom = ColorUtils.blendARGB(backdropBottom, 0xFFFFFFFF, CARD_LIGHT);
        card = ColorUtils.blendARGB(backdropMiddle, 0xFFFFFFFF, CARD_LIGHT);
    }

    public int getBackdropTop() {
        return backdropTop;
    }

    public int getBackdropBottom() {
        return backdropBottom;
    }

    public int getCardTop() {
        return cardTop;
    }

    public int getCardBottom() {
        return cardBottom;
    }

    @Override
    public int getColor(int key) {
        if (Theme.isCurrentThemeDark()) {
            if (key == Theme.key_windowBackgroundGray || key == Theme.key_actionBarDefault) {
                return backdropMiddle;
            }
            if (key == Theme.key_windowBackgroundWhite) {
                return card;
            }
            if (key == Theme.key_profile_title || key == Theme.key_windowBackgroundWhiteBlackText) {
                return 0xFFFFFFFF;
            }
            if (key == Theme.key_avatar_subtitleInProfileBlue || key == Theme.key_windowBackgroundWhiteGrayText
                    || key == Theme.key_windowBackgroundWhiteGrayText2) {
                return 0xA6FFFFFF;
            }
        }
        return Theme.getColor(key);
    }
}
