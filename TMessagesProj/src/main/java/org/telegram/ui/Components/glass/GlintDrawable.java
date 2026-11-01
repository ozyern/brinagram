package org.telegram.ui.Components.glass;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A soft diagonal specular band that sweeps across a circular area, like light catching glass
 * or polished metal. Drive it with {@link #setProgress(float)} from 0 (before) to 1 (after).
 */
public class GlintDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final Path clip = new Path();
    private final float clipRadiusFraction;
    private LinearGradient gradient;
    private float progress;

    /**
     * @param clipRadiusFraction radius of the circular clip as a fraction of the bounds width
     *                           (0.5 = full inscribed circle).
     */
    public GlintDrawable(float clipRadiusFraction) {
        this.clipRadiusFraction = clipRadiusFraction;
    }

    public void setProgress(float progress) {
        if (this.progress != progress) {
            this.progress = progress;
            invalidateSelf();
        }
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        final float band = bounds.width() * 0.35f;
        gradient = new LinearGradient(-band, 0, band, 0,
            new int[] {0x00FFFFFF, 0x66FFF6E8, 0xCCFFFFFF, 0x66FFF6E8, 0x00FFFFFF},
            new float[] {0f, 0.35f, 0.5f, 0.65f, 1f},
            Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        clip.rewind();
        clip.addCircle(bounds.exactCenterX(), bounds.exactCenterY(), bounds.width() * clipRadiusFraction, Path.Direction.CW);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (gradient == null || progress <= 0f || progress >= 1f) {
            return;
        }
        final Rect b = getBounds();
        final float travel = b.width() * 1.6f;
        matrix.reset();
        matrix.setRotate(-35f);
        matrix.postTranslate(b.exactCenterX() - travel / 2f + travel * progress, b.exactCenterY());
        gradient.setLocalMatrix(matrix);
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(b, paint);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
