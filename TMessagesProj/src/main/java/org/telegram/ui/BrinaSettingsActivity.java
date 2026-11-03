package org.telegram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.BrinaConfig;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Settings → Brinagram: the app's own options. */
public class BrinaSettingsActivity extends UniversalFragment {

    private static final int ID_LIQUID_GLASS = 1;
    private static final int ID_HIDE_STORIES = 2;
    private static final int ID_NUMBER_ROUNDING = 3;
    private static final int ID_ASK_BEFORE_CALL = 4;
    private static final int ID_HIDE_KEYBOARD = 5;
    private static final int ID_NEXT_CHANNEL = 6;
    private static final int ID_GREETING_STICKER = 7;
    private static final int ID_INSTANT_CAMERA = 8;
    private static final int ID_VOICE_AUTOPLAY = 9;
    private static final int ID_PROXIMITY = 10;
    private static final int ID_PREFER_IPV6 = 11;
    private static final int ID_PLAIN_CHAT_BACKGROUND = 12;
    private static final int ID_SHOW_MESSAGE_TIME = 13;

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.BrinaSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        BrinaConfig.load();

        items.add(UItem.asHeader(getString(R.string.BrinaSettingsAppearance)));
        items.add(UItem.asCheck(ID_LIQUID_GLASS, getString(R.string.BrinaLiquidGlass)).setChecked(LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS)));
        items.add(UItem.asCheck(ID_HIDE_STORIES, getString(R.string.BrinaHideStories)).setChecked(BrinaConfig.hideStories));
        items.add(UItem.asCheck(ID_NUMBER_ROUNDING, getString(R.string.BrinaDisableNumberRounding)).setChecked(BrinaConfig.disableNumberRounding));
        items.add(UItem.asShadow(getString(R.string.BrinaLiquidGlassInfo)));

        items.add(UItem.asHeader(getString(R.string.BrinaSettingsChats)));
        items.add(UItem.asCheck(ID_PLAIN_CHAT_BACKGROUND, getString(R.string.BrinaPlainChatBackground)).setChecked(BrinaConfig.plainChatBackground));
        items.add(UItem.asCheck(ID_SHOW_MESSAGE_TIME, getString(R.string.BrinaShowMessageTime)).setChecked(BrinaConfig.showMessageTime));
        items.add(UItem.asCheck(ID_HIDE_KEYBOARD, getString(R.string.BrinaHideKeyboardOnScroll)).setChecked(BrinaConfig.hideKeyboardOnChatScroll));
        items.add(UItem.asCheck(ID_NEXT_CHANNEL, getString(R.string.BrinaDisableNextChannel)).setChecked(BrinaConfig.disableJumpToNextChannel));
        items.add(UItem.asCheck(ID_GREETING_STICKER, getString(R.string.BrinaDisableGreetingSticker)).setChecked(BrinaConfig.disableGreetingSticker));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.BrinaSettingsMedia)));
        items.add(UItem.asCheck(ID_INSTANT_CAMERA, getString(R.string.BrinaDisableInstantCamera)).setChecked(BrinaConfig.disableInstantCamera));
        items.add(UItem.asCheck(ID_VOICE_AUTOPLAY, getString(R.string.BrinaDisableVoiceAutoplay)).setChecked(BrinaConfig.disableVoiceMessageAutoPlay));
        items.add(UItem.asCheck(ID_PROXIMITY, getString(R.string.BrinaDisableProximity)).setChecked(BrinaConfig.disableProximityEvents));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.BrinaSettingsCalls)));
        items.add(UItem.asCheck(ID_ASK_BEFORE_CALL, getString(R.string.BrinaAskBeforeCall)).setChecked(BrinaConfig.askBeforeCall));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.BrinaSettingsConnection)));
        items.add(UItem.asCheck(ID_PREFER_IPV6, getString(R.string.BrinaPreferIPv6)).setChecked(BrinaConfig.preferIPv6));
        items.add(UItem.asShadow(getString(R.string.BrinaPreferIPv6Info)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_LIQUID_GLASS:
                LiteMode.toggleFlag(LiteMode.FLAG_LIQUID_GLASS, !LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS));
                break;
            case ID_HIDE_STORIES:
                BrinaConfig.toggle("hideStories");
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.storiesEnabledUpdate);
                break;
            case ID_NUMBER_ROUNDING:
                BrinaConfig.toggle("disableNumberRounding");
                break;
            case ID_ASK_BEFORE_CALL:
                BrinaConfig.toggle("askBeforeCall");
                break;
            case ID_PLAIN_CHAT_BACKGROUND:
                BrinaConfig.toggle("plainChatBackground");
                break;
            case ID_SHOW_MESSAGE_TIME:
                BrinaConfig.toggle("showMessageTime");
                break;
            case ID_HIDE_KEYBOARD:
                BrinaConfig.toggle("hideKeyboardOnChatScroll");
                break;
            case ID_NEXT_CHANNEL:
                BrinaConfig.toggle("disableJumpToNextChannel");
                break;
            case ID_GREETING_STICKER:
                BrinaConfig.toggle("disableGreetingSticker");
                break;
            case ID_INSTANT_CAMERA:
                BrinaConfig.toggle("disableInstantCamera");
                break;
            case ID_VOICE_AUTOPLAY:
                BrinaConfig.toggle("disableVoiceMessageAutoPlay");
                break;
            case ID_PROXIMITY:
                BrinaConfig.toggle("disableProximityEvents");
                break;
            case ID_PREFER_IPV6:
                BrinaConfig.toggle("preferIPv6");
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated()) {
                        ConnectionsManager.getInstance(a).checkConnection();
                    }
                }
                break;
            default:
                return;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
