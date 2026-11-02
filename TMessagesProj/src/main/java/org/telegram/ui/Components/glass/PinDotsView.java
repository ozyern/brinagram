package org.telegram.ui.Components.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;

/** A centered row of {@link PinDots}, spaced in proportion to the screen. */
public final class PinDotsView extends View {

    private final PinDots dots = new PinDots(this);
    private final int screenWidth;
    private int color = 0xFFFFFFFF;

    public PinDotsView(Context context, int count, int screenWidth) {
        super(context);
        this.screenWidth = screenWidth;
        dots.setCount(count);
    }

    public PinDots getDots() {
        return dots;
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final float step = screenWidth * PinLayout.DOT_STEP;
        final float radius = screenWidth * PinLayout.DOT_RADIUS;
        final int count = dots.getCount();
        final float left = (getWidth() - step * (count - 1)) / 2f;
        final float y = getHeight() / 2f;
        for (int i = 0; i < count; i++) {
            dots.draw(canvas, i, left + step * i, y, radius, color);
        }
    }
}
