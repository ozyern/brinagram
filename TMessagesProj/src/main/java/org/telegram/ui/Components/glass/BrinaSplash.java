package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.R;
import org.telegram.ui.Components.CubicBezierInterpolator;

/**
 * BrinaGram opening animation, drawn by the app so it is identical on every Android version.
 *
 * <p>The system splash (Android 12+) and the launch window (older versions) are just the plain
 * splash color; this overlay takes over on the first frame:
 * <ol>
 *     <li>0–560ms: the medallion springs in from 60% with a slight twist while a gold glow blooms.</li>
 *     <li>200–900ms: four sparkles twinkle around it, echoing the stars on the icon.</li>
 *     <li>520–980ms: a specular glint sweeps across the medallion.</li>
 *     <li>960–1320ms: the app is revealed through a circle opening out from the medallion, which
 *     zooms toward the viewer and dissolves; the app settles in from 94%.</li>
 * </ol>
 */
public final class BrinaSplash {

    private static final long DURATION = 1320;
    private static final long DURATION_LITE = 520;

    private BrinaSplash() {}

    public static void install(@NonNull Activity activity, @NonNull ViewGroup content) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // The system splash is only the splash color: hand over to our overlay at once.
                activity.getSplashScreen().setOnExitAnimationListener(splashView -> splashView.remove());
            }
            // on the decor view, so it stays above everything the activity adds later
            final ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            final SplashView view = new SplashView(activity);
            decor.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            view.start(content);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @SuppressLint("ViewConstructor")
    private static final class SplashView extends View {

        private static final float[][] STARS = {
            // x, y (relative to the medallion radius), size (dp), delay (0..1 of the sparkle phase)
            {-1.25f, -0.95f, 12f, 0.00f},
            {1.30f, -0.55f, 9f, 0.18f},
            {-1.05f, 1.15f, 8f, 0.34f},
            {1.10f, 1.05f, 11f, 0.50f},
        };

        private final Drawable medallion;
        private final GlintDrawable glint = new GlintDrawable(0.5f);
        private final Paint background = new Paint();
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path revealPath = new Path();
        private final Path starPath = new Path();
        private final OvershootInterpolator overshoot = new OvershootInterpolator(1.6f);
        private final boolean lite;
        private float t; // ms on the full-length timeline
        private View content;

        SplashView(Context context) {
            super(context);
            medallion = ContextCompat.getDrawable(context, R.drawable.brina_medallion);
            background.setColor(ContextCompat.getColor(context, R.color.brina_splash_background));
            starPaint.setColor(0xFFFFE9C2);
            lite = !LiteMode.isEnabled(LiteMode.FLAG_CHAT_BACKGROUND);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return true; // swallow touches until the app is revealed
        }

        void start(View content) {
            this.content = content;
            final long duration = lite ? DURATION_LITE : DURATION;
            final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(duration);
            animator.setInterpolator(null);
            animator.addUpdateListener(a -> {
                t = (float) a.getAnimatedValue() * DURATION;
                final float in = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(phase(960, 1320));
                setContentScale(0.94f + 0.06f * in);
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setContentScale(1f);
                    if (getParent() instanceof ViewGroup) {
                        ((ViewGroup) getParent()).removeView(SplashView.this);
                    }
                }
            });
            animator.start();
        }

        /** Scales the app as it is revealed. */
        private void setContentScale(float s) {
            if (content != null) {
                content.setScaleX(s);
                content.setScaleY(s);
            }
        }

        private float phase(float from, float to) {
            return Math.max(0f, Math.min(1f, (t - from) / (to - from)));
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            final float w = getWidth(), h = getHeight();
            final float cx = w / 2f, cy = h / 2f;
            final float radius = dp(66);

            // background, with a growing circular hole revealing the app
            final float reveal = CubicBezierInterpolator.EASE_IN.getInterpolation(phase(960, 1320));
            if (reveal > 0f) {
                final float max = (float) Math.hypot(cx, cy) + dp(8);
                revealPath.rewind();
                revealPath.addCircle(cx, cy, radius * 0.6f + (max - radius * 0.6f) * reveal, Path.Direction.CW);
                canvas.save();
                canvas.clipOutPath(revealPath);
                canvas.drawRect(0, 0, w, h, background);
                canvas.restore();
            } else {
                canvas.drawRect(0, 0, w, h, background);
            }

            // medallion: springs in, then zooms toward the viewer and dissolves
            final float enter = overshoot.getInterpolation(phase(0, 560));
            final float enterAlpha = CubicBezierInterpolator.EASE_OUT.getInterpolation(phase(0, 300));
            final float exit = CubicBezierInterpolator.EASE_IN.getInterpolation(phase(960, 1260));
            final float scale = (0.6f + 0.4f * enter) * (1f + 1.4f * exit);
            final float alpha = enterAlpha * (1f - exit);
            final float rotation = -8f * (1f - Math.min(1f, enter));

            // gold bloom behind it
            final float bloom = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(phase(80, 760)) * (1f - exit);
            if (bloom > 0.001f) {
                final float gr = radius * (1.4f + 0.9f * bloom);
                glowPaint.setShader(new RadialGradient(cx, cy, gr,
                    new int[] {((int) (0x59 * bloom) << 24) | 0xE8C690, ((int) (0x1F * bloom) << 24) | 0xE8C690, 0x00E8C690},
                    new float[] {0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawCircle(cx, cy, gr, glowPaint);
            }

            if (medallion != null && alpha > 0f) {
                canvas.save();
                canvas.translate(cx, cy);
                canvas.rotate(rotation);
                canvas.scale(scale, scale);
                final int r = (int) radius;
                medallion.setBounds(-r, -r, r, r);
                medallion.setAlpha((int) (255 * alpha));
                medallion.draw(canvas);
                glint.setBounds(-r, -r, r, r);
                glint.setAlpha((int) (255 * alpha));
                glint.setProgress(CubicBezierInterpolator.EASE_BOTH.getInterpolation(phase(520, 980)));
                glint.draw(canvas);
                canvas.restore();
            }

            // sparkles
            if (!lite) {
                for (float[] s : STARS) {
                    final float start = 200 + s[3] * 420;
                    final float p = phase(start, start + 380);
                    if (p <= 0f || p >= 1f) {
                        continue;
                    }
                    final float pop = (float) Math.sin(Math.PI * p); // grows, then fades
                    drawStar(canvas, cx + s[0] * radius, cy + s[1] * radius, dp(s[2]) * pop, 45f * p, (int) (255 * pop * (1f - exit)));
                }
            }
        }

        /** A four-pointed sparkle (concave diamond). */
        private void drawStar(Canvas canvas, float x, float y, float size, float rotation, int alpha) {
            if (size <= 0f || alpha <= 0) {
                return;
            }
            final float in = size * 0.18f;
            starPath.rewind();
            starPath.moveTo(0, -size);
            starPath.quadTo(in, -in, size, 0);
            starPath.quadTo(in, in, 0, size);
            starPath.quadTo(-in, in, -size, 0);
            starPath.quadTo(-in, -in, 0, -size);
            starPath.close();
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(rotation);
            starPaint.setAlpha(alpha);
            canvas.drawPath(starPath, starPaint);
            canvas.restore();
        }
    }
}
