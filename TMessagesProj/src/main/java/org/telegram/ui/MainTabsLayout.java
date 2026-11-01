package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;


import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedLinearLayout;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.glass.GlassTabView;

import java.util.HashSet;
import java.util.Set;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.ListAnimator;

@SuppressLint("ViewConstructor")
public class MainTabsLayout extends AnimatedLinearLayout {

    private final Theme.ResourcesProvider resourcesProvider;

    public MainTabsLayout(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
    }

    private static final float[] PASS_TEXT_SIZES_DP = {12f, 12f, 10f};
    private static final int[] PASS_PADDINGS_DP = {16, 8, 4};

    private int maxWidthPx;

    public void setMaxWidth(int maxWidthPx) {
        if (this.maxWidthPx != maxWidthPx) {
            this.maxWidthPx = maxWidthPx;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        final int tabHeight = height - getPaddingTop() - getPaddingBottom();

        if (maxWidthPx > 0 && width > maxWidthPx) {
            width = maxWidthPx;
        }

        final int maxTotalWidthForTabs = width - getPaddingLeft() - getPaddingRight();
        final int minTotalWidthForTabs = Math.min(dp(320), maxTotalWidthForTabs);

        int chosenPass = PASS_TEXT_SIZES_DP.length - 1;
        float lastMeasuredTextSize = -1;
        for (int pass = 0; pass < PASS_TEXT_SIZES_DP.length; pass++) {
            if (PASS_TEXT_SIZES_DP[pass] != lastMeasuredTextSize) {
                measureTabTexts(PASS_TEXT_SIZES_DP[pass]);
                lastMeasuredTextSize = PASS_TEXT_SIZES_DP[pass];
            }
            final int padding = dp(PASS_PADDINGS_DP[pass]);
            float total = 0;
            for (int a = 0, N = getChildCount(); a < N; a++) {
                if (!isViewVisible(getChildAt(a))) continue;
                final float withMargin = tabsTextWidth[a] + padding * 2;
                total += withMargin;
            }
            final boolean fits = total <= maxTotalWidthForTabs;
            if (fits || pass == PASS_TEXT_SIZES_DP.length - 1) {
                chosenPass = pass;
                break;
            }
        }

        applyPassTextSize(chosenPass);

        final int tabPadding = dp(PASS_PADDINGS_DP[chosenPass]);
        final int maxTabTextWidthIfEq = (maxTotalWidthForTabs / Math.max(1, visibleChildCount)) - tabPadding * 2;

        float totalWidth = 0;
        int totalWeight = 0;
        for (int a = 0, N = getChildCount(); a < N; a++) {
            final View child = getChildAt(a);
            if (!isViewVisible(child)) {
                tabsTextWidth[a] = tabsTextWidthWithMargin[a] = 0;
                tabsWeight[a] = 0;
                continue;
            }

            tabsTextWidthWithMargin[a] = tabsTextWidth[a] + tabPadding * 2;
            tabsWeight[a] = tabsTextWidthWithMargin[a] > (maxTabTextWidthIfEq + tabPadding * 2) ? 0 : 1;

            totalWidth += tabsTextWidthWithMargin[a];
            totalWeight += tabsWeight[a];
        }

        if (totalWeight == 0) {
            for (int a = 0, N = getChildCount(); a < N; a++) {
                tabsWeight[a] = isViewVisible(getChildAt(a)) ? 1 : 0;
            }
            totalWeight = visibleChildCount;
        }

        if (totalWidth > maxTotalWidthForTabs) {
            final float m = maxTotalWidthForTabs / totalWidth;
            for (int a = 0, N = getChildCount(); a < N; a++) {
                tabsTextWidthWithMargin[a] *= m;
            }
        } else if (totalWidth < minTotalWidthForTabs) {
            final float growW = minTotalWidthForTabs - totalWidth;
            final float growP = growW / totalWeight;

            for (int a = 0, N = getChildCount(); a < N; a++) {
                tabsTextWidthWithMargin[a] += growP * tabsWeight[a];
            }
        }

        int l = 0;
        for (int a = 0, N = getChildCount(); a < N; a++) {
            if (!isViewVisible(getChildAt(a))) {
                continue;
            }

            tabsWidth[a] = Math.round(tabsTextWidthWithMargin[a]);
            tabsLeftPos[a] = l;
            l += tabsWidth[a];
        }
        setMeasuredDimension(l + getPaddingLeft() + getPaddingRight(), height);
        for (int a = 0, N = getChildCount(); a < N; a++) {
            final View child = getChildAt(a);
            child.measure(
                MeasureSpec.makeMeasureSpec(tabsWidth[a], MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(tabHeight, MeasureSpec.EXACTLY));
        }

        calculateTotalSizesAfterMeasure();
    }

    public interface Tab {
        float measureTextWidth();
        default float measureTextWidth(float textSizeDp) { return measureTextWidth(); }
        default void setTextSizeDp(float textSizeDp) {}
    }



    // fills tabsTextWidth[] and return visible child count;

    private float[] tabsTextWidth;
    private float[] tabsTextWidthWithMargin;
    private int[] tabsWeight;
    private int[] tabsWidth;

    private int[] tabsLeftPos;


    private int visibleChildCount;
    private int biggestTabTextWidth;

    private void measureTabTexts(float textSizeDp) {
        final int childCount = getChildCount();
        if (tabsTextWidth == null || tabsTextWidth.length < childCount) {
            tabsTextWidth = new float[childCount];
            tabsTextWidthWithMargin = new float[childCount];
            tabsWeight = new int[childCount];
            tabsLeftPos = new int[childCount];
            tabsWidth = new int[childCount];
        }

        float maxTabWidthF = 0;
        int index = 0;

        for (int a = 0; a < childCount; a++) {
            final View child = getChildAt(a);
            if (!isViewVisible(child)) {
                tabsTextWidth[a] = -1;
                continue;
            }

            final float tabWidth;
            if (child instanceof MainTabsLayout.Tab) {
                tabWidth = ((MainTabsLayout.Tab) child).measureTextWidth(textSizeDp);
            } else {
                tabWidth = 0;
            }

            tabsTextWidth[a] = tabWidth;
            maxTabWidthF = Math.max(maxTabWidthF, tabWidth);
            index++;
        }

        biggestTabTextWidth = (int) Math.ceil(maxTabWidthF);
        visibleChildCount = index;
    }

    private void applyPassTextSize(int pass) {
        final float textSizeDp = PASS_TEXT_SIZES_DP[pass];
        for (int a = 0, N = getChildCount(); a < N; a++) {
            final View child = getChildAt(a);
            if (child instanceof MainTabsLayout.Tab) {
                ((MainTabsLayout.Tab) child).setTextSizeDp(textSizeDp);
            }
        }
    }

    @Override
    protected void setChildVisibilityFactor(View view, float factor) {
        final float s = lerp(0.7f, 1f, factor);
        view.setAlpha(factor);
        view.setScaleX(s);
        view.setScaleY(s);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkVisualWidth();
    }

    @Override
    protected void onItemsChanged() {
        super.onItemsChanged();
        checkVisualWidth();
    }

    private void checkVisualWidth() {
        for (int a = 0, N = getEntriesCount(); a < N; a++) {
            final ListAnimator.Entry<Holder> entry = getEntry(a);
            final float width = entry.getRectF().width();
            ((GlassTabView) entry.item.view).setVisualWidth(width);
        }
    }

    // ---------------------------------------------------------------------------------------
    // BrinaGram liquid dock.
    //
    // A capsule rests behind the selected tab: a pale/dark wash with a soft highlight, and the
    // tab under it drawn in the accent color. Pressing anywhere swells the capsule into clear
    // glass (78/56 of its size, jelly springs) that comes to the finger and rides it, stretching
    // with speed, magnifying and tinting the tabs under it, while the whole bar is tugged a few dp
    // along with the drag. On release it settles on the nearest tab, and the page switches once
    // the capsule has arrived (capped at 180ms) so the motion is never cut off.
    // ---------------------------------------------------------------------------------------

    /** 78dp of swell on a 56dp capsule. */
    private static final float PRESSED_SCALE = 78f / 56f;
    /** The tab under the capsule grows with the press. */
    private static final float PRESSED_TAB_SCALE = 1.12f;
    /** How far the whole bar is tugged by a drag. */
    private static final int TUG_DP = 4;
    private static final long NAVIGATE_MAX_WAIT_MS = 180;

    private final Set<View> tabsWithIgnoreClick = new HashSet<>();
    private final android.graphics.RectF capsuleRect = new android.graphics.RectF();
    private final android.graphics.Path capsulePath = new android.graphics.Path();
    private final Paint accentLayerPaint = new Paint();
    private final Paint washPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Capsule center, in px. NaN until the first layout. */
    private float capsuleX = Float.NaN;
    private float pressProgress;
    private float capsuleScaleX = 1f, capsuleScaleY = 1f;
    /** Capsule speed in tab widths per second, smoothed. */
    private float velocity;
    private float lastX = Float.NaN;
    private long lastFrameNanos;

    private boolean pressed;
    private boolean dragging;
    private boolean longPressed;
    private float downX;
    private float tugRaw;
    private float tug;
    private View downTab;
    private View inkTab;
    private final int tapSlop = dp(12);

    {
        rimPaint.setStyle(Paint.Style.STROKE);
        setWillNotDraw(false);
    }

    private static FloatPropertyCompat<MainTabsLayout> property(String name, java.util.function.Function<MainTabsLayout, Float> get, java.util.function.BiConsumer<MainTabsLayout, Float> set) {
        return new FloatPropertyCompat<MainTabsLayout>(name) {
            @Override
            public float getValue(MainTabsLayout object) {
                return get.apply(object);
            }

            @Override
            public void setValue(MainTabsLayout object, float value) {
                set.accept(object, value);
                object.invalidate();
            }
        };
    }

    // stiff and critically damped
    private final SpringAnimation capsuleXSpring = new SpringAnimation(this, property("capsuleX", o -> o.capsuleX, (o, v) -> o.capsuleX = v))
        .setSpring(new SpringForce().setStiffness(1000f).setDampingRatio(1f));
    private final SpringAnimation pressSpring = new SpringAnimation(this, property("press", o -> o.pressProgress, (o, v) -> o.pressProgress = v))
        .setSpring(new SpringForce().setStiffness(1000f).setDampingRatio(1f));
    // jelly: spring(0.6f, 250f) / spring(0.7f, 250f)
    private final SpringAnimation scaleXSpring = new SpringAnimation(this, property("capsuleScaleX", o -> o.capsuleScaleX, (o, v) -> o.capsuleScaleX = v))
        .setSpring(new SpringForce(1f).setStiffness(250f).setDampingRatio(0.6f));
    private final SpringAnimation scaleYSpring = new SpringAnimation(this, property("capsuleScaleY", o -> o.capsuleScaleY, (o, v) -> o.capsuleScaleY = v))
        .setSpring(new SpringForce(1f).setStiffness(250f).setDampingRatio(0.7f));
    // the bar lets go of the tug on spring(1f, 300f)
    private final SpringAnimation tugSpring = new SpringAnimation(this, property("tug", o -> o.tug, (o, v) -> { o.tug = v; o.setTranslationX(v); }))
        .setSpring(new SpringForce(0f).setStiffness(300f).setDampingRatio(1f));

    {
        pressSpring.setMinimumVisibleChange(0.001f);
        scaleXSpring.setMinimumVisibleChange(0.001f);
        scaleYSpring.setMinimumVisibleChange(0.001f);
    }

    private final Runnable longPressRunnable = () -> {
        if (!pressed || dragging || downTab == null) {
            return;
        }
        if (downTab.isLongClickable() && downTab.performLongClick()) {
            longPressed = true;
            pressed = false;
            unpress();
            final View selected = findSelectedTab();
            if (selected != null) {
                moveCapsule(centerX(selected));
            }
        }
    };

    private void press() {
        pressSpring.animateToFinalPosition(1f);
        scaleXSpring.animateToFinalPosition(PRESSED_SCALE);
        scaleYSpring.animateToFinalPosition(PRESSED_SCALE);
    }

    private void unpress() {
        pressSpring.animateToFinalPosition(0f);
        scaleXSpring.animateToFinalPosition(1f);
        scaleYSpring.animateToFinalPosition(1f);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        if (child instanceof GlassTabView) {
            ((GlassTabView) child).setLiquidDock(true);
        }
    }

    /** Kept for API compatibility; the liquid dock resolves taps for every tab itself. */
    public void addTabToIgnoreClick(View v) {
        tabsWithIgnoreClick.add(v);
    }

    public void setTabSelected(View tab, boolean animated) {
        for (int a = 0, N = getChildCount(); a < N; a++) {
            final View child = getChildAt(a);
            if (child instanceof GlassTabView) {
                ((GlassTabView) child).setSelected(child == tab, animated);
            }
        }
        if (tab != null && !pressed) {
            final float cx = centerX(tab);
            if (!animated || Float.isNaN(capsuleX) || getWidth() == 0) {
                capsuleXSpring.cancel();
                capsuleX = cx;
                springTarget = cx;
            } else if (gestureActive) {
                // a page swipe is driving the capsule; it settles when the swipe ends
            } else if (Math.abs(capsuleX - cx) < tabPitch() * 0.5f) {
                // a page swipe already carried the capsule here: just settle
                moveCapsule(cx);
            } else if (Math.abs(getSpringTarget() - cx) > 0.5f) {
                // a change that did not come from this bar (page swipe, back stack):
                // the same press, slide and settle a tap gets
                press();
                moveCapsule(cx);
                releaseWhenArrived(cx, null);
            }
        }
        invalidate();
    }

    private float springTarget = Float.NaN;

    /** Page swipe in progress: the capsule follows between {@code from} and {@code to}. */
    private boolean gestureActive;

    public void setGestureCapsule(@Nullable View from, @Nullable View to, float fraction, boolean active) {
        if (!active) {
            if (gestureActive) {
                gestureActive = false;
                // the swipe ended: settle on whichever tab is selected now
                final View selected = findSelectedTab();
                if (selected != null && !pressed) {
                    moveCapsule(centerX(selected));
                }
            }
            return;
        }
        if (pressed || from == null || from.getVisibility() != View.VISIBLE) {
            return;
        }
        gestureActive = true;
        final float target = to == null || to.getVisibility() != View.VISIBLE ? centerX(from) : lerp(centerX(from), centerX(to), fraction);
        capsuleXSpring.cancel();
        capsuleX = springTarget = target;
        invalidate();
    }

    private float getSpringTarget() {
        return Float.isNaN(springTarget) ? capsuleX : springTarget;
    }

    private void moveCapsule(float x) {
        springTarget = x;
        capsuleXSpring.animateToFinalPosition(x);
    }

    @Nullable
    private View findSelectedTab() {
        for (int a = 0, N = getChildCount(); a < N; a++) {
            final View child = getChildAt(a);
            if (child.getVisibility() == View.VISIBLE && child instanceof GlassTabView && ((GlassTabView) child).isTabSelected()) {
                return child;
            }
        }
        return null;
    }

    // ---- touch: the gesture belongs to the whole bar ----

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MainTabsActivity keeps the dock "disabled" at all times; what matters is whether it is shown
        if (getVisibility() != View.VISIBLE || getAlpha() < 0.5f) {
            return false;
        }
        final float x = e.getX();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downTab = findChildUnder(this, x, e.getY());
                if (downTab == null) {
                    downTab = findNearestVisibleChildByX(x, this);
                }
                if (downTab == null) {
                    return false;
                }
                downX = x;
                tugRaw = 0f;
                tugSpring.cancel();
                pressed = true;
                dragging = false;
                longPressed = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                if (Float.isNaN(capsuleX)) {
                    capsuleX = centerX(downTab);
                }
                press();
                // pressed on the capsule: it stays; pressed elsewhere: it flows to the finger's tab
                if (Math.abs(x - capsuleX) >= tabPitch() / 2f) {
                    moveCapsule(clampXToChildrenCenters(x, this));
                }
                AndroidUtilities.runOnUIThread(longPressRunnable, android.view.ViewConfiguration.getLongPressTimeout());
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!pressed) {
                    return longPressed;
                }
                if (!dragging && Math.abs(x - downX) > tapSlop) {
                    dragging = true;
                    AndroidUtilities.cancelRunOnUIThread(longPressRunnable);
                }
                if (dragging) {
                    final float before = getSpringTarget();
                    final float after = clampXToChildrenCenters(x, this);
                    if (findNearestVisibleChildByX(before, this) != findNearestVisibleChildByX(after, this)) {
                        try {
                            performHapticFeedback(android.view.HapticFeedbackConstants.TEXT_HANDLE_MOVE);
                        } catch (Exception ignored) {}
                    }
                    moveCapsule(after);
                    tugRaw = x - downX;
                    applyTug();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                AndroidUtilities.cancelRunOnUIThread(longPressRunnable);
                if (longPressed || !pressed) {
                    longPressed = false;
                    return true;
                }
                pressed = false;
                tugSpring.animateToFinalPosition(0f);
                final boolean cancel = e.getActionMasked() == MotionEvent.ACTION_CANCEL;
                final View selected = findSelectedTab();
                final View landed = cancel ? selected : findNearestVisibleChildByX(getSpringTarget(), this);
                if (landed == null) {
                    unpress();
                    return true;
                }
                final float cx = centerX(landed);
                moveCapsule(cx);
                if (cancel) {
                    releaseWhenArrived(cx, null);
                } else if (landed != selected) {
                    // let the capsule travel before the (heavy) page switch starts
                    releaseWhenArrived(cx, landed);
                } else {
                    releaseWhenArrived(cx, null);
                    if (!dragging || Math.abs(x - downX) < tapSlop) {
                        landed.performClick(); // re-tap: the page scrolls to top
                    }
                }
                dragging = false;
                return true;
            }
        }
        return pressed;
    }

    /** Unpresses once the capsule is (nearly) at {@code targetX}, then optionally clicks {@code tab}. */
    private void releaseWhenArrived(float targetX, @Nullable View tab) {
        final long start = android.os.SystemClock.elapsedRealtime();
        final boolean[] clicked = {false};
        final Runnable[] check = new Runnable[1];
        check[0] = () -> {
            if (pressed) {
                return; // a new gesture took over
            }
            final long waited = android.os.SystemClock.elapsedRealtime() - start;
            final boolean arrived = Math.abs(capsuleX - targetX) < tabPitch() * 0.05f;
            if (tab != null && !clicked[0] && (Math.abs(capsuleX - targetX) < tabPitch() * 0.3f || waited >= NAVIGATE_MAX_WAIT_MS)) {
                clicked[0] = true;
                tab.performClick();
            }
            if (arrived || waited >= 600) {
                unpress();
                if (tab != null && !clicked[0]) {
                    tab.performClick();
                }
            } else {
                postOnAnimation(check[0]);
            }
        };
        postOnAnimation(check[0]);
    }

    private void applyTug() {
        final float width = getWidth();
        if (width == 0f || tugRaw == 0f) {
            tug = 0f;
        } else {
            final float fraction = Math.max(-1f, Math.min(1f, tugRaw / width));
            tug = dp(TUG_DP) * Math.signum(fraction) * CubicBezierInterpolator.EASE_OUT.getInterpolation(Math.abs(fraction));
        }
        setTranslationX(tug);
    }

    private float tabPitch() {
        final int n = getVisibleCount();
        return n == 0 ? getWidth() : (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) n;
    }

    private int getVisibleCount() {
        int n = 0;
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getVisibility() == View.VISIBLE) n++;
        }
        return n;
    }

    // ---- drawing ----

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        final View selected = findSelectedTab();
        if (Float.isNaN(capsuleX) && selected != null) {
            capsuleX = centerX(selected);
        }
        // follow layout changes (tabs appearing/disappearing) while idle
        if (!pressed && !gestureActive && selected != null && !capsuleXSpring.isRunning() && pressProgress <= 0.001f) {
            capsuleX = springTarget = centerX(selected);
        }
        if (Float.isNaN(capsuleX)) {
            super.dispatchDraw(canvas);
            return;
        }

        trackVelocity();
        updateInk();

        final float press = Math.max(0f, Math.min(1f, pressProgress));
        final boolean dark = GlassTabView.isGlassDark(resourcesProvider);
        final float tabHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        // the tab's own width, so the resting capsule never spills onto its neighbours
        final float baseWidth = getInterpolatedWidthByX(capsuleX, this);
        // longer and flatter the faster it travels, round again as it slows
        final float v = velocity / 10f;
        final float sx = capsuleScaleX / (1f - clamp(v * 0.75f, -0.2f, 0.2f));
        final float sy = capsuleScaleY * (1f - clamp(v * 0.25f, -0.2f, 0.2f));
        final float w = baseWidth * sx;
        final float h = tabHeight * sy;
        final float cy = getHeight() / 2f;
        capsuleRect.set(capsuleX - w / 2f, cy - h / 2f, capsuleX + w / 2f, cy + h / 2f);
        final float r = Math.min(w, h) / 2f;
        capsulePath.rewind();
        capsulePath.addRoundRect(capsuleRect, r, r, android.graphics.Path.Direction.CW);

        // the pane lights up a little while held, with a glow following the capsule
        if (press > 0.01f) {
            glowPaint.setShader(new android.graphics.RadialGradient(capsuleX, cy, getHeight() * 1.5f,
                Theme.multAlpha(0xFFFFFFFF, 0.16f * press), 0, android.graphics.Shader.TileMode.CLAMP));
            final float m = dp(org.telegram.ui.DialogsActivity.MAIN_TABS_MARGIN);
            final float dockR = (getHeight() - m * 2f) / 2f;
            canvas.drawRoundRect(m, m, getWidth() - m, getHeight() - m, dockR, dockR, glowPaint);
        }

        // 1. the bar's tabs, in their own ink — but not under the capsule: there only the
        //    accent copy is drawn, otherwise the magnified copy and the original both show
        canvas.save();
        org.telegram.ui.Components.glass.LiquidGlassStyle.clipOut(canvas, capsulePath);
        super.dispatchDraw(canvas);
        canvas.restore();

        // 2. capsule shadow (0.35 at rest, 1 held) and wash (0.8, clearing to 0.2 when held)
        shadowPaint.setColor(0);
        shadowPaint.setShadowLayer(dp(8), 0, dp(2), Theme.multAlpha(0x40000000, lerp(0.35f, 1f, press)));
        canvas.save();
        org.telegram.ui.Components.glass.LiquidGlassStyle.clipOut(canvas, capsulePath);
        canvas.drawRoundRect(capsuleRect, r, r, shadowPaint);
        canvas.restore();
        // near-opaque at rest so the plain label under the capsule doesn't ghost through the accent copy
        washPaint.setColor(Theme.multAlpha(dark ? 0xFF1C1B1C : 0xFFF2F2F2, 0.94f * (1f - 0.75f * press)));
        canvas.drawRoundRect(capsuleRect, r, r, washPaint);

        // 3. through the glass: every tab in the accent ink, magnified as the capsule is held
        final int accent = Theme.getColor(Theme.key_glass_tabSelected, resourcesProvider);
        accentLayerPaint.setColorFilter(new android.graphics.PorterDuffColorFilter(accent, android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.save();
        canvas.clipPath(capsulePath);
        canvas.saveLayer(capsuleRect, accentLayerPaint);
        final float magnify = lerp(1f, PRESSED_TAB_SCALE, press);
        canvas.scale(magnify, magnify, capsuleX, cy);
        // photos (the profile avatar) are not tinted: they are drawn as they are, below
        GlassTabView.liquidDrawMode = GlassTabView.DRAW_NO_AVATAR;
        super.dispatchDraw(canvas);
        GlassTabView.liquidDrawMode = GlassTabView.DRAW_ALL;
        canvas.restore();
        if (hasAvatarTab()) {
            canvas.save();
            canvas.scale(magnify, magnify, capsuleX, cy);
            GlassTabView.liquidDrawMode = GlassTabView.DRAW_AVATAR_ONLY;
            super.dispatchDraw(canvas);
            GlassTabView.liquidDrawMode = GlassTabView.DRAW_ALL;
            canvas.restore();
        }
        // inner shadow while held
        if (press > 0.01f) {
            rimPaint.setShader(null);
            rimPaint.setStrokeWidth(dp(8) * press);
            rimPaint.setColor(Theme.multAlpha(0x22000000, press));
            rimPaint.setMaskFilter(new android.graphics.BlurMaskFilter(dp(8) * press, android.graphics.BlurMaskFilter.Blur.NORMAL));
            canvas.drawRoundRect(capsuleRect, r, r, rimPaint);
            rimPaint.setMaskFilter(null);
        }
        canvas.restore();

        // 4. specular highlight (white, lit from 45deg; 0.5 at rest -> 1 held)
        drawHighlight(canvas, capsuleRect, r, lerp(0.5f, 1f, press));

        if (pressed || press > 0.001f || capsuleXSpring.isRunning() || Math.abs(velocity) > 0.01f) {
            invalidate();
        }
    }

    private boolean hasAvatarTab() {
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child instanceof GlassTabView && child.getVisibility() == View.VISIBLE && ((GlassTabView) child).hasAvatar()) {
                return true;
            }
        }
        return false;
    }

    private void trackVelocity() {
        final long now = System.nanoTime();
        final float pitch = Math.max(1f, tabPitch());
        if (!Float.isNaN(lastX) && lastFrameNanos != 0) {
            final float dt = Math.max(1e-3f, Math.min(1f / 30f, (now - lastFrameNanos) / 1e9f));
            final float raw = (capsuleX - lastX) / pitch / dt;
            // eased towards the raw figure at the rate a 300-stiffness spring would
            velocity += (raw - velocity) * (1f - (float) Math.exp(-dt * 17f));
            if (Math.abs(velocity) < 0.01f && Math.abs(raw) < 0.01f) {
                velocity = 0f;
            }
        }
        lastX = capsuleX;
        lastFrameNanos = now;
    }

    /** The lit tab is the one the capsule is over, not the route. */
    private void updateInk() {
        final View under = findNearestVisibleChildByX(capsuleX, this);
        if (under != inkTab) {
            final boolean animated = inkTab != null;
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                if (child instanceof GlassTabView) {
                    ((GlassTabView) child).setInk(child == under, animated);
                }
            }
            inkTab = under;
        }
    }

    private void drawHighlight(Canvas canvas, android.graphics.RectF rect, float r, float alpha) {
        final float stroke = AndroidUtilities.dpf2(1f);
        rimPaint.setStrokeWidth(stroke);
        rimPaint.setColor(0xFFFFFFFF);
        rimPaint.setShader(new android.graphics.LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            new int[] {
                Theme.multAlpha(0xFFFFFFFF, 0.5f * alpha),
                Theme.multAlpha(0xFFFFFFFF, 0.04f * alpha),
                Theme.multAlpha(0xFFFFFFFF, 0.04f * alpha),
                Theme.multAlpha(0xFFFFFFFF, 0.5f * alpha)
            },
            new float[] {0f, 0.35f, 0.65f, 1f},
            android.graphics.Shader.TileMode.CLAMP));
        final float inset = stroke / 2f;
        rect.inset(inset, inset);
        canvas.drawRoundRect(rect, r - inset, r - inset, rimPaint);
        rect.inset(-inset, -inset);
        rimPaint.setShader(null);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    @Override
    public void setScaleY(float scaleY) {
        super.setScaleY(scaleY);
        checkLayerType();
    }

    @Override
    public void setScaleX(float scaleX) {
        super.setScaleX(scaleX);
        checkLayerType();
    }

    private void checkLayerType() {
        final int layerType = Math.abs(getScaleX() - 1f) < 0.0001f && Math.abs(getScaleY() - 1f) < 0.0001f ?
            View.LAYER_TYPE_NONE : View.LAYER_TYPE_HARDWARE;
        if (getLayerType() != layerType) {
            setLayerType(layerType, null);
            invalidate();
        }
    }

    // ---- geometry helpers ----

    private static float centerX(View v) {
        return v.getX() + v.getWidth() * 0.5f;
    }

    public static View findChildUnder(ViewGroup parent, float x, float y) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            if (x >= child.getLeft() && x <= child.getRight() && y >= child.getTop() && y <= child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    private static float clampXToChildrenCenters(float x, ViewGroup parent) {
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        boolean found = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view == null || view.getVisibility() != View.VISIBLE) continue;
            float c = centerX(view);
            min = Math.min(min, c);
            max = Math.max(max, c);
            found = true;
        }
        if (!found) return x;
        return Math.max(min, Math.min(max, x));
    }

    @Nullable
    private static View findNearestVisibleChildByX(float x, ViewGroup parent) {
        View nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view == null || view.getVisibility() != View.VISIBLE) continue;
            float distance = Math.abs(centerX(view) - x);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = view;
            }
        }
        return nearest;
    }

    private static float getInterpolatedWidthByX(float x, ViewGroup parent) {
        View left = null, right = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view == null || view.getVisibility() != View.VISIBLE) continue;
            float c = centerX(view);
            if (c <= x && (left == null || c > centerX(left))) left = view;
            if (c >= x && (right == null || c < centerX(right))) right = view;
        }
        if (left == null && right == null) return 0f;
        if (left == null) return right.getWidth();
        if (right == null || left == right) return left.getWidth();
        float lx = centerX(left), rx = centerX(right);
        if (lx == rx) return left.getWidth();
        return lerp(left.getWidth(), right.getWidth(), (x - lx) / (rx - lx));
    }
}
