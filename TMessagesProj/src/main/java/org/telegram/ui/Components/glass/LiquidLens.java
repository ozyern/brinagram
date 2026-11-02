package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * The moving glass selection used by tab strips, attach bars and menus.
 *
 * <p>The lens follows a target rectangle on a spring and behaves like a drop of glass. Held or
 * slowing down it is a tall bubble, standing past the track it rides in, with bright bands along
 * its top and bottom; at speed it stretches long and flat and clears completely. Chromatic fringes
 * light both ends, and what lies just outside the rim is bent back inside it. It also reports a
 * {@linkplain #getTugX() tug} so the host can nudge its whole track along the direction of travel.
 *
 * <p>Per frame the host calls {@link #setTarget}, then {@link #step}, then draws in this order:
 * {@link #drawBody}, its content (optionally through {@link #drawRefracted}), {@link #drawGlass}.
 * The lens invalidates the host until it has settled.
 */
public final class LiquidLens {

    /** Spring-like curve for one-shot animations: fast, with one small overshoot. */
    public static final Interpolator SPRING = t -> {
        if (t >= 1f) {
            return 1f;
        }
        return (float) (1.0 - Math.exp(-6.4 * t) * Math.cos(7.2 * t));
    };

    private static final float MOVE_DAMPING_RATIO = 0.74f;
    private static final float SWELL_STIFFNESS = 250f;
    private static final float SWELL_DAMPING = 2f * 0.6f * (float) Math.sqrt(SWELL_STIFFNESS);
    /** How much of the swell height speed takes away: the drop flattens as it stretches. */
    private static final float FLATTEN = 0.75f;
    /** The rim's bent band, as a fraction of the lens width at each end. */
    private static final float RIM_BAND = 0.17f;

    private final View host;

    private float stiffness = 340f;
    private float damping = 2f * MOVE_DAMPING_RATIO * (float) Math.sqrt(stiffness);

    /** Swell proportions, relative to the resting size. */
    private float swellWidth = 0.18f;
    private float swellHeight = 0.6f;
    /** Extra length at full speed, relative to the resting width. */
    private float stretchWidth = 0.85f;
    /** How far the resting fill clears as the lens swells. */
    private float clearing = 0.7f;
    private float refraction = 1.1f;
    private float tugDp = 4f;
    private float speedForFullSwell = dp(700);

    private boolean initialized;
    private float x, vx, width, vw;
    private float targetX, targetWidth, centerY, restHeight;
    private float swell, vSwell;
    private float stretch, vStretch;
    private float visibility = 1f;
    private float targetVisibility = 1f;
    private boolean pressed;
    private boolean settled = true;
    private long lastNanos;

    private final RectF rect = new RectF();
    private final RectF band = new RectF();
    private final RectF innerRect = new RectF();
    private final Path path = new Path();
    private final Path innerPath = new Path();
    private float radius;

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fringePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LiquidLens(@NonNull View host) {
        this.host = host;
        rimPaint.setStyle(Paint.Style.STROKE);
        fringePaint.setStyle(Paint.Style.STROKE);
    }

    /** Sets how far the lens swells, as fractions of its resting width and height. */
    public LiquidLens setSwell(float widthFraction, float heightFraction) {
        swellWidth = widthFraction;
        swellHeight = heightFraction;
        return this;
    }

    /** Sets how much longer the lens gets at full speed, as a fraction of its resting width. */
    public LiquidLens setStretch(float widthFraction) {
        stretchWidth = widthFraction;
        return this;
    }

    /** Sets how far the resting fill clears as the lens swells: 0 keeps it, 1 clears it entirely. */
    public LiquidLens setClearing(float fraction) {
        clearing = fraction;
        return this;
    }

    /** Sets the stiffness of the spring that carries the lens to its target. */
    public LiquidLens setStiffness(float value) {
        stiffness = value;
        damping = 2f * MOVE_DAMPING_RATIO * (float) Math.sqrt(value);
        return this;
    }

    public LiquidLens setRefraction(float magnification) {
        refraction = magnification;
        return this;
    }

    public LiquidLens setTugDp(float value) {
        tugDp = value;
        return this;
    }

    /**
     * @param glue keep the lens stuck to the target once settled, for targets that move because
     *             their list is scrolling rather than because the selection changed
     */
    public void setTarget(float left, float top, float right, float bottom, boolean glue) {
        targetX = (left + right) / 2f;
        targetWidth = right - left;
        centerY = (top + bottom) / 2f;
        restHeight = bottom - top;
        if (!initialized || visibility <= 0.01f || (glue && settled)) {
            snap();
        } else if (Math.abs(targetX - x) > 0.5f || Math.abs(targetWidth - width) > 0.5f) {
            settled = false;
        }
    }

    public void snap() {
        initialized = true;
        x = targetX;
        width = targetWidth;
        vx = vw = 0f;
    }

    public void setPressed(boolean value) {
        if (pressed != value) {
            pressed = value;
            settled = false;
            host.invalidate();
        }
    }

    /** Fades the lens in and out; it jumps to its target instead of sliding while hidden. */
    public void setVisible(boolean visible, boolean animated) {
        targetVisibility = visible ? 1f : 0f;
        if (!animated) {
            visibility = targetVisibility;
        }
        settled = false;
        host.invalidate();
    }

    public boolean isVisible() {
        return visibility > 0.01f;
    }

    public boolean isSettled() {
        return settled;
    }

    public RectF getRect() {
        return rect;
    }

    /** Offset for the host's track: a few dp along the direction of travel. */
    public float getTugX() {
        if (speedForFullSwell <= 0f) {
            return 0f;
        }
        final float fraction = Math.max(-1f, Math.min(1f, vx / speedForFullSwell));
        return dp(tugDp) * fraction;
    }

    /** Advances the physics to now and lays out the lens. Call once per drawn frame. */
    public void step() {
        final long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : (now - lastNanos) / 1e9f;
        lastNanos = now;
        if (dt > 1f / 30f) {
            dt = 1f / 30f; // the first frame after a pause, or a dropped frame
        }
        if (!settled && dt > 0f) {
            final float h = dt / 2f;
            for (int i = 0; i < 2; i++) {
                vx += (stiffness * (targetX - x) - damping * vx) * h;
                x += vx * h;
                vw += (stiffness * (targetWidth - width) - damping * vw) * h;
                width += vw * h;
                final float speed = Math.min(1f, Math.abs(vx) / speedForFullSwell);
                final float swellTarget = pressed ? 1f : speed;
                vSwell += (SWELL_STIFFNESS * (swellTarget - swell) - SWELL_DAMPING * vSwell) * h;
                swell += vSwell * h;
                vStretch += (SWELL_STIFFNESS * (speed - stretch) - SWELL_DAMPING * vStretch) * h;
                stretch += vStretch * h;
            }
            final float rate = dt / 0.12f;
            if (visibility < targetVisibility) {
                visibility = Math.min(targetVisibility, visibility + rate);
            } else if (visibility > targetVisibility) {
                visibility = Math.max(targetVisibility, visibility - rate);
            }
            if (Math.abs(targetX - x) < 0.5f && Math.abs(vx) < 4f
                && Math.abs(targetWidth - width) < 0.5f && Math.abs(vw) < 4f
                && !pressed && Math.abs(swell) < 0.003f && Math.abs(vSwell) < 0.01f
                && Math.abs(stretch) < 0.003f && Math.abs(vStretch) < 0.01f
                && visibility == targetVisibility) {
                settled = true;
                x = targetX;
                width = targetWidth;
                vx = vw = swell = vSwell = stretch = vStretch = 0f;
            }
        }
        final float s = Math.max(0f, swell);
        final float st = Math.max(0f, stretch);
        final float w = width * (1f + swellWidth * s + stretchWidth * st);
        final float h = restHeight * (1f + swellHeight * s * (1f - FLATTEN * Math.min(1f, st)));
        rect.set(x - w / 2f, centerY - h / 2f, x + w / 2f, centerY + h / 2f);
        radius = Math.min(w, h) / 2f;
        path.rewind();
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        if (!settled) {
            host.invalidate();
        }
    }

    /** The resting fill, clearing away as the lens swells into glass. */
    public void drawBody(Canvas canvas, int color, boolean dark) {
        if (visibility <= 0f || rect.isEmpty()) {
            return;
        }
        final float s = Math.max(0f, Math.min(1f, swell));
        bodyPaint.setShader(null);
        bodyPaint.setColor(Theme.multAlpha(color, visibility * (1f - clearing * s)));
        canvas.drawRoundRect(rect, radius, radius, bodyPaint);
        if (s > 0.01f && clearing > 0f) {
            // clear glass reads a shade deeper than the pane it slides over
            bodyPaint.setColor(Theme.multAlpha(0xFF000000, (dark ? 0.16f : 0.04f) * s * visibility));
            canvas.drawRoundRect(rect, radius, radius, bodyPaint);
        }
    }

    /** Draws {@code content}, magnified where it passes under the swollen lens. */
    public void drawRefracted(Canvas canvas, Runnable content) {
        final float s = Math.max(0f, Math.min(1f, swell));
        if (s <= 0.01f || visibility <= 0f) {
            content.run();
            return;
        }
        canvas.save();
        LiquidGlassStyle.clipOut(canvas, path);
        content.run();
        canvas.restore();

        final float magnify = 1f + (refraction - 1f) * s;
        canvas.save();
        canvas.clipPath(path);
        canvas.scale(magnify, magnify, rect.centerX(), rect.centerY());
        content.run();
        canvas.restore();

        if (s > 0.25f) {
            drawBentRim(canvas, content, s);
        }
    }

    /** Folds what lies just past each end of the lens back inside its rim, squeezed and mirrored. */
    private void drawBentRim(Canvas canvas, Runnable content, float amount) {
        final float band = rect.width() * RIM_BAND;
        innerRect.set(rect.left + band, rect.top, rect.right - band, rect.bottom);
        final float innerRadius = Math.min(innerRect.width(), innerRect.height()) / 2f;
        innerPath.rewind();
        innerPath.addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW);
        final float stretchY = 1f + 0.25f * amount;
        for (int side = 0; side < 2; side++) {
            final boolean left = side == 0;
            canvas.save();
            canvas.clipPath(path);
            LiquidGlassStyle.clipOut(canvas, innerPath);
            canvas.clipRect(left ? rect.left : rect.centerX(), rect.top, left ? rect.centerX() : rect.right, rect.bottom);
            canvas.scale(-0.6f, stretchY, left ? rect.left : rect.right, rect.centerY());
            content.run();
            canvas.restore();
        }
    }

    /** The glass itself, on top of the content: rim, and bands and fringes while swollen. */
    public void drawGlass(Canvas canvas, boolean dark) {
        if (visibility <= 0f || rect.isEmpty()) {
            return;
        }
        final float amount = Math.max(0f, Math.min(1f, swell));
        // the bands belong to the tall bubble; stretched out, they spread too thin to show
        final float bands = amount * (1f - Math.max(0f, Math.min(1f, stretch)));
        drawGlass(canvas, rect, radius, amount, bands, visibility, dark);
    }

    private void drawGlass(Canvas canvas, RectF area, float r, float amount, float bands, float alpha, boolean dark) {
        final float stroke = AndroidUtilities.dpf2(1f);
        final float inset = stroke / 2f;
        final float restRim = dark ? 0.22f : 0.5f;

        rimPaint.setStrokeWidth(stroke);
        rimPaint.setShader(new LinearGradient(area.left, area.top, area.right, area.bottom,
            new int[] {
                Theme.multAlpha(0xFFFFFFFF, (restRim + (0.75f - restRim) * amount) * alpha),
                Theme.multAlpha(0xFFFFFFFF, 0.05f * alpha),
                Theme.multAlpha(0xFFFFFFFF, (restRim * 0.7f + 0.3f * amount) * alpha)
            },
            new float[] {0f, 0.55f, 1f},
            Shader.TileMode.CLAMP));
        area.inset(inset, inset);
        canvas.drawRoundRect(area, r - inset, r - inset, rimPaint);
        area.inset(-inset, -inset);

        if (amount <= 0.01f) {
            return;
        }

        // light bent along the top and bottom of the lens
        final float h = area.height();
        final float bandWidth = area.width() * 0.5f;
        final float bandHeight = Math.max(dp(2), h * 0.075f);
        final float cx = area.centerX();
        bandPaint.setShader(new LinearGradient(cx - bandWidth / 2f, 0, cx + bandWidth / 2f, 0,
            new int[] {0, Theme.multAlpha(0xFFFFFFFF, 0.6f * bands * alpha), 0},
            null, Shader.TileMode.CLAMP));
        final float gap = h * 0.055f;
        band.set(cx - bandWidth / 2f, area.top + gap, cx + bandWidth / 2f, area.top + gap + bandHeight);
        canvas.drawRoundRect(band, bandHeight / 2f, bandHeight / 2f, bandPaint);
        band.set(cx - bandWidth / 2f, area.bottom - gap - bandHeight, cx + bandWidth / 2f, area.bottom - gap);
        canvas.drawRoundRect(band, bandHeight / 2f, bandHeight / 2f, bandPaint);

        // chromatic fringes, strongest at the two ends of the lens
        final float fringe = AndroidUtilities.dpf2(1.5f);
        final float a = 0.75f * amount * alpha;
        fringePaint.setStrokeWidth(fringe);
        fringePaint.setShader(new LinearGradient(area.left, 0, area.right, 0,
            new int[] {Theme.multAlpha(0xFF3FD8FF, a), 0, 0, Theme.multAlpha(0xFFFF5EC4, a)},
            new float[] {0f, 0.2f, 0.8f, 1f},
            Shader.TileMode.CLAMP));
        final float i1 = fringe;
        area.inset(i1, i1);
        canvas.drawRoundRect(area, Math.max(0f, r - i1), Math.max(0f, r - i1), fringePaint);
        area.inset(-i1, -i1);
        fringePaint.setShader(new LinearGradient(area.left, 0, area.right, 0,
            new int[] {Theme.multAlpha(0xFFFFB44A, a), 0, 0, Theme.multAlpha(0xFF6F8CFF, a)},
            new float[] {0f, 0.14f, 0.86f, 1f},
            Shader.TileMode.CLAMP));
        final float i2 = fringe * 2f;
        area.inset(i2, i2);
        canvas.drawRoundRect(area, Math.max(0f, r - i2), Math.max(0f, r - i2), fringePaint);
        area.inset(-i2, -i2);
    }
}
