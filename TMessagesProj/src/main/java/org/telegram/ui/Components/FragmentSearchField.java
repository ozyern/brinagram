package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationNotificationsLocker;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.FiltersView;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

import java.util.ArrayList;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;

@SuppressLint("ViewConstructor")
public class FragmentSearchField extends FrameLayout implements FactorAnimator.Target, Theme.Colorable {
    private static final int ANIMATOR_ID_CLOSE_BUTTON_VISIBLE = 0;
    private static final int ANIMATOR_ID_SEARCH_ICON_VISIBLE = 1;
    private static final int ANIMATOR_ID_SEARCH_FILTERS_WIDTH = 2;
    private static final int ANIMATOR_ID_CANCEL_VISIBLE = 3;

    /** Field metrics: the glyph sits this far in, and text starts just past it. */
    private static final int GLYPH_INSET_DP = 8;
    private static final int TEXT_INSET_DP = 34;
    private static final int TEXT_SIZE_SP = 17;
    private static final int CANCEL_GAP_DP = 10;
    /** The system's tertiary fill and secondary label, on dark and light backgrounds. */
    private static final int SYSTEM_FILL_DARK = 0x3D767680;
    private static final int SYSTEM_FILL_LIGHT = 0x1F767680;
    private static final int SECONDARY_LABEL_DARK = 0x99EBEBF5;
    private static final int SECONDARY_LABEL_LIGHT = 0x993C3C43;

    private final BoolAnimator animatorCloseIconVisible = new BoolAnimator(ANIMATOR_ID_CLOSE_BUTTON_VISIBLE, this, CubicBezierInterpolator.EASE_OUT_QUINT, 380, false);
    private final BoolAnimator animatorSearchIconVisible = new BoolAnimator(ANIMATOR_ID_SEARCH_ICON_VISIBLE, this, CubicBezierInterpolator.EASE_OUT_QUINT, 380, true);
    private final FactorAnimator animatorSearchFiltersWidth = new FactorAnimator(ANIMATOR_ID_SEARCH_FILTERS_WIDTH, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 280);
    private final BoolAnimator animatorCancelVisible = new BoolAnimator(ANIMATOR_ID_CANCEL_VISIBLE, this, CubicBezierInterpolator.EASE_OUT_QUINT, 380, false);

    private final Theme.ResourcesProvider resourcesProvider;

    private final ImageView searchIcon;
    private final ImageView closeIcon;
    private final TextView cancelButton;
    private boolean cancelButtonEnabled;
    private final LinearLayout additionalIconsLayout;
    private boolean closeButtonForcedVisible;
    public final EditTextBoldCursor editText;
    private BlurredBackgroundDrawable blurredBackgroundDrawable;

