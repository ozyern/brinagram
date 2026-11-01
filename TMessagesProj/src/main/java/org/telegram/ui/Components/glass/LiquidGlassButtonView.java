package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import org.telegram.ui.ActionBar.Theme;

/**
 * A round control made of glass: the back button (and other bar icons), as in Exhale's
 * LiquidGlassIconButton / ColorOS.
 *
 * <p>A 44dp disc with a light film and a specular rim. Pressed, the disc squishes to 0.92 on an
 * underdamped spring, follows the finger about a third of its width (tanh, so it never slides out
 * from under the thumb) and lights up with a white film. The glyph squishes less (0.96) so it reads
 * as sitting under the pressed surface. No ripple.
 */
public class LiquidGlassButtonView extends ImageView {

    /** How far the pane follows a dragging finger, as a share of its size. */
    private static final float FOLLOW_REACH = 0.38f;
    /** How much white the pane takes on while held. */
    private static final float PRESS_LIGHT = 0.14f;

    private final Theme.ResourcesProvider resourcesProvider;
    private final GlassCircleDrawable disc;
    private int diameter = dp(44);

    private float pressProgress;
    private float offsetX, offsetY;
    private float downX, downY;

    private final SpringAnimation pressSpring = new SpringAnimation(this, new FloatPropertyCompat<LiquidGlassButtonView>("press") {
        @Override
        public float getValue(LiquidGlassButtonView object) {
            return object.pressProgress;
        }

        @Override
        public void setValue(LiquidGlassButtonView object, float value) {
            object.pressProgress = value;
            object.invalidate();
        }
    }).setSpring(new SpringForce().setDampingRatio(0.5f).setStiffness(300f));

    public LiquidGlassButtonView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.disc = new GlassCircleDrawable(resourcesProvider);
        pressSpring.setMinimumVisibleChange(0.001f);
        setScaleType(ScaleType.CENTER);
    }

    /** The disc is dark glass behind a light glyph and light glass behind a dark one. */
    public void setGlyphColor(int color) {
        disc.setDarkGlass(androidx.core.graphics.ColorUtils.calculateLuminance(color | 0xFF000000) > 0.5f);
        invalidate();
    }

    public void setDiameter(int px) {
        diameter = px;
        invalidate();
    }

    /** The glass draws its own press feedback; Telegram's ripple backgrounds are ignored. */
    @Override
    public void setBackground(Drawable background) {
        super.setBackground(null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(null);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                offsetX = offsetY = 0;
                if (isEnabled()) {
                    pressSpring.animateToFinalPosition(1f);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                offsetX = event.getX() - downX;
                offsetY = event.getY() - downY;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pressSpring.animateToFinalPosition(0f);
                offsetX = offsetY = 0;
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final float press = pressProgress;
        final float cx = getWidth() / 2f, cy = getHeight() / 2f;
        final float reach = diameter;
        final float tx = (float) (reach * FOLLOW_REACH * Math.tanh(offsetX / reach)) * Math.min(1f, Math.max(0f, press));
        final float ty = (float) (reach * FOLLOW_REACH * Math.tanh(offsetY / reach)) * Math.min(1f, Math.max(0f, press));

        // the pane
        final float squish = lerp(1f, 0.92f, press);
        canvas.save();
        canvas.translate(tx, ty);
        canvas.scale(squish, squish, cx, cy);
        final int r = diameter / 2;
        disc.setBounds((int) (cx - r), (int) (cy - r), (int) (cx + r), (int) (cy + r));
        disc.setPressLight(PRESS_LIGHT * Math.max(0f, press));
        disc.setAlpha(isEnabled() ? 255 : 102);
        disc.draw(canvas);
        canvas.restore();

        // the glyph, giving less than the glass around it
        final float glyphSquish = lerp(1f, 0.96f, press);
        canvas.save();
        canvas.translate(tx, ty);
        canvas.scale(glyphSquish, glyphSquish, cx, cy);
        super.onDraw(canvas);
        canvas.restore();
    }
}
