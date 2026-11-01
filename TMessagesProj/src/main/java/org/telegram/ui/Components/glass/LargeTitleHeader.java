package org.telegram.ui.Components.glass;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Large collapsing title for list screens.
 *
 * <p>A 34sp bold title sits above the list and scrolls away with it; as it goes, it shrinks a
 * little and fades, and the action bar's own small title fades in. Scrolling back to the top
 * brings the large title back. Works with any vertical list in a FrameLayout.
 */
@SuppressLint("ViewConstructor")
public class LargeTitleHeader extends FrameLayout {

    public static final int HEIGHT_DP = 52;

    private final ActionBar actionBar;
    private final RecyclerView listView;
    private final TextView titleView;

    private LargeTitleHeader(Context context, ActionBar actionBar, RecyclerView listView, CharSequence title, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.actionBar = actionBar;
        this.listView = listView;

        titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 34);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        titleView.setSingleLine();
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setPivotX(0);
        titleView.setPivotY(dp(HEIGHT_DP));
        addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 20, 0, 20, 6));
        setClickable(false);
    }

    /**
     * Adds a large title above {@code listView} inside {@code container} (the list's parent, a
     * FrameLayout). The list gets top padding for it and stops clipping to that padding.
     */
    public static LargeTitleHeader attach(@NonNull ActionBar actionBar, @NonNull RecyclerView listView, @NonNull ViewGroup container,
                                          CharSequence title, Theme.ResourcesProvider resourcesProvider) {
        final LargeTitleHeader header = new LargeTitleHeader(container.getContext(), actionBar, listView, title, resourcesProvider);
        final ViewGroup.LayoutParams listParams = listView.getLayoutParams();
        int topMargin = 0;
        if (listParams instanceof ViewGroup.MarginLayoutParams) {
            topMargin = ((ViewGroup.MarginLayoutParams) listParams).topMargin;
        }
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(HEIGHT_DP), Gravity.TOP);
        lp.topMargin = topMargin;
        container.addView(header, container.indexOfChild(listView) + 1, lp);

        listView.setPadding(listView.getPaddingLeft(), listView.getPaddingTop() + dp(HEIGHT_DP), listView.getPaddingRight(), listView.getPaddingBottom());
        listView.setClipToPadding(false);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                header.update();
            }
        });
        listView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> header.update());
        header.update();
        return header;
    }

    /**
     * Same as {@link #attach}, once the list is inside a FrameLayout; the title is taken from the
     * action bar at that moment. For screens that build their list before adding it to a parent.
     */
    public static void attachWhenReady(@NonNull ActionBar actionBar, @NonNull RecyclerView listView, Theme.ResourcesProvider resourcesProvider) {
        final Runnable tryAttach = () -> {
            if (listView.getTag(org.telegram.messenger.R.id.brina_large_title) != null) {
                return;
            }
            if (listView.getParent() instanceof FrameLayout && actionBar.getTitle() != null) {
                listView.setTag(org.telegram.messenger.R.id.brina_large_title, Boolean.TRUE);
                attach(actionBar, listView, (ViewGroup) listView.getParent(), actionBar.getTitle(), resourcesProvider);
            }
        };
        if (listView.isAttachedToWindow()) {
            tryAttach.run();
        } else {
            listView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                    listView.removeOnAttachStateChangeListener(this);
                    listView.post(tryAttach);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                }
            });
        }
    }

    /** How far the list has scrolled past its top, in px (capped once the title is gone). */
    private int scrolled() {
        final RecyclerView.LayoutManager lm = listView.getLayoutManager();
        if (!(lm instanceof LinearLayoutManager) || listView.getChildCount() == 0) {
            return 0;
        }
        final View first = lm.findViewByPosition(0);
        if (first == null) {
            return dp(HEIGHT_DP) * 4; // the first row is off screen: the title is long gone
        }
        return listView.getPaddingTop() - first.getTop();
    }

    public void update() {
        final float height = dp(HEIGHT_DP);
        final int offset = scrolled();
        final float collapse = Math.max(0f, Math.min(1f, offset / height));
        // the large title travels with the content, never further down than its rest position
        setTranslationY(-Math.max(0, offset));
        final float scale = offset < 0 ? 1f + Math.min(0.08f, -offset / height * 0.08f) : 1f - 0.06f * collapse;
        titleView.setScaleX(scale);
        titleView.setScaleY(scale);
        titleView.setAlpha(1f - collapse);
        // the bar's small title takes over in the second half of the collapse
        final View small = actionBar.getTitleTextView();
        if (small != null) {
            small.setAlpha(Math.max(0f, Math.min(1f, (collapse - 0.5f) * 2f)));
        }
    }
}
