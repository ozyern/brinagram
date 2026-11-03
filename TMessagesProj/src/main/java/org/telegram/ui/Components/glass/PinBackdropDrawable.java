package org.telegram.ui.Components.glass;

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

/**
 * The luminous wallpaper behind PIN, code and phone entry: deep indigo crossed by two arcs of blue
 * light, with a soft white glow low on the screen. It is what gives the glass keys something to catch.
 */
public final class PinBackdropDrawable extends Drawable {

    private static final int BASE_TOP_DARK = 0xFF3A35C4, BASE_BOTTOM_DARK = 0xFF3F49D8;
    private static final int ARC_DARK = 0xFF0A6CF0, GLOW_DARK = 0xE6E4E6FF;
    private static final int BASE_TOP_LIGHT = 0xFFD9DCFF, BASE_BOTTOM_LIGHT = 0xFFC5CBFF;
    private static final int ARC_LIGHT = 0xFF8FC0FF, GLOW_LIGHT = 0xF2FFFFFF;

    private final boolean dark;
    private final Paint base = new Paint();
    private final Paint upperArc = new Paint(Paint.DITHER_FLAG);
    private final Paint lowerArc = new Paint(Paint.DITHER_FLAG);
    private final Paint glow = new Paint(Paint.DITHER_FLAG);
    private int alpha = 255;

    public PinBackdropDrawable(boolean dark) {
        this.dark = dark;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        final float w = Math.max(1, b.width()), h = Math.max(1, b.height());
        final int arc = dark ? ARC_DARK : ARC_LIGHT;
        base.setShader(new LinearGradient(0, b.top, 0, b.bottom,
            dark ? BASE_TOP_DARK : BASE_TOP_LIGHT,
            dark ? BASE_BOTTOM_DARK : BASE_BOTTOM_LIGHT,
            Shader.TileMode.CLAMP));
        // a ring is a radial gradient that is only opaque in a narrow band near its edge
        upperArc.setShader(ring(b.left + w * 1.02f, b.top + h * 0.62f, h * 0.56f, arc, 0.78f));
        lowerArc.setShader(ring(b.left + w * 1.18f, b.top + h * 1.02f, h * 0.48f, arc, 0.80f));
        glow.setShader(new RadialGradient(b.left + w * 0.60f, b.top + h * 0.84f, w * 0.72f,
            new int[] {dark ? GLOW_DARK : GLOW_LIGHT, multiply(dark ? GLOW_DARK : GLOW_LIGHT, 0.45f), 0x00FFFFFF},
            new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
    }

    private static RadialGradient ring(float cx, float cy, float radius, int color, float bandStart) {
        final float bandPeak = bandStart + (1f - bandStart) * 0.45f;
        return new RadialGradient(cx, cy, radius,
            new int[] {color & 0x00FFFFFF, color & 0x00FFFFFF, color, color & 0x00FFFFFF},
            new float[] {0f, bandStart, bandPeak, 1f}, Shader.TileMode.CLAMP);
    }

    private static int multiply(int color, float alphaFactor) {
        return ((int) ((color >>> 24) * alphaFactor) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect b = getBounds();
        if (b.isEmpty() || alpha == 0) {
            return;
        }
        base.setAlpha(alpha);
        upperArc.setAlpha(alpha);
        lowerArc.setAlpha(alpha);
        glow.setAlpha(alpha);
        canvas.drawRect(b, base);
        canvas.drawRect(b, upperArc);
        canvas.drawRect(b, lowerArc);
        canvas.drawRect(b, glow);
    }

    @Override
    public void setAlpha(int value) {
        if (alpha != value) {
            alpha = value;
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return alpha == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }
}
