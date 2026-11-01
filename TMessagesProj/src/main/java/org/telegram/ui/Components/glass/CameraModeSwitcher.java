package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;

/**
 * Camera mode switcher: "VIDEO  PHOTO" in bold caps, the active mode in the accent color on a
 * dark pill that springs between them. Tap a mode or swipe sideways to change it.
 */
@SuppressLint("ViewConstructor")
public class CameraModeSwitcher extends View {

    public static final int MODE_VIDEO = 0;
    public static final int MODE_PHOTO = 1;

    private static final int ACCENT = 0xFFF08A3C;

    private final String[] labels;
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pill = new RectF();
    private final float[] centers = new float[2];
    private final float[] widths = new float[2];
    private final Utilities.Callback<Integer> onModeChanged;
    private int mode = MODE_PHOTO;
    private float position = MODE_PHOTO;
    private float downX;

    private final SpringAnimation spring = new SpringAnimation(this, new FloatPropertyCompat<CameraModeSwitcher>("position") {
        @Override
        public float getValue(CameraModeSwitcher object) {
            return object.position;
        }

        @Override
        public void setValue(CameraModeSwitcher object, float value) {
            object.position = value;
            object.invalidate();
        }
    }).setSpring(new SpringForce().setStiffness(500f).setDampingRatio(0.75f));

    public CameraModeSwitcher(Context context, Utilities.Callback<Integer> onModeChanged) {
        super(context);
        this.onModeChanged = onModeChanged;
        labels = new String[] {
            LocaleController.getString(R.string.BrinaCameraVideo).toUpperCase(),
            LocaleController.getString(R.string.BrinaCameraPhoto).toUpperCase()
        };
        textPaint.setTypeface(AndroidUtilities.bold());
        textPaint.setTextSize(dp(13));
        pillPaint.setColor(0xCC1C1C1E);
        spring.setMinimumVisibleChange(0.001f);
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode, boolean animated) {
        if (this.mode == mode) {
            return;
        }
        this.mode = mode;
        if (animated) {
            spring.animateToFinalPosition(mode);
        } else {
            spring.cancel();
            position = mode;
            invalidate();
        }
        if (onModeChanged != null) {
            onModeChanged.run(mode);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(32));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final float gap = dp(24);
        widths[0] = textPaint.measureText(labels[0]);
        widths[1] = textPaint.measureText(labels[1]);
        // the active mode sits in the middle; the other one beside it
        centers[MODE_PHOTO] = w / 2f;
        centers[MODE_VIDEO] = w / 2f - (widths[0] / 2f + gap + widths[1] / 2f);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                return true;
            case MotionEvent.ACTION_UP: {
                final float dx = e.getX() - downX;
                if (Math.abs(dx) > dp(24)) {
                    setMode(dx > 0 ? MODE_VIDEO : MODE_PHOTO, true);
                } else {
                    // tap: the label under the finger, in the shifted coordinates
                    final float shift = getWidth() / 2f - lerp(centers[MODE_VIDEO], centers[MODE_PHOTO], position);
                    final float x = e.getX() - shift;
                    setMode(Math.abs(x - centers[MODE_VIDEO]) < Math.abs(x - centers[MODE_PHOTO]) ? MODE_VIDEO : MODE_PHOTO, true);
                }
                return true;
            }
        }
        return super.onTouchEvent(e);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final float cy = getHeight() / 2f;
        // the strip slides so the active label is centered
        final float shift = getWidth() / 2f - lerp(centers[MODE_VIDEO], centers[MODE_PHOTO], position);
        final float pillW = lerp(widths[0], widths[1], position) + dp(28);
        final float pillH = dp(28);
        pill.set(getWidth() / 2f - pillW / 2f, cy - pillH / 2f, getWidth() / 2f + pillW / 2f, cy + pillH / 2f);
        canvas.drawRoundRect(pill, pillH / 2f, pillH / 2f, pillPaint);

        final float baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        for (int i = 0; i < 2; i++) {
            final float active = 1f - Math.min(1f, Math.abs(position - i));
            textPaint.setColor(androidx.core.graphics.ColorUtils.blendARGB(0xFFFFFFFF, ACCENT, active));
            canvas.drawText(labels[i], centers[i] + shift - widths[i] / 2f, baseline, textPaint);
        }
    }
}
