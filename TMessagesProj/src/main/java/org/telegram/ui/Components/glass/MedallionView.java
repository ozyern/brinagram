package org.telegram.ui.Components.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.R;
import org.telegram.ui.Components.CubicBezierInterpolator;

/**
 * The BrinaGram medallion for the welcome screen: a warm glow behind it, a gentle floating
 * motion, and a specular glint that sweeps across every few seconds.
 */
public class MedallionView extends View {

    private static final long FLOAT_PERIOD = 4200;
    private static final long GLINT_PERIOD = 4800;
    private static final long GLINT_DURATION = 900;

    private final Drawable medallion;
    private final GlintDrawable glint = new GlintDrawable(0.5f);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long startTime = SystemClock.elapsedRealtime();
    private boolean attached;

    public MedallionView(Context context) {
        super(context);
        medallion = ContextCompat.getDrawable(context, R.drawable.brina_medallion);
        glint.setCallback(this);
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == glint || super.verifyDrawable(who);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final float glowRadius = Math.min(w, h) * 0.62f;
        glowPaint.setShader(new RadialGradient(0, 0, glowRadius,
            new int[] {0x66E8C690, 0x22E8C690, 0x00E8C690},
            new float[] {0.55f, 0.78f, 1f}, Shader.TileMode.CLAMP));
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
        if (medallion == null) {
            return;
        }
        final boolean animate = LiteMode.isEnabled(LiteMode.FLAG_CHAT_BACKGROUND);
        final long t = SystemClock.elapsedRealtime() - startTime;

        final float size = Math.min(getWidth(), getHeight()) * 0.9f;
        final float cx = getWidth() / 2f;
        final float floatY = animate ? (float) Math.sin(t * 2 * Math.PI / FLOAT_PERIOD) * AndroidUtilities.dpf2(4) : 0;
        final float cy = getHeight() / 2f + floatY;

        canvas.save();
        canvas.translate(cx, cy);
        canvas.drawCircle(0, 0, size * 0.62f, glowPaint);
        canvas.restore();

        final int l = (int) (cx - size / 2), tp = (int) (cy - size / 2);
        medallion.setBounds(l, tp, (int) (l + size), (int) (tp + size));
        medallion.draw(canvas);

        if (animate) {
            final long phase = t % GLINT_PERIOD;
            final float g = phase < GLINT_DURATION ? CubicBezierInterpolator.EASE_BOTH.getInterpolation(phase / (float) GLINT_DURATION) : 0f;
            glint.setBounds(medallion.getBounds());
            glint.setProgress(g);
            glint.draw(canvas);
            if (attached) {
                postInvalidateOnAnimation();
            }
        }
    }
}
