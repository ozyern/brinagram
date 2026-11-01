package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.ui.ActionBar.Theme;

/**
 * A disc of glass for round bar controls (back button, toolbar icons): a light film (16% dark /
 * 20% light), a soft drop shadow and a specular rim lit from the
 * top-left. Can also act as a pressed-state background via {@link #setPressLight}.
 */
public class GlassCircleDrawable extends Drawable {

    private final Theme.ResourcesProvider resourcesProvider;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float pressLight;
    private int alpha = 255;
    private boolean pressedFromState;

    public GlassCircleDrawable(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
        rim.setStyle(Paint.Style.STROKE);
        shadow.setColor(0);
    }

    private Boolean darkGlass;
    private int fixedDiameter;

    /** Draws a disc of this size centered in the bounds instead of filling them. */
    public GlassCircleDrawable setFixedDiameter(int px) {
        fixedDiameter = px;
        invalidateSelf();
        return this;
    }

    /** Overrides the theme: dark glass (for light glyphs) or light glass (for dark glyphs). */
    public void setDarkGlass(boolean darkGlass) {
        this.darkGlass = darkGlass;
        invalidateSelf();
    }

    public void setPressLight(float pressLight) {
        if (this.pressLight != pressLight) {
            this.pressLight = pressLight;
            invalidateSelf();
        }
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(@NonNull int[] state) {
        boolean pressed = false;
        for (int s : state) {
            if (s == android.R.attr.state_pressed) {
                pressed = true;
                break;
            }
        }
        if (pressed != pressedFromState) {
            pressedFromState = pressed;
            invalidateSelf();
            return true;
        }
        return false;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        rim.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
            new int[] {0x99FFFFFF, 0x0DFFFFFF, 0x0DFFFFFF, 0x55FFFFFF},
            new float[] {0f, 0.4f, 0.6f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        final boolean dark = darkGlass != null ? darkGlass : GlassTabView.isGlassDark(resourcesProvider);
        final float cx = b.exactCenterX(), cy = b.exactCenterY();
        final float r = fixedDiameter > 0 ? Math.min(fixedDiameter, Math.min(b.width(), b.height())) / 2f : Math.min(b.width(), b.height()) / 2f;
        final float a = alpha / 255f;

        shadow.setShadowLayer(dp(6), 0, dp(1.5f), Theme.multAlpha(0x33000000, a));
        canvas.drawCircle(cx, cy, r - dpf2(0.5f), shadow);

        final int base = dark ? 0xFFFFFFFF : 0xFFFFFFFF;
        fill.setColor(Theme.multAlpha(dark ? 0xFFFFFFFF : base, (dark ? 0.16f : 0.80f) * a));
        canvas.drawCircle(cx, cy, r, fill);
        if (!dark) {
            fill.setColor(Theme.multAlpha(0xFF000000, 0.04f * a));
            canvas.drawCircle(cx, cy, r, fill);
        }

        final float press = Math.max(pressLight, pressedFromState ? 0.14f : 0f);
        if (press > 0f) {
            light.setColor(Theme.multAlpha(dark ? 0xFFFFFFFF : 0xFF000000, (dark ? press : press * 0.5f) * a));
            canvas.drawCircle(cx, cy, r, light);
        }

        final float stroke = dpf2(1f);
        rim.setStrokeWidth(stroke);
        rim.setAlpha((int) (255 * a * (dark ? 0.9f : 0.75f)));
        canvas.drawCircle(cx, cy, r - stroke / 2f, rim);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        fill.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
