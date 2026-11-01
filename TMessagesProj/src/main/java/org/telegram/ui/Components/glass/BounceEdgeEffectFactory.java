package org.telegram.ui.Components.glass;

import android.graphics.Canvas;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;

/**
 * iOS-style rubber-band overscroll for vertical lists: pulling past an end drags the whole list
 * with resistance, and letting go (or a fling hitting the end) springs it back. Replaces the
 * platform glow / stretch.
 */
public class BounceEdgeEffectFactory extends RecyclerView.EdgeEffectFactory {

    /** Share of the pull that moves the list; less than 1 gives the rubber-band resistance. */
    private static final float PULL_FRACTION = 0.22f;
    /** Share of a fling's velocity carried into the bounce when it hits the end. */
    private static final float FLING_FRACTION = 0.35f;
    private static final float MAX_OFFSET_FRACTION = 0.35f;

    @NonNull
    @Override
    protected EdgeEffect createEdgeEffect(@NonNull RecyclerView view, int direction) {
        if (direction != DIRECTION_TOP && direction != DIRECTION_BOTTOM) {
            return super.createEdgeEffect(view, direction);
        }
        final float sign = direction == DIRECTION_BOTTOM ? -1f : 1f;
        return new EdgeEffect(view.getContext()) {
            private SpringAnimation spring;

            private void pull(float deltaDistance) {
                if (spring != null) {
                    spring.cancel();
                }
                final float max = view.getHeight() * MAX_OFFSET_FRACTION;
                final float current = view.getTranslationY();
                // more resistance the further it is already pulled
                final float resistance = 1f - Math.min(1f, Math.abs(current) / Math.max(1f, max));
                final float delta = sign * view.getHeight() * deltaDistance * PULL_FRACTION * resistance;
                view.setTranslationY(Math.max(-max, Math.min(max, current + delta)));
            }

            private void springBack(float velocity) {
                if (view.getTranslationY() == 0f && velocity == 0f) {
                    return;
                }
                if (spring != null) {
                    spring.cancel();
                }
                spring = new SpringAnimation(view, SpringAnimation.TRANSLATION_Y)
                    .setSpring(new SpringForce(0f).setDampingRatio(0.8f).setStiffness(320f));
                spring.setStartVelocity(velocity);
                spring.start();
            }

            @Override
            public void onPull(float deltaDistance) {
                pull(deltaDistance);
            }

            @Override
            public void onPull(float deltaDistance, float displacement) {
                pull(deltaDistance);
            }

            @Override
            public float onPullDistance(float deltaDistance, float displacement) {
                pull(deltaDistance);
                return deltaDistance;
            }

            @Override
            public float getDistance() {
                return 0f; // nothing for the platform stretch to release
            }

            @Override
            public void onRelease() {
                springBack(0f);
            }

            @Override
            public void onAbsorb(int velocity) {
                springBack(sign * velocity * FLING_FRACTION);
            }

            @Override
            public boolean isFinished() {
                return spring == null || !spring.isRunning();
            }

            @Override
            public boolean draw(Canvas canvas) {
                return false; // no glow, no stretch: the list itself moves
            }
        };
    }

    /** Uses the bounce on a vertical list. */
    public static void apply(RecyclerView listView) {
        listView.setEdgeEffectFactory(new BounceEdgeEffectFactory());
        listView.setOverScrollMode(RecyclerView.OVER_SCROLL_ALWAYS);
    }
}
