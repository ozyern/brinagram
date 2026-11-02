package org.telegram.ui.Components.glass;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

/**
 * Plays once, the first time the app is ever opened: drops of white light on black.
 *
 * <ol>
 *     <li>A drop splits in two, the pair circles in depth and merges back into one.</li>
 *     <li>It parts stickily into four that slowly turn.</li>
 *     <li>They draw together into a glowing cluster, warm on one side and cool on the other.</li>
 *     <li>The cluster floods the screen with white, which fades to reveal the app.</li>
 * </ol>
 *
 * <p>The drops are metaballs evaluated on a small bitmap, so they run the same on every Android
 * version; the scaled-up bitmap also gives them their soft edge.
 */
@SuppressLint("ViewConstructor")
public final class BrinaSetupAnimation extends View {

    private static final String PREFS = "brina_first_run";
    private static final String KEY_PLAYED = "setup_animation_played";

    /** Seconds: the timeline ends when the white has faded away. */
    static final float DURATION = 6.25f;
    private static final float FADE_START = 5.8f;

    private static final int FIELD_SIZE = 240;
    /** The field covers this fraction of the view's width. */
    private static final float FIELD_SPAN = 0.92f;
    private static final float BASE_RADIUS = 0.074f;
    private static final int BALLS = 4;

    private static long finishesAtMillis;

    private final Bitmap bitmap = Bitmap.createBitmap(FIELD_SIZE, FIELD_SIZE, Bitmap.Config.ARGB_8888);
    private final int[] pixels = new int[FIELD_SIZE * FIELD_SIZE];
    private final float[] balls = new float[BALLS * 3];
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floodPaint = new Paint();
    private final RectF dest = new RectF();
    private float time;

    public BrinaSetupAnimation(Context context) {
        super(context);
        glowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        floodPaint.setColor(0xFFFFFFFF);
    }

    /** Whether the animation has yet to be played on this install. */
    public static boolean shouldPlay(@NonNull Context context) {
        return !prefs(context).getBoolean(KEY_PLAYED, false);
    }

    /** How much longer the animation covers the app, for screens that reveal themselves after it. */
    public static long remainingMillis() {
        return Math.max(0L, finishesAtMillis - SystemClock.elapsedRealtime());
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Adds the animation on top of {@code decor} and marks it as played. */
    public static void play(@NonNull ViewGroup decor) {
        final Context context = decor.getContext();
        prefs(context).edit().putBoolean(KEY_PLAYED, true).apply();
        final BrinaSetupAnimation view = new BrinaSetupAnimation(context);
        decor.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.start();
    }

    private void start() {
        finishesAtMillis = SystemClock.elapsedRealtime() + (long) (DURATION * 1000);
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, DURATION);
        animator.setDuration((long) (DURATION * 1000));
        animator.setInterpolator(null);
        animator.addUpdateListener(a -> {
            time = (float) a.getAnimatedValue();
            setAlpha(1f - smooth(FADE_START, DURATION, time));
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (getParent() instanceof ViewGroup) {
                    ((ViewGroup) getParent()).removeView(BrinaSetupAnimation.this);
                }
                bitmap.recycle();
            }
        });
        animator.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true; // nothing underneath is reachable until the app is revealed
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (bitmap.isRecycled()) {
            return;
        }
        final float w = getWidth(), h = getHeight();
        final float cx = w / 2f, cy = h / 2f;
        canvas.drawColor(0xFF000000);

        final float zoom = zoom(time);
        final float bloom = bloom(time);
        if (bloom > 0.001f) {
            drawGlow(canvas, cx, cy, w * 0.60f * zoom, 0xFFFFFFFF, 0.30f * bloom);
            final float offset = w * 0.05f * zoom;
            drawGlow(canvas, cx - offset, cy - offset, w * 0.36f * zoom, 0xFFFFB27A, 0.85f * bloom);
            drawGlow(canvas, cx + offset, cy + offset, w * 0.36f * zoom, 0xFF4FA3FF, 0.85f * bloom);
        }

        computeBalls(time, balls);
        renderField();
        final float half = w * FIELD_SPAN / 2f * zoom;
        dest.set(cx - half, cy - half, cx + half, cy + half);
        canvas.drawBitmap(bitmap, null, dest, bitmapPaint);

