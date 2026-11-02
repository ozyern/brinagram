package org.telegram.ui.Components.glass;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.ui.ActionBar.Theme;

/**
 * The row of dots of a PIN or code. Empty slots stay put as faint dots. A digit sparks a white dot
 * into its slot, swelling past its size with a soft bloom before it settles; a wrong code drops
 * the filled dots out of their slots one after another.
 */
public final class PinDots {

    private static final int EMPTY = 0, POPPING = 1, FILLED = 2, SHRINKING = 3, FALLING = 4;

    private static final float[] POP_TIME = {0, 17, 33, 50, 67, 83, 100, 117, 150, 183, 217, 267, 300};
    private static final float[] POP_SCALE = {0f, 0.15f, 0.35f, 0.65f, 1.05f, 1.4f, 1.55f, 1.6f, 1.55f, 1.35f, 1.2f, 1.05f, 1f};
    private static final float[] POP_BLOOM = {0f, 0.2f, 0.5f, 0.8f, 1f, 1f, 1f, 1f, 0.8f, 0.55f, 0.35f, 0.1f, 0f};
    private static final long POP_MS = 300;
    private static final long SHRINK_MS = 120;
    private static final long FALL_MS = 170;
    private static final long FALL_STAGGER_MS = 100;
    private static final float FALL_RADII = 4f;
    private static final float EMPTY_ALPHA = 0.3f;

    private final View host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] state = new int[0];
    private long[] startedAt = new long[0];

    public PinDots(@NonNull View host) {
        this.host = host;
    }

    public void setCount(int count) {
        state = new int[count];
        startedAt = new long[count];
        host.invalidate();
    }

    public int getCount() {
        return state.length;
    }

    /** Brings every slot to the given state, animating those that changed. */
    public void update(@NonNull boolean[] filled) {
        final long now = SystemClock.uptimeMillis();
        for (int i = 0; i < state.length; i++) {
            final boolean isFilled = i < filled.length && filled[i];
            final boolean shownFilled = state[i] == POPPING || state[i] == FILLED;
            if (isFilled && !shownFilled) {
                state[i] = POPPING;
                startedAt[i] = now;
                host.invalidate();
            } else if (!isFilled && shownFilled) {
                state[i] = SHRINKING;
                startedAt[i] = now;
                host.invalidate();
            }
        }
    }

    /** Fills the first {@code count} slots. */
    public void setFilledCount(int count) {
        final boolean[] filled = new boolean[state.length];
        for (int i = 0; i < filled.length; i++) {
            filled[i] = i < count;
        }
        update(filled);
    }

    /** Drops the filled dots one by one; the last two swap turns, so it never reads as a sweep. */
    public void fall() {
        int filledCount = 0;
        final int[] order = new int[state.length];
        for (int i = 0; i < state.length; i++) {
            if (state[i] == POPPING || state[i] == FILLED) {
                order[filledCount++] = i;
            }
        }
        if (filledCount >= 3) {
            final int last = order[filledCount - 1];
            order[filledCount - 1] = order[filledCount - 2];
            order[filledCount - 2] = last;
        }
        final long now = SystemClock.uptimeMillis();
        for (int k = 0; k < filledCount; k++) {
            state[order[k]] = FALLING;
            startedAt[order[k]] = now + k * FALL_STAGGER_MS;
        }
        host.invalidate();
    }

    /** Draws one slot centered on (x, y). */
    public void draw(@NonNull Canvas canvas, int index, float x, float y, float radius, int color) {
        paint.setColor(Theme.multAlpha(color, EMPTY_ALPHA));
        canvas.drawCircle(x, y, radius, paint);

        final long elapsed = SystemClock.uptimeMillis() - startedAt[index];
        switch (state[index]) {
            case POPPING:
                if (elapsed >= POP_MS) {
                    state[index] = FILLED;
                    drawFilled(canvas, x, y, radius, color);
                } else {
                    final float scale = BrinaMotion.sample(POP_TIME, POP_SCALE, elapsed);
                    final float bloom = BrinaMotion.sample(POP_TIME, POP_BLOOM, elapsed);
                    paint.setColor(Theme.multAlpha(color, 0.12f * bloom));
                    canvas.drawCircle(x, y, radius * scale * 1.55f, paint);
                    paint.setColor(Theme.multAlpha(color, 0.22f * bloom));
                    canvas.drawCircle(x, y, radius * scale * 1.25f, paint);
                    paint.setColor(color);
                    canvas.drawCircle(x, y, radius * scale, paint);
                    host.invalidate();
                }
                break;
            case FILLED:
                drawFilled(canvas, x, y, radius, color);
                break;
            case SHRINKING:
                if (elapsed >= SHRINK_MS) {
                    state[index] = EMPTY;
                } else {
                    final float f = 1f - elapsed / (float) SHRINK_MS;
                    paint.setColor(Theme.multAlpha(color, f));
                    canvas.drawCircle(x, y, radius * f, paint);
                    host.invalidate();
                }
                break;
            case FALLING:
                if (elapsed >= FALL_MS) {
                    state[index] = EMPTY;
                } else if (elapsed < 0) {
                    drawFilled(canvas, x, y, radius, color); // waiting its turn
                    host.invalidate();
                } else {
                    final float f = elapsed / (float) FALL_MS;
                    final float alpha = f < 0.5f ? 1f : 1f - (f - 0.5f) * 2f;
                    paint.setColor(Theme.multAlpha(color, alpha));
                    canvas.drawCircle(x, y + radius * FALL_RADII * f * (0.4f + 0.6f * f), radius * (1f - 0.2f * f), paint);
                    host.invalidate();
                }
                break;
            default:
                break;
        }
    }

    private void drawFilled(Canvas canvas, float x, float y, float radius, int color) {
        paint.setColor(color);
        canvas.drawCircle(x, y, radius, paint);
    }
}
