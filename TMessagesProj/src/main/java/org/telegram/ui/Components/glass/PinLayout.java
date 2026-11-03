package org.telegram.ui.Components.glass;

import android.content.res.Configuration;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;

/**
 * Where everything sits on a PIN or code entry screen, as fractions of the screen so the layout
 * keeps its proportions on any phone. Vertical positions are centers, from the top of the screen.
 */
public final class PinLayout {

    private PinLayout() {}

    public static final float LOCK_Y = 0.074f;
    /** A 58dp lock animation drawn at this scale. */
    public static final float LOCK_SCALE = 0.45f;
    public static final float MESSAGE_Y = 0.233f;
    public static final float MESSAGE_WIDTH = 0.77f;
    public static final float MESSAGE_TEXT_SP = 18f;
    public static final float DOTS_Y = 0.322f;
    /** Distance between dots and their radius, of the screen's width. */
    public static final float DOT_STEP = 0.0628f;
    public static final float DOT_RADIUS = 0.0149f;
    public static final float FIRST_ROW_Y = 0.477f;
    /** Distance between key rows, of the screen's height. */
    public static final float ROW_STEP = 0.1064f;
    /** Distance between key columns and the keys' diameter, of the screen's width. */
    public static final float COLUMN_STEP = 0.2545f;
    public static final float KEY_SIZE = 0.192f;
    public static final float DIGIT_SP = 30f;

    /** The proportions only hold on an upright phone; elsewhere screens keep their compact layout. */
    public static boolean applies(@NonNull Configuration configuration) {
        return !AndroidUtilities.isTablet() && configuration.orientation != Configuration.ORIENTATION_LANDSCAPE;
    }

    /** Distance between key rows that leaves a small gap between keys even on a short screen. */
    public static int rowStep(int screenWidth, int screenHeight) {
        return Math.max((int) (ROW_STEP * screenHeight), (int) (KEY_SIZE * screenWidth) + AndroidUtilities.dp(4));
    }

    /** The keypad's height: from just above its first row down to the bottom of the screen. */
    public static int keypadHeight(int screenWidth, int screenHeight) {
        final int keySize = (int) (KEY_SIZE * screenWidth);
        final int rows = rowStep(screenWidth, screenHeight) * 3 + keySize;
        return Math.max((int) ((1f - FIRST_ROW_Y) * screenHeight + keySize / 2f), rows + AndroidUtilities.dp(16)) + AndroidUtilities.dp(KEYPAD_TOP_INSET_DP);
    }

    /** Room above the first row, so a key that swells under the finger stays whole. */
    public static final int KEYPAD_TOP_INSET_DP = 6;

    public static int estimatedScreenHeight() {
        return AndroidUtilities.displaySize.y + AndroidUtilities.statusBarHeight;
    }
}
