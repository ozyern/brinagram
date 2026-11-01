package org.telegram.ui.Components.glass;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.window.SplashScreenView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.telegram.messenger.FileLog;
import org.telegram.ui.Components.CubicBezierInterpolator;

/**
 * BrinaGram opening animation (Android 12+).
 *
 * <p>Timeline once the app is ready to draw:
 * <ol>
 *     <li>0–520ms: a specular glint sweeps diagonally across the medallion.</li>
 *     <li>260–760ms: the medallion grows slightly and dissolves, the splash fades out.</li>
 *     <li>300–820ms: the app content settles in from 94% scale, like an iOS app launch.</li>
 * </ol>
 * Older Android versions show the static {@code brina_launch_background} instead.
 */
public final class BrinaSplash {

    private static final long GLINT_DURATION = 520;
    private static final long EXIT_DELAY = 260;
    private static final long EXIT_DURATION = 500;
    private static final long CONTENT_DELAY = 300;
    private static final long CONTENT_DURATION = 520;
    private static final float CONTENT_START_SCALE = 0.94f;

    private BrinaSplash() {}

    public static void install(@NonNull Activity activity, @NonNull View content) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        try {
            installImpl(activity, content);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static void installImpl(Activity activity, View content) {
        activity.getSplashScreen().setOnExitAnimationListener(splashView -> {
            final View icon = splashView.getIconView();
            final GlintDrawable glint = new GlintDrawable(1f / 3f); // the system masks the icon to its central 2/3 circle
            if (icon != null) {
                icon.setForeground(glint);
            }

            content.setAlpha(0f);
            content.setScaleX(CONTENT_START_SCALE);
            content.setScaleY(CONTENT_START_SCALE);

            final long total = Math.max(EXIT_DELAY + EXIT_DURATION, CONTENT_DELAY + CONTENT_DURATION);
            final ValueAnimator animator = ValueAnimator.ofFloat(0f, total);
            animator.setDuration(total);
            animator.setInterpolator(null); // linear clock; each phase applies its own easing
            animator.addUpdateListener(a -> {
                final float t = (float) a.getAnimatedValue();

                glint.setProgress(CubicBezierInterpolator.EASE_BOTH.getInterpolation(clamp(t / GLINT_DURATION)));

                final float exit = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(clamp((t - EXIT_DELAY) / EXIT_DURATION));
                if (icon != null) {
                    final float s = 1f + 0.12f * exit;
                    icon.setScaleX(s);
                    icon.setScaleY(s);
                    icon.setAlpha(1f - exit);
                }
                splashView.setAlpha(1f - CubicBezierInterpolator.EASE_IN.getInterpolation(clamp((t - EXIT_DELAY) / EXIT_DURATION)));

                final float in = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(clamp((t - CONTENT_DELAY) / CONTENT_DURATION));
                final float cs = CONTENT_START_SCALE + (1f - CONTENT_START_SCALE) * in;
                content.setScaleX(cs);
                content.setScaleY(cs);
                content.setAlpha(in);
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finish(splashView, content);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    finish(splashView, content);
                }
            });
            animator.start();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static void finish(SplashScreenView splashView, View content) {
        content.setAlpha(1f);
        content.setScaleX(1f);
        content.setScaleY(1f);
        splashView.remove();
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
