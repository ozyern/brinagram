package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

/**
 * iOS "inset grouped" list style (like the iOS Settings app).
 *
 * <p>Rows that paint an opaque, non-gray background become rounded cards, inset from the
 * screen edges. Runs of consecutive card rows form one card; only the outer corners are rounded.
 * Section separators ({@link ShadowSectionCell}, {@link TextInfoPrivacyCell}) lose their
 * drop-shadow strips and become plain gaps on the grouped background.
 *
 * <p>Only applied to vertical, non-grid lists. Enable with
 * {@code RecyclerListView#setInsetGrouped(true)}.
 */
public class InsetGroupedDecoration extends RecyclerView.ItemDecoration {

    public static final int CARD_INSET_DP = 16;
    public static final int CARD_RADIUS_DP = 16;

    private final Theme.ResourcesProvider resourcesProvider;
    private final Path clipPath = new Path();
    private final RectF rect = new RectF();
    private final float[] radii = new float[8];

    public InsetGroupedDecoration(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
    }

    public static boolean isSupported(RecyclerView parent) {
        final RecyclerView.LayoutManager lm = parent.getLayoutManager();
        return lm instanceof LinearLayoutManager
            && !(lm instanceof GridLayoutManager)
            && ((LinearLayoutManager) lm).getOrientation() == RecyclerView.VERTICAL;
    }

    private int groupedBackground() {
        return Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider);
    }

    /** True if the row paints an opaque strip that should become (part of) a card. */
    public boolean isCard(View view) {
        if (view == null || view instanceof ShadowSectionCell || view instanceof TextInfoPrivacyCell) {
            return false;
        }
        final int color = opaqueColorOf(view.getBackground());
        return color != 0 && color != groupedBackground();
    }

    private static int opaqueColorOf(Drawable d) {
        if (d instanceof ColorDrawable) {
            final int c = ((ColorDrawable) d).getColor();
            return Color.alpha(c) == 255 ? c : 0;
        }
        if (d instanceof LayerDrawable) { // includes RippleDrawable with a solid content layer
            final LayerDrawable layers = (LayerDrawable) d;
            for (int i = 0; i < layers.getNumberOfLayers(); i++) {
                final int c = opaqueColorOf(layers.getDrawable(i));
                if (c != 0) {
                    return c;
                }
            }
        }
        return 0;
    }

    /** Turns shadowed separator strips into plain gaps. Call when a child is attached. */
    public void onChildAttached(View view) {
        if (view instanceof ShadowSectionCell || view instanceof TextInfoPrivacyCell) {
            view.setBackground(null);
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (isSupported(parent) && isCard(view)) {
            outRect.left = outRect.right = dp(CARD_INSET_DP);
        } else {
            outRect.setEmpty();
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (isSupported(parent)) {
            c.drawColor(groupedBackground());
        }
    }

    /**
     * Clips a card row so the first/last row of each card gets rounded outer corners.
     * @return true if the canvas was clipped (caller must restore).
     */
    public boolean clipChild(Canvas canvas, RecyclerView parent, View child) {
        if (!isSupported(parent) || !isCard(child)) {
            return false;
        }
        final int position = parent.getChildAdapterPosition(child);
        if (position == RecyclerView.NO_POSITION) {
            return false;
        }
        final RecyclerView.LayoutManager lm = parent.getLayoutManager();
        // A neighbor that is not laid out is off screen, so that card edge is not visible anyway.
        final View prev = lm.findViewByPosition(position - 1);
        final View next = lm.findViewByPosition(position + 1);
        final boolean roundTop = position == 0 || (prev != null && !isCard(prev));
        final boolean roundBottom = position == lm.getItemCount() - 1 || (next != null && !isCard(next));
        if (!roundTop && !roundBottom) {
            return false;
        }
        final float r = dp(CARD_RADIUS_DP);
        radii[0] = radii[1] = radii[2] = radii[3] = roundTop ? r : 0;
        radii[4] = radii[5] = radii[6] = radii[7] = roundBottom ? r : 0;
        rect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        rect.offset(child.getTranslationX(), child.getTranslationY());
        clipPath.rewind();
        clipPath.addRoundRect(rect, radii, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);
        return true;
    }
}
