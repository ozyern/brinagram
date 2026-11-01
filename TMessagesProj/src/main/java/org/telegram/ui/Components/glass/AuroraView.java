package org.telegram.ui.Components.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.messenger.LiteMode;

/**
 * Slowly drifting, translucent warm light blobs (champagne, gold, rose) used behind the
 * BrinaGram welcome screen. Blobs follow independent Lissajous paths, so the motion never
 * visibly loops. Animation stops while detached and when animations are disabled in Lite mode.
 */
public class AuroraView extends View {

    private static final int BLOBS = 3;
    /** x/y path frequencies (cycles per 10s) and phases per blob. */
    private static final float[][] PATHS = {
        {0.31f, 0.23f, 0.0f, 1.3f},
        {0.19f, 0.29f, 2.1f, 0.4f},
        {0.27f, 0.17f, 4.2f, 2.6f},
    };
    /** blob centers (fraction of width/height) and drift amplitude (fraction). */
    private static final float[][] LAYOUT = {
        {0.18f, 0.22f, 0.22f, 0.12f},
        {0.85f, 0.40f, 0.20f, 0.14f},
        {0.40f, 0.86f, 0.26f, 0.10f},
    };
    private static final int[] LIGHT = {0x4DE8C690, 0x38F3B8B0, 0x40F6E3C4};
    private static final int[] DARK = {0x598C6A3E, 0x406E3B3B, 0x4D5A4026};

    private final Paint[] paints = new Paint[BLOBS];
    private final long startTime = SystemClock.elapsedRealtime();
    private boolean dark;
    private boolean attached;

    public AuroraView(Context context) {
        super(context);
        for (int i = 0; i < BLOBS; i++) {
            paints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
    }

    public void setDark(boolean dark) {
        if (this.dark != dark) {
            this.dark = dark;
            rebuildShaders(getWidth(), getHeight());
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuildShaders(w, h);
    }

    private void rebuildShaders(int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final float radius = Math.max(w, h) * 0.55f;
        final int[] colors = dark ? DARK : LIGHT;
        for (int i = 0; i < BLOBS; i++) {
            final int c = colors[i];
            paints[i].setShader(new RadialGradient(0, 0, radius,
                new int[] {c, c & 0x00FFFFFF | ((c >>> 25) << 24), c & 0x00FFFFFF},
                new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        final boolean animate = LiteMode.isEnabled(LiteMode.FLAG_CHAT_BACKGROUND);
        final float t = animate ? (SystemClock.elapsedRealtime() - startTime) / 10000f * (float) (Math.PI * 2) : 0f;
        for (int i = 0; i < BLOBS; i++) {
            final float[] p = PATHS[i], l = LAYOUT[i];
            final float cx = w * (l[0] + l[2] * (float) Math.sin(t * p[0] + p[2]));
            final float cy = h * (l[1] + l[3] * (float) Math.sin(t * p[1] + p[3]));
            canvas.save();
            canvas.translate(cx, cy);
            canvas.drawCircle(0, 0, Math.max(w, h) * 0.55f, paints[i]);
            canvas.restore();
        }
        if (animate && attached) {
            postInvalidateOnAnimation();
        }
    }
}
