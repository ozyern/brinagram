package org.telegram.ui.Components.glass;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;

/**
 * Colors for profile pages on dark themes: the backdrop and cards take their hue from the photo,
 * and text is white so it reads on any hue. Light themes pass through unchanged.
 */
public final class TintedProfileResources implements Theme.ResourcesProvider {

    private int backdropTop = 0xFF1C1C1E;
    private int backdropBottom = 0xFF000000;
    private int backdropMiddle = 0xFF0E0E0F;
    private int card = 0xFF1C1C1E;

    /** Derives the palette from the photo's average color. */
    public void setTint(int color) {
        final float[] hsv = new float[3];
        Color.colorToHSV(color | 0xFF000000, hsv);
        final float saturation = Math.max(0.18f, Math.min(0.55f, hsv[1]));
        backdropTop = Color.HSVToColor(new float[] {hsv[0], saturation, 0.36f});
        backdropMiddle = Color.HSVToColor(new float[] {hsv[0], saturation, 0.2f});
        backdropBottom = Color.HSVToColor(new float[] {hsv[0], saturation, 0.07f});
        card = ColorUtils.blendARGB(backdropMiddle, 0xFFFFFFFF, 0.1f);
    }

    public int getBackdropTop() {
        return backdropTop;
    }

    public int getBackdropBottom() {
        return backdropBottom;
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