        final float flood = smooth(5.55f, 5.72f, time);
        if (flood > 0f) {
            floodPaint.setAlpha((int) (255 * flood));
            canvas.drawRect(0, 0, w, h, floodPaint);
        }
    }

    private void drawGlow(Canvas canvas, float x, float y, float radius, int color, float alpha) {
        final int rgb = color & 0xFFFFFF;
        final int a = (int) (255 * Math.min(1f, alpha)) << 24;
        glowPaint.setShader(new RadialGradient(x, y, radius, new int[] {a | rgb, (a >>> 25 << 24) | rgb, rgb}, new float[] {0.2f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius, glowPaint);
    }

    /** Evaluates the metaball field into the bitmap: white, with alpha easing across its threshold. */
    private void renderField() {
        final float cell = FIELD_SPAN / FIELD_SIZE;
        final float origin = -FIELD_SPAN / 2f + cell / 2f;
        int index = 0;
        for (int py = 0; py < FIELD_SIZE; py++) {
            final float y = origin + py * cell;
            for (int px = 0; px < FIELD_SIZE; px++) {
                final float x = origin + px * cell;
                float field = 0f;
                for (int b = 0; b < BALLS * 3; b += 3) {
                    final float r = balls[b + 2];
                    if (r > 0f) {
                        final float dx = x - balls[b], dy = y - balls[b + 1];
                        field += r * r / Math.max(1e-6f, dx * dx + dy * dy);
                    }
                }
                pixels[index++] = fieldToColor(field);
            }
        }
        bitmap.setPixels(pixels, 0, FIELD_SIZE, 0, 0, FIELD_SIZE, FIELD_SIZE);
    }

    private static int fieldToColor(float field) {
        if (field <= 0.85f) {
            return 0x00FFFFFF;
        }
        if (field >= 1.15f) {
            return 0xFFFFFFFF;
        }
        final float f = (field - 0.85f) / 0.3f;
        return ((int) (255 * f * f * (3f - 2f * f)) << 24) | 0xFFFFFF;
    }

    /**
     * Where the drops are at {@code t} seconds, as (x, y, radius) triples in fractions of the view's
     * width, relative to its center.
     */
    static void computeBalls(float t, float[] out) {
        final float appear = smooth(0f, 0.12f, t);
        final float merged = smooth(2.98f, 3.18f, t);
        if (t < 2.98f) {
            // the pair: A is half the distance between the two, circling in depth by theta
            final float maxA = 0.185f;
            final float a = maxA * backOut(smooth(0.12f, 0.6f, t)) * (1f - smooth(0.8f, 2.5f, t));
            final float closeness = 1f - Math.min(1f, a / maxA);
            final double theta = Math.PI * 2.2 * smooth(0.85f, 2.5f, t);
            final float depth = (float) Math.sin(theta) * (1f - closeness);
            // a pair sitting on one spot must read as a single drop of the same size
            final float base = BASE_RADIUS * lerp(1f, 0.7071f, closeness) * appear;
            final float swell = 1f + 0.3f * bump(0.1f, 0.4f, t);
            final float x = a * (float) Math.cos(theta);
            final float y = a * 0.25f * (float) Math.sin(theta);
            put(out, 0, -x, -y, base * swell * (1f + 0.18f * depth));
            put(out, 1, x, y, base * swell * (1f - 0.18f * depth));
            put(out, 2, 0f, 0f, 0f);
            put(out, 3, 0f, 0f, 0f);
            return;
        }
        // four: each radius so that, on one spot, they read as the single drop they came from
        float spread = 0.235f * backOut(smooth(2.98f, 3.38f, t));
        spread = lerp(spread, 0.108f, smooth(4.3f, 4.55f, t));
        spread *= 1f - 0.1f * smooth(4.55f, 5.0f, t);
        final float radius = BASE_RADIUS * lerp(0.5f, 0.95f, merged) * lerp(1f, 0.9f, smooth(4.3f, 4.55f, t));
        final double angle = Math.toRadians(-45.0 + 60.0 * Math.min(t - 2.98f, 1.5f));
        for (int i = 0; i < 4; i++) {
            final double a = angle + i * Math.PI / 2.0;
            put(out, i, spread * (float) Math.cos(a), spread * (float) Math.sin(a), radius);
        }
    }

    /** How far the whole scene is enlarged: a slight gathering, then a rush past the screen edges. */
    static float zoom(float t) {
        final float gather = 1f + 0.55f * smooth(5.2f, 5.45f, t);
        final float rush = smooth(5.45f, 5.72f, t);
        return gather * (1f + 12f * rush * rush);
    }

    static float bloom(float t) {
        return smooth(4.3f, 4.55f, t) * (0.85f + 0.15f * (float) Math.sin(t * 6f));
    }

    private static void put(float[] out, int i, float x, float y, float r) {
        out[i * 3] = x;
        out[i * 3 + 1] = y;
        out[i * 3 + 2] = r;
    }

    private static float lerp(float a, float b, float f) {
        return a + (b - a) * f;
    }

    /** 0 before {@code from}, 1 after {@code to}, easing in and out between them. */
    static float smooth(float from, float to, float t) {
        final float f = Math.max(0f, Math.min(1f, (t - from) / (to - from)));
        return f * f * (3f - 2f * f);
    }

    /** Eases past 1 and back, for drops that spring apart. */
    private static float backOut(float f) {
        final float g = f - 1f;
        return 1f + g * g * (2.2f * g + 1.2f);
    }

    /** A rise and fall between {@code from} and {@code to}. */
    private static float bump(float from, float to, float t) {
        final float f = Math.max(0f, Math.min(1f, (t - from) / (to - from)));
        return (float) Math.sin(Math.PI * f);
    }
}