    public FragmentSearchField(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        editText = new EditTextBoldCursor(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                setPivotX(getPaddingLeft());
                setPivotY(getMeasuredHeight() / 2.0f);
            }

            @Override
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && editText.length() == 0 && hasRemovableFilters()) {
                    if (hasRemovableFilters()) {
                        FiltersView.MediaFilterData filterToRemove = currentSearchFilters.get(currentSearchFilters.size() - 1);
                        if (searchFiltersListener != null) {
                            searchFiltersListener.onSearchFilterCleared(filterToRemove);
                        }
                        removeSearchFilter(filterToRemove);
                    }
                    return true;
                }
                return super.onKeyDown(keyCode, event);
            }
        };
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_SP);
        editText.setCursorWidth(1.5f);
        editText.setInputType(editText.getInputType() | InputType.TYPE_TEXT_VARIATION_FILTER);
        editText.setSingleLine(true);
        editText.setBackground(null);
        editText.setVerticalScrollBarEnabled(false);
        editText.setHorizontalScrollBarEnabled(false);
        editText.setPadding(dp(TEXT_INSET_DP), 0, dp(TEXT_INSET_DP), 0);
        editText.setClipToPadding(true);
        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!currentSearchFilters.isEmpty()) {
                    if (s.length() > 0 && selectedFilterIndex >= 0) {
                        selectedFilterIndex = -1;
                        onFiltersChanged();
                    }
                }
                checkCloseButtonVisible();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            editText.setLocalePreferredLineHeightForMinimumUsed(false);
        }
        addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 0, 0, 0));

        searchIcon = new ImageView(context);
        searchIcon.setScaleType(ImageView.ScaleType.CENTER);
        searchIcon.setImageResource(R.drawable.brina_search_glass);
        addView(searchIcon, LayoutHelper.createFrame(20, 20, Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), GLYPH_INSET_DP, 0, GLYPH_INSET_DP, 0));

        additionalIconsLayout = new LinearLayout(context);
        additionalIconsLayout.setOrientation(LinearLayout.HORIZONTAL);
        addView(additionalIconsLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT), 32, 0, 32, 0));

        closeIcon = new ImageView(context);
        closeIcon.setScaleType(ImageView.ScaleType.CENTER);
        closeIcon.setImageResource(R.drawable.brina_search_clear);
        closeIcon.setVisibility(GONE);
        closeIcon.setOnClickListener(v -> {
            if (cancelButtonEnabled && !hasRemovableFilters()) {
                editText.getText().clear(); // with a Cancel button beside the field, this only clears
            } else if (hasRemovableFilters()) {
                if (searchFiltersListener != null) {
                    searchFiltersListener.hideActionMode();
                }
                for (int i = 0; i < currentSearchFilters.size(); i++) {
                    if (searchFiltersListener != null && currentSearchFilters.get(i).removable) {
                        searchFiltersListener.onSearchFilterCleared(currentSearchFilters.get(i));
                    }
                }
                clearSearchFilters();
            } else if (onCloseSearch != null) {
                onCloseSearch.run();
            } else {
                editText.getText().clear();
            }
        });
        addView(closeIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT), 5, 0, 5, 0));

        cancelButton = new TextView(context);
        cancelButton.setText(LocaleController.getString(R.string.Cancel));
        cancelButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_SP);
        cancelButton.setGravity(Gravity.CENTER);
        cancelButton.setVisibility(INVISIBLE); // measured even while hidden, so the field knows how far to draw back
        cancelButton.setOnClickListener(v -> {
            if (onCloseSearch != null) {
                onCloseSearch.run();
            }
        });
        addView(cancelButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT)));

        searchFilterLayout = new LinearLayout(getContext()) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                animatorSearchFiltersWidth.animateTo(getMeasuredWidth());
                super.onLayout(changed, l, t, r, b);
            }
        };
        searchFilterLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchFilterLayout.setVisibility(View.VISIBLE);
        addView(searchFilterLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 32, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 4, 0, 4, 0));

        setWillNotDraw(false);
        checkUi_editTextPaddings();
        updateColors();
    }

    public void setHintText(CharSequence text, boolean animated) {
        editText.setHintText(text, animated);
    }

    public void addAdditionalIcon(View icon) {
        additionalIconsLayout.addView(icon);
    }

    private Drawable bg;

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        canvas.save();
        final int cancelInset = getCancelInset();
        final int insetLeft = LocaleController.isRTL ? cancelInset : 0;
        final int insetRight = LocaleController.isRTL ? 0 : cancelInset;
        if (bg != null) {
            bg.setBounds(
                getPaddingLeft() + insetLeft,
                getPaddingTop(),
                getWidth() - getPaddingRight() - insetRight,
                getHeight() - getPaddingBottom()
            );
            bg.draw(canvas);
        }
        if (blurredBackgroundDrawable != null) {
            blurredBackgroundDrawable.setBounds(
                    getPaddingLeft() - dp(4) + insetLeft,
                    getPaddingTop() - dp(4),
                    getWidth() - getPaddingRight() + dp(4) - insetRight,
                    (getHeight() - getPaddingBottom()) + dp(4));
            blurredBackgroundDrawable.draw(canvas);
        }
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    public void setupBlurredBackground(BlurredBackgroundDrawable drawable) {
        drawable.setRadius(dp(20));
        drawable.setPadding(dp(4));
        blurredBackgroundDrawable = drawable;
    }

    public void setBlurredBackgroundVisibility(float visibility) {
        final int alpha = (int) (255 * visibility);
        boolean changed = false;
        if (blurredBackgroundDrawable != null) {
            if (blurredBackgroundDrawable.getAlpha() != alpha) {
                blurredBackgroundDrawable.setAlpha(alpha);
                changed = true;
            }
        }
        if (bg != null) {
            if (bg.getAlpha() != (255 - alpha)) {
                bg.setAlpha(255 - alpha);
                changed = true;
            }
        }
        if (changed) {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        checkUi_editTextPaddings();
    }

    private void checkUi_editTextPaddings() {
        final int filtersWidth = (int) animatorSearchFiltersWidth.getFactor() + dp(6); //searchFilterLayout.getWidth();
        final int pStart = Math.max(filtersWidth, dp(TEXT_INSET_DP));
        final int pEnd = dp(TEXT_INSET_DP) + additionalIconsLayout.getMeasuredWidth() + getCancelInset();

        final int pLeft = LocaleController.isRTL ? pEnd : pStart;
        final int pRight = LocaleController.isRTL ? pStart : pEnd;

        AndroidUtilities.rectTmp2.set(
            pLeft, 0,
            editText.getMeasuredWidth() - pRight,
            editText.getMeasuredHeight()
        );
        editText.setClipBounds(AndroidUtilities.rectTmp2);
        editText.setPadding(pLeft, 0, pRight, 0);
    }

    /** How far the field has drawn back to make room for the Cancel button beside it. */
    private int getCancelInset() {
        if (!cancelButtonEnabled) {
            return 0;
        }
        return (int) ((cancelButton.getMeasuredWidth() + dp(CANCEL_GAP_DP)) * animatorCancelVisible.getFloatValue());
    }

    /**
     * Shows a Cancel button beside the field while search is open (see {@link #setCloseButtonVisible});
     * it runs the close listener, and the button inside the field only clears the text.
     */
    public void setCancelButtonEnabled(boolean enabled) {
        cancelButtonEnabled = enabled;
        checkCloseButtonVisible();
    }

    public boolean isSectionBackground;
    public void setSectionBackground() {
        isSectionBackground = true;
        setPadding(dp(3), dp(3), dp(3), dp(3));
        updateColors();
    }

    private boolean isWhiteBackground;

    public void setWhiteBackground() {
        isWhiteBackground = true;
        updateColors();
    }

    @Override
    public void updateColors() {
        final boolean isDark = resourcesProvider != null ? resourcesProvider.isDark() : Theme.isCurrentThemeDark();
        bg = isSectionBackground ?
            Theme.createRoundRectDrawableShadowed(dp(20), getThemedColor(Theme.key_windowBackgroundWhite)) :
            Theme.createRoundRectDrawable(dp(20), isWhiteBackground ? getThemedColor(Theme.key_windowBackgroundWhite) : (isDark ? SYSTEM_FILL_DARK : SYSTEM_FILL_LIGHT));
        final int secondaryLabel = isDark ? SECONDARY_LABEL_DARK : SECONDARY_LABEL_LIGHT;
        searchIcon.setColorFilter(secondaryLabel, PorterDuff.Mode.SRC_IN);
        closeIcon.setColorFilter(secondaryLabel, PorterDuff.Mode.SRC_IN);
        closeIcon.setBackground(null);
        editText.setHintTextColor(secondaryLabel);
        editText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorColor(getThemedColor(Theme.key_groupcreate_cursor));
        cancelButton.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4));
        if (blurredBackgroundDrawable != null) {
            blurredBackgroundDrawable.updateColors();
        }

        for (int i = 0, N = additionalIconsLayout.getChildCount(); i < N; i++) {
            final View view = additionalIconsLayout.getChildAt(i);
            if (view instanceof ActionBarMenuItem) {
                final ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.getIconView() != null) {
                    item.getIconView().setColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlackText, 0.6f), PorterDuff.Mode.MULTIPLY);
                }
                view.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, dp(17)));
            }
        }

        for (int i = 0, N = searchFilterLayout.getChildCount(); i < N; i++) {
            if (searchFilterLayout.getChildAt(i) instanceof ActionBarMenuItem.SearchFilterView) {
                ((ActionBarMenuItem.SearchFilterView) searchFilterLayout.getChildAt(i)).updateColors();
            }
        }

        invalidate();
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    private int getThemedColor(int key, float alpha) {
        return Theme.multAlpha(getThemedColor(key), alpha);
    }

    private Runnable onCloseSearch;

    public void setCloseButtonOnClickListener(Runnable onCloseSearch) {
        this.onCloseSearch = onCloseSearch;
    }

    public void setCloseButtonVisible(boolean visible) {
        closeButtonForcedVisible = visible;
        checkCloseButtonVisible();
    }

    private void checkCloseButtonVisible() {
        animatorCloseIconVisible.setValue(editText.length() > 0 || closeButtonForcedVisible && !cancelButtonEnabled, true);
        animatorCancelVisible.setValue(cancelButtonEnabled && closeButtonForcedVisible, true);
    }

    /** Keeps the field's end icons inside it as it draws back from the Cancel button. */
    private void updateCancelLayout(float factor) {
        final float direction = LocaleController.isRTL ? 1f : -1f;
        final float shift = direction * getCancelInset();
        closeIcon.setTranslationX(shift);
        additionalIconsLayout.setTranslationX(shift);
        cancelButton.setVisibility(factor > 0f ? VISIBLE : INVISIBLE);
        cancelButton.setAlpha(factor);
        cancelButton.setTranslationX(-direction * dp(12) * (1f - factor));
        checkUi_editTextPaddings();
        invalidate();
    }

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
        if (id == ANIMATOR_ID_CANCEL_VISIBLE) {
            updateCancelLayout(factor);
        } else if (id == ANIMATOR_ID_CLOSE_BUTTON_VISIBLE) {
            FragmentFloatingButton.setAnimatedVisibility(closeIcon, factor);
        } else if (id == ANIMATOR_ID_SEARCH_ICON_VISIBLE) {
            FragmentFloatingButton.setAnimatedVisibility(searchIcon, factor);
        } else if (id == ANIMATOR_ID_SEARCH_FILTERS_WIDTH) {
            checkUi_editTextPaddings();
        }
    }


    public interface SearchFiltersListener {
        void onSearchFilterCleared(FiltersView.MediaFilterData filterData);
        void hideActionMode();
    }

    private final AnimationNotificationsLocker notificationsLocker = new AnimationNotificationsLocker();
    private final ArrayList<FiltersView.MediaFilterData> currentSearchFilters = new ArrayList<>();
    private final LinearLayout searchFilterLayout;
    private SearchFiltersListener searchFiltersListener;
    private int selectedFilterIndex;

    private boolean hasRemovableFilters() {
        if (currentSearchFilters.isEmpty()) {
            return false;
        }
        for (int i = 0; i < currentSearchFilters.size(); i++) {
            if (currentSearchFilters.get(i).removable) {
                return true;
            }
        }
        return false;
    }

    public void setSearchFiltersListener(SearchFiltersListener searchFiltersListener) {
        this.searchFiltersListener = searchFiltersListener;
    }

    public void addSearchFilter(FiltersView.MediaFilterData filter) {
        currentSearchFilters.add(filter);
        if (true /*searchContainer.getTag() != null*/) {
            selectedFilterIndex = currentSearchFilters.size() - 1;
        }
        onFiltersChanged();
    }

    public void removeSearchFilter(FiltersView.MediaFilterData filter) {
        if (!filter.removable) {
            return;
        }
        currentSearchFilters.remove(filter);
        if (selectedFilterIndex < 0 || selectedFilterIndex > currentSearchFilters.size() - 1) {
            selectedFilterIndex = currentSearchFilters.size() - 1;
        }
        onFiltersChanged();
        if (searchFiltersListener != null) {
            searchFiltersListener.hideActionMode();
        }
    }

    public void clearSearchFiltersWithCallback() {
        if (!currentSearchFilters.isEmpty()) {
            if (searchFiltersListener != null) {
                for (int i = 0; i < currentSearchFilters.size(); i++) {
                    if ( currentSearchFilters.get(i).removable) {
                        searchFiltersListener.onSearchFilterCleared(currentSearchFilters.get(i));
                    }
                }
            }
//                clearSearchFilters();
        }
    }

    public void clearSearchFilters() {
        for (int i = 0; i < currentSearchFilters.size(); i++) {
            if (currentSearchFilters.get(i).removable) {
                currentSearchFilters.remove(i);
                i--;
            }
        }
        onFiltersChanged();
    }

    private void onFiltersChanged() {
        final boolean visible = !currentSearchFilters.isEmpty();

        animatorSearchIconVisible.setValue(!visible, true);


        ArrayList<FiltersView.MediaFilterData> localFilters = new ArrayList<>(currentSearchFilters);

        if (true /*searchContainer != null && searchContainer.getTag() != null*/) {
            TransitionSet transition = new TransitionSet();
            ChangeBounds changeBounds = new ChangeBounds();
            changeBounds.setDuration(150);
            transition.addTransition(new Visibility() {
                @Override
                public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
                    if (view instanceof ActionBarMenuItem.SearchFilterView) {
                        AnimatorSet set = new AnimatorSet();
                        set.playTogether(
                                ObjectAnimator.ofFloat(view, View.ALPHA, 0, 1f),
                                ObjectAnimator.ofFloat(view, View.SCALE_X, 0.5f, 1f),
                                ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.5f, 1f)
                        );
                        set.setInterpolator(CubicBezierInterpolator.DEFAULT);
                        return set;
                    }
                    return ObjectAnimator.ofFloat(view, View.ALPHA, 0, 1f);
                }
                @Override
                public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
                    if (view instanceof ActionBarMenuItem.SearchFilterView) {
                        AnimatorSet set = new AnimatorSet();
                        set.playTogether(
                                ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), 0f),
                                ObjectAnimator.ofFloat(view, View.SCALE_X,  view.getScaleX(), 0.5f),
                                ObjectAnimator.ofFloat(view, View.SCALE_Y,  view.getScaleX(), 0.5f)
                        );
                        set.setInterpolator(CubicBezierInterpolator.DEFAULT);
                        return set;
                    }
                    return ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0);
                }
            }.setDuration(150)).addTransition(changeBounds);
            transition.setOrdering(TransitionSet.ORDERING_TOGETHER);
            transition.setInterpolator(CubicBezierInterpolator.EASE_OUT);
            transition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    notificationsLocker.lock();
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    notificationsLocker.unlock();
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    notificationsLocker.unlock();
                }

                @Override
                public void onTransitionPause(Transition transition) {

                }

                @Override
                public void onTransitionResume(Transition transition) {

                }
            });
            TransitionManager.beginDelayedTransition(searchFilterLayout, transition);
        }


        for (int i = 0; i < searchFilterLayout.getChildCount(); i++) {
            boolean removed = localFilters.remove(((ActionBarMenuItem.SearchFilterView) searchFilterLayout.getChildAt(i)).getFilter());
            if (!removed) {
                searchFilterLayout.removeViewAt(i);
                i--;
            }
        }

        for (int i = 0; i < localFilters.size(); i++) {
            FiltersView.MediaFilterData filter = localFilters.get(i);
            ActionBarMenuItem.SearchFilterView searchFilterView;
            if (filter.reaction != null) {
                searchFilterView = new ActionBarMenuItem.ReactionFilterView(getContext(), resourcesProvider, false);
            } else {
                searchFilterView = new ActionBarMenuItem.SearchFilterView(getContext(), resourcesProvider, false);
            }

            searchFilterView.setGlass();
            searchFilterView.setData(filter);
            searchFilterView.setOnClickListener(view -> {
                int index = currentSearchFilters.indexOf(searchFilterView.getFilter());
                if (selectedFilterIndex != index) {
                    selectedFilterIndex = index;
                    onFiltersChanged();
                    return;
                }
                if (searchFilterView.getFilter().removable) {
                    if (!searchFilterView.isSelectedForDelete()) {
                        searchFilterView.setSelectedForDelete(true);
                    } else {
                        FiltersView.MediaFilterData filterToRemove = searchFilterView.getFilter();
                        removeSearchFilter(filterToRemove);


                        if (searchFiltersListener != null) {
                            searchFiltersListener.onSearchFilterCleared(filterToRemove);
                        }
                    }
                }
            });
            searchFilterLayout.addView(searchFilterView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, 0, LocaleController.isRTL ? 6 : 0, 0, LocaleController.isRTL ? 0 : 6, 0));
        }


        for (int i = 0; i < searchFilterLayout.getChildCount(); i++) {
            ((ActionBarMenuItem.SearchFilterView) searchFilterLayout.getChildAt(i)).setExpanded(i == selectedFilterIndex);
        }
        searchFilterLayout.setTag(visible ? 1 : null);
    }
}
