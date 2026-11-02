package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

/**
 * A pane of liquid glass for buttons, keys and fields: a translucent film, a lit rim that is
 * brightest along the top, and a soft band of light under the top edge. Lights up while pressed.
 */
public class GlassPillDrawable extends Drawable {

    private final RectF rect = new RectF();
    private final RectF band = new RectF();
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean dark = true;
    private boolean pressed;
    private int alpha = 255;
    private boolean shadersValid;

    public GlassPillDrawable() {
        rimPaint.setStyle(Paint.Style.STROKE);
    }

    public GlassPillDrawable setDark(boolean value) {
        if (dark != value) {
            dark = value;
            shadersValid = false;
            invalidateSelf();
        }
        return this;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        shadersValid = false;
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
        if (nowPressed != pressed) {
            pressed = nowPressed;
            shadersValid = false;
            invalidateSelf();
            return true;
        }
        return false;
    }

    private void buildShaders() {
        final Rect b = getBounds();
        final int fillTop, fillBottom;
        if (dark) {
            fillTop = pressed ? 0x59FFFFFF : 0x2EFFFFFF;
            fillBottom = pressed ? 0x38FFFFFF : 0x14FFFFFF;
        } else {
            fillTop = pressed ? 0xFFFFFFFF : 0xD9FFFFFF;
            fillBottom = pressed ? 0xE6FFFFFF : 0x99FFFFFF;
        }
        fillPaint.setShader(new LinearGradient(0, b.top, 0, b.bottom, fillTop, fillBottom, Shader.TileMode.CLAMP));
        rimPaint.setShader(new LinearGradient(0, b.top, 0, b.bottom,
            dark ? 0x73FFFFFF : 0xFFFFFFFF,
            dark ? 0x1AFFFFFF : 0x1F000000,
            Shader.TileMode.CLAMP));
        final float bandWidth = b.width() * 0.7f;
        final float cx = b.exactCenterX();
        bandPaint.setShader(new LinearGradient(cx - bandWidth / 2f, 0, cx + bandWidth / 2f, 0,
            new int[] {0, dark ? 0x38FFFFFF : 0x66FFFFFF, 0}, null, Shader.TileMode.CLAMP));
        shadersValid = true;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        if (!shadersValid) {
            buildShaders();
        }
        rect.set(b);
        final float r = Math.min(rect.width(), rect.height()) / 2f;
        fillPaint.setAlpha(alpha);
        canvas.drawRoundRect(rect, r, r, fillPaint);

        final float bandHeight = Math.max(dp(2), rect.height() * 0.09f);
        final float bandWidth = rect.width() * 0.7f;
        band.set(rect.centerX() - bandWidth / 2f, rect.top + dp(2), rect.centerX() + bandWidth / 2f, rect.top + dp(2) + bandHeight);
        bandPaint.setAlpha(alpha);
        canvas.drawRoundRect(band, bandHeight / 2f, bandHeight / 2f, bandPaint);

        final float stroke = AndroidUtilities.dpf2(1f);
        rimPaint.setStrokeWidth(stroke);
        rimPaint.setAlpha(alpha);
        rect.inset(stroke / 2f, stroke / 2f);
        canvas.drawRoundRect(rect, r - stroke / 2f, r - stroke / 2f, rimPaint);
    }

    @Override
    public void setAlpha(int value) {
        alpha = value;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
