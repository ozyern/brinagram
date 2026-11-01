package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BrinaBrand;

/** "Brinagram" set in bold white text, used where Telegram drew its wordmark image. Tint with a color filter. */
public class BrinaWordmarkDrawable extends Drawable {

    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final String text = BrinaBrand.APP_NAME;
    private final Rect textBounds = new Rect();

    public BrinaWordmarkDrawable(float textSizeDp) {
        paint.setColor(0xFFFFFFFF);
        paint.setTypeface(AndroidUtilities.bold());
        paint.setTextSize(dp(textSizeDp));
        paint.getTextBounds(text, 0, text.length(), textBounds);
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) Math.ceil(paint.measureText(text));
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) Math.ceil(paint.descent() - paint.ascent());
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect b = getBounds();
        final float scale = Math.min(1f, Math.min(b.width() / (float) Math.max(1, getIntrinsicWidth()), b.height() / (float) Math.max(1, getIntrinsicHeight())));
        canvas.save();
        canvas.translate(b.left, b.exactCenterY());
        canvas.scale(scale, scale);
        canvas.drawText(text, 0, -(paint.descent() + paint.ascent()) / 2f, paint);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
