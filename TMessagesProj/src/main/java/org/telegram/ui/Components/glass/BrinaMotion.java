package org.telegram.ui.Components.glass;

import android.animation.ValueAnimator;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Motion shared by every popup: the panel opens from a tenth of its size around a point near its
 * anchor, lands on a barely visible overshoot and closes quickly back to the same point.
 *
 * <p>The curves are keyframes rather than formulas so they can be tuned one frame at a time; times
 * are in milliseconds.
 */
public final class BrinaMotion {

    private BrinaMotion() {}

    /** Pivot of a popup's growth, as fractions of its size measured from the anchored corner. */
    public static final float PIVOT_ALONG_WIDTH = 0.11f;
    public static final float PIVOT_ALONG_HEIGHT = 0.23f;

    public static final long POPUP_ENTER_MS = 440;
    public static final long POPUP_EXIT_MS = 102;

    private static final float[] ENTER_TIME = {0, 13, 24, 42, 56, 74, 89, 112, 137, 153, 170, 187, 203, 220, 320, 440};
    private static final float[] ENTER_SCALE = {0.12f, 0.24f, 0.38f, 0.48f, 0.59f, 0.70f, 0.77f, 0.87f, 0.949f, 0.97f, 0.985f, 0.994f, 1.003f, 1.006f, 1.003f, 1f};
    private static final float[] ENTER_ALPHA_TIME = {0, 13, 42, 74, 110};
    private static final float[] ENTER_ALPHA = {0f, 0.2f, 0.5f, 0.8f, 1f};

    private static final float[] EXIT_TIME = {0, 17, 34, 51, 68, 85, 102};
    private static final float[] EXIT_SCALE = {1f, 0.97f, 0.82f, 0.62f, 0.43f, 0.3f, 0.2f};
    private static final float[] EXIT_ALPHA = {1f, 1f, 0.85f, 0.55f, 0.3f, 0.15f, 0f};

    /** Linear interpolation through keyframes; {@code x} is clamped to the first and last time. */
    static float sample(float[] times, float[] values, float x) {
        if (x <= times[0]) {
            return values[0];
        }
        final int last = times.length - 1;
        if (x >= times[last]) {
            return values[last];
        }
        int i = 1;
        while (times[i] < x) {
            i++;
        }
        final float f = (x - times[i - 1]) / (times[i] - times[i - 1]);
        return values[i - 1] + (values[i] - values[i - 1]) * f;
    }

    public static void applyEnter(@NonNull View view, float ms) {
        final float scale = sample(ENTER_TIME, ENTER_SCALE, ms);
        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setAlpha(sample(ENTER_ALPHA_TIME, ENTER_ALPHA, ms));
    }

    public static void applyExit(@NonNull View view, float ms) {
        final float scale = sample(EXIT_TIME, EXIT_SCALE, ms);
        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setAlpha(sample(EXIT_TIME, EXIT_ALPHA, ms));
    }

    /** Drives {@code view} through the opening curve. Not started. */
    @NonNull
    public static ValueAnimator popupEnter(@NonNull View view) {
        applyEnter(view, 0);
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, POPUP_ENTER_MS);
        animator.setDuration(POPUP_ENTER_MS);
        animator.setInterpolator(null);
        animator.addUpdateListener(a -> applyEnter(view, (float) a.getAnimatedValue()));
        return animator;
    }

    /** Drives {@code view} through the closing curve. Not started. */
    @NonNull
    public static ValueAnimator popupExit(@NonNull View view, long maxDuration) {
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, POPUP_EXIT_MS);
        animator.setDuration(Math.min(POPUP_EXIT_MS, maxDuration));
        animator.setInterpolator(null);
        animator.addUpdateListener(a -> applyExit(view, (float) a.getAnimatedValue()));
        return animator;
    }
}
