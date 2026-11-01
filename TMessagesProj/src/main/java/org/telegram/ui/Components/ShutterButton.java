/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import androidx.annotation.Keep;

public class ShutterButton extends View {

    public enum State {
        DEFAULT,
        RECORDING
    }

    private final static int LONG_PRESS_TIME = 800;

    private Drawable shadowDrawable;

    private DecelerateInterpolator interpolator = new DecelerateInterpolator();
    private Paint whitePaint;
    private Paint redPaint;
    private ShutterButtonDelegate delegate;
    private State state;
    private boolean pressed;
    private float redProgress;
    private long lastUpdateTime;
    private long totalTime;
    private boolean processRelease;

    private Runnable longPressed = new Runnable() {
        public void run() {
            if (delegate != null) {
                if (!delegate.shutterLongPressed()) {
                    processRelease = false;
                }
            }
        }
    };

    public interface ShutterButtonDelegate {
        boolean shutterLongPressed();
        void shutterReleased();
        void shutterCancel();
        boolean onTranslationChanged(float x, float y);
    }

    public ShutterButton(Context context) {
        super(context);
        shadowDrawable = getResources().getDrawable(R.drawable.camera_btn);
        whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setStyle(Paint.Style.FILL);
        whitePaint.setColor(0xffffffff);
        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setStyle(Paint.Style.FILL);
        redPaint.setColor(0xffcd4747);
        state = State.DEFAULT;
    }

    private boolean videoMode;

    /** Video mode: a tap starts recording and the next tap stops it (no hold needed). */
    public void setVideoMode(boolean videoMode) {
        this.videoMode = videoMode;
    }

    public boolean isVideoMode() {
        return videoMode;
    }

    public void setDelegate(ShutterButtonDelegate shutterButtonDelegate) {
        delegate = shutterButtonDelegate;
    }

    public ShutterButtonDelegate getDelegate() {
        return delegate;
    }

    private void setHighlighted(boolean value) {
        AnimatorSet animatorSet = new AnimatorSet();
        if (value) {
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, View.SCALE_X, 1.06f),
                    ObjectAnimator.ofFloat(this, View.SCALE_Y, 1.06f));
        } else {
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, View.SCALE_X, 1.0f),
                    ObjectAnimator.ofFloat(this, View.SCALE_Y, 1.0f));
            animatorSet.setStartDelay(40);
        }
        animatorSet.setDuration(120);
        animatorSet.setInterpolator(interpolator);
        animatorSet.start();
    }

    @Keep
    @Override
    public void setScaleX(float scaleX) {
        super.setScaleX(scaleX);
        invalidate();
    }

    public State getState() {
        return state;
    }

    // BrinaGram camera shutter: a thin translucent ring around a glowing orange disc that
    // dips when pressed and morphs into a red rounded square while recording.
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.RectF recordRect = new android.graphics.RectF();
    {
        ringPaint.setStyle(Paint.Style.STROKE);
    }

    private void drawBrinaShutter(Canvas canvas, int cx, int cy) {
        final float press = Math.max(0f, Math.min(1f, (getScaleX() - 1.0f) / 0.06f));
        final float ringR = AndroidUtilities.dp(36);
        ringPaint.setStrokeWidth(AndroidUtilities.dpf2(3f));
        ringPaint.setColor(0x66FFFFFF);
        canvas.drawCircle(cx, cy, ringR - AndroidUtilities.dpf2(1.5f), ringPaint);

        final float record = state == State.RECORDING ? redProgress : 0f;
        final float discR = AndroidUtilities.dp(30) * (1f - 0.08f * press);
        discPaint.setShader(new android.graphics.LinearGradient(cx, cy - discR, cx, cy + discR,
            androidx.core.graphics.ColorUtils.blendARGB(0xFFF6A44E, 0xFFFF453A, record),
            androidx.core.graphics.ColorUtils.blendARGB(0xFFE8742A, 0xFFE0322B, record),
            android.graphics.Shader.TileMode.CLAMP));
        discPaint.setShadowLayer(AndroidUtilities.dp(12), 0, 0, androidx.core.graphics.ColorUtils.blendARGB(0x66F08A3C, 0x66FF453A, record));
        // recording: shrink to a 26dp rounded square
        final float half = discR + (AndroidUtilities.dp(13) - discR) * record;
        final float corner = half * (1f - 0.65f * record);
        recordRect.set(cx - half, cy - half, cx + half, cy + half);
        canvas.drawRoundRect(recordRect, corner, corner, discPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int cx = getMeasuredWidth() / 2;
        int cy = getMeasuredHeight() / 2;

        if (state == State.RECORDING && redProgress != 1.0f) {
            long dt = Math.min(17, Math.abs(System.currentTimeMillis() - lastUpdateTime));
            lastUpdateTime = System.currentTimeMillis();
            totalTime = Math.min(240, totalTime + dt);
            redProgress = interpolator.getInterpolation(totalTime / 240.0f);
            invalidate();
        }
        drawBrinaShutter(canvas, cx, cy);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(AndroidUtilities.dp(84), AndroidUtilities.dp(84));
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!videoMode) {
                    AndroidUtilities.runOnUIThread(longPressed, LONG_PRESS_TIME);
                }
                pressed = true;
                processRelease = true;
                setHighlighted(true);
                break;
            case MotionEvent.ACTION_UP:
                setHighlighted(false);
                AndroidUtilities.cancelRunOnUIThread(longPressed);
                if (videoMode && state != State.RECORDING) {
                    delegate.shutterLongPressed(); // start recording
                } else if (processRelease) {
                    delegate.shutterReleased();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x >= 0 && x <= getMeasuredWidth() ? 0 : x;
                float dy = y >= 0 && y <= getMeasuredHeight() ? 0 : y;
                if (delegate.onTranslationChanged(dx, dy)) {
                    AndroidUtilities.cancelRunOnUIThread(longPressed);
                    if (state == State.RECORDING) {
                        processRelease = false;
                        setHighlighted(false);
                        delegate.shutterCancel();
                        setState(State.DEFAULT, true);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                setHighlighted(false);
                pressed = false;
        }
        return true;
    }

    public void setState(State value, boolean animated) {
        if (state != value) {
            state = value;
            if (animated) {
                lastUpdateTime = System.currentTimeMillis();
                totalTime = 0;
                if (state != State.RECORDING) {
                    redProgress = 0.0f;
                }
            } else {
                if (state == State.RECORDING) {
                    redProgress = 1.0f;
                } else {
                    redProgress = 0.0f;
                }
            }
            invalidate();
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Button");
        info.setClickable(true);
        info.setLongClickable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.getId(), LocaleController.getString(R.string.AccActionTakePicture)));
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK.getId(), LocaleController.getString(R.string.AccActionRecordVideo)));
        }
    }
}
