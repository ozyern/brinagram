package org.telegram.ui.Components.glass;

import org.telegram.ui.ActionBar.Theme;

/**
 * Colors for grouped screens (Settings, Profile): on dark themes the backdrop is pure black and
 * the groups are neutral dark cards, whatever tint the theme itself has. Light themes pass through.
 */
public final class BlackGroupedResources implements Theme.ResourcesProvider {

    public static final BlackGroupedResources INSTANCE = new BlackGroupedResources();

    private static final int BACKDROP = 0xFF000000;
    private static final int CARD = 0xFF1C1C1E;

    private BlackGroupedResources() {}

    @Override
    public int getColor(int key) {
        if (Theme.isCurrentThemeDark()) {
            if (key == Theme.key_windowBackgroundGray || key == Theme.key_actionBarDefault) {
                return BACKDROP;
            }
            if (key == Theme.key_windowBackgroundWhite) {
                return CARD;
            }
        }
        return Theme.getColor(key);
    }
}
