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
 * Popup panel: a charcoal (or, on light themes, near-white) gradient with a thin lit rim.
 *
 * <p>It wraps the themed nine-patch, which keeps providing the soft shadow and the paddings that
 * popup positioning code relies on; the panel is painted over the nine-patch's inner area.
 */
public class GlassMenuDrawable extends Drawable {

    private final Drawable base;
    private final Rect padding = new Rect();
    private final RectF inner = new RectF();
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alpha = 255;
    private boolean dark = true;
    private boolean invisible;
    private float shaderTop = Float.NaN;
    private float shaderBottom = Float.NaN;

    public GlassMenuDrawable(@NonNull Drawable base) {
        this.base = base;
        base.getPadding(padding);
        rimPaint.setStyle(Paint.Style.STROKE);
    }

    /** Picks the dark or light panel to suit the color the popup was themed with. */
    public void setBaseColor(int color) {
        invisible = (color >>> 24) == 0;
        final boolean newDark = androidx.core.graphics.ColorUtils.calculateLuminance(color | 0xFF000000) < 0.5f;
        if (newDark != dark) {
            dark = newDark;
            shaderTop = Float.NaN;
        }
        invalidateSelf();
    }

    @Override
    public boolean getPadding(@NonNull Rect out) {
        out.set(padding);
        return true;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        base.setBounds(bounds);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        base.draw(canvas);
        if (invisible) {
            return;
        }
        final Rect b = getBounds();
        inner.set(b.left + padding.left, b.top + padding.top, b.right - padding.right, b.bottom - padding.bottom);
        if (inner.width() <= 0 || inner.height() <= 0) {
            return;
        }
        if (inner.top != shaderTop || inner.bottom != shaderBottom) {
            shaderTop = inner.top;
            shaderBottom = inner.bottom;
            fillPaint.setShader(new LinearGradient(0, inner.top, 0, inner.bottom,
                dark ? 0xFF262626 : 0xFFFFFFFF,
                dark ? 0xFF131313 : 0xFFF4F4F6,
                Shader.TileMode.CLAMP));
            rimPaint.setShader(new LinearGradient(0, inner.top, 0, inner.bottom,
                dark ? 0x38FFFFFF : 0xFFFFFFFF,
                dark ? 0x0DFFFFFF : 0x0D000000,
                Shader.TileMode.CLAMP));
        }
        final float r = dp(LiquidGlassStyle.MENU_RADIUS_DP);
        fillPaint.setAlpha(alpha);
        canvas.drawRoundRect(inner, r, r, fillPaint);

        final float stroke = AndroidUtilities.dpf2(1f);
        final float inset = stroke / 2f;
        rimPaint.setStrokeWidth(stroke);
        rimPaint.setAlpha(alpha);
        inner.inset(inset, inset);
        canvas.drawRoundRect(inner, r - inset, r - inset, rimPaint);
    }

    @Override
    public void setAlpha(int value) {
        alpha = value;
        base.setAlpha(value);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        base.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
