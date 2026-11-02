package org.telegram.ui.Components.glass;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The profile page's backdrop: one gradient in screen space, slightly diagonal so the light falls
 * from the upper left. Everything painted from it lines up however far down the page it starts,
 * so the header, the list and the cards never show a seam.
 */
public final class ProfileBackdrop {

    private ProfileBackdrop() {}

    /**
     * @param topInScreen where the canvas that will draw this starts, measured from the screen's top
     */
    @NonNull
    public static LinearGradient shader(int topColor, int bottomColor, float screenWidth, float screenHeight, float topInScreen) {
        final LinearGradient gradient = new LinearGradient(0, 0, screenWidth * 0.3f, screenHeight, topColor, bottomColor, Shader.TileMode.CLAMP);
        final Matrix matrix = new Matrix();
        matrix.setTranslate(0, -topInScreen);
        gradient.setLocalMatrix(matrix);
        return gradient;
    }

    /** Fills its bounds with a backdrop shader. */
    public static final class Fill extends Drawable {

        private final Paint paint = new Paint();

        public Fill(@NonNull Shader shader) {
            paint.setShader(shader);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawRect(getBounds(), paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }
}
