package org.telegram.ui.Components.glass;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * A round key of a PIN pad: a film of glass with a rim lit from above that floods with white light
 * while pressed. {@link #lightOnly()} leaves out the glass, for keys that have their own.
 */
public final class PinKeyDrawable extends Drawable {

    private static final long LIGHT_IN_MS = 70;
    private static final long LIGHT_OUT_MS = 170;
    private static final float LIGHT_ALPHA = 0.85f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean dark;
    private boolean lightOnly;
    private boolean pressed;
    private float light;
    private ValueAnimator animator;

    public PinKeyDrawable(boolean dark) {
        this.dark = dark;
        rim.setStyle(Paint.Style.STROKE);
    }

    public PinKeyDrawable lightOnly() {
        lightOnly = true;
        return this;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        rim.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
            dark ? 0x66FFFFFF : 0x40000000,
            dark ? 0x26FFFFFF : 0x14000000,
            Shader.TileMode.CLAMP));
        final float r = Math.max(1f, Math.min(bounds.width(), bounds.height()) / 2f);
        lightPaint.setShader(new RadialGradient(bounds.exactCenterX(), bounds.exactCenterY(), r,
            new int[] {0xFFFFFFFF, 0xFFFFFFFF, 0xCCDDEEFF}, new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(@NonNull int[] state) {
        boolean nowPressed = false;
        for (int s : state) {
            if (s == android.R.attr.state_pressed) {
                nowPressed = true;
                break;
            }
        }
        if (nowPressed == pressed) {
            return false;
        }
        pressed = nowPressed;
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(light, nowPressed ? 1f : 0f);
        animator.setDuration(nowPressed ? LIGHT_IN_MS : LIGHT_OUT_MS);
        animator.addUpdateListener(a -> {
            light = (float) a.getAnimatedValue();
            invalidateSelf();
        });
        animator.start();
        return true;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        final float cx = b.exactCenterX(), cy = b.exactCenterY();
        final float r = Math.min(b.width(), b.height()) / 2f;
        if (!lightOnly) {
            fill.setColor(dark ? 0x24FFFFFF : 0x0F000000);
            canvas.drawCircle(cx, cy, r, fill);
            final float stroke = AndroidUtilities.dpf2(1f);
            rim.setStrokeWidth(stroke);
            canvas.drawCircle(cx, cy, r - stroke / 2f, rim);
        }
        if (light > 0.001f) {
            lightPaint.setAlpha((int) (0xFF * LIGHT_ALPHA * light));
            canvas.drawCircle(cx, cy, r, lightPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        rim.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
