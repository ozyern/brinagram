package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Brinagram's own settings (Settings → Brinagram).
 *
 * <p>Stored in the "brinaconfig" preferences. Each field is read directly by the Telegram code it
 * changes; see the call sites for what each one hooks.
 */
public final class BrinaConfig {

    private static final String PREFS = "brinaconfig";
    private static final Object lock = new Object();
    private static volatile boolean loaded;

    // Chats
    /** ChatActivity: dismiss the keyboard when the message list is dragged. */
    public static boolean hideKeyboardOnChatScroll;
    /** ChatActivity: no "swipe up for the next unread channel" at the end of a channel. */
    public static boolean disableJumpToNextChannel;
    /** ChatGreetingsView: no sticker suggestion in empty private chats. */
    public static boolean disableGreetingSticker;
    /** ChatActivityEnterView: the record button only records voice, never round videos. */
    public static boolean disableInstantCamera;
    /** MediaController: do not keep playing the next unread voice message. */
    public static boolean disableVoiceMessageAutoPlay;
    /** MediaController: ignore the proximity sensor (no switching to the earpiece). */
    public static boolean disableProximityEvents;

    // General
    /** VoIPHelper: confirm before starting a call. */
    public static boolean askBeforeCall = true;
    /** DialogsActivity: hide the stories row above the chat list. */
    public static boolean hideStories;
    /** LocaleController: show 4777 instead of 4.8K. */
    public static boolean disableNumberRounding;

    // Connection
    /** ConnectionsManager: connect over IPv6 whenever the network has it. */
    public static boolean preferIPv6;

    private BrinaConfig() {}

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void load() {
        if (loaded) {
            return;
        }
        synchronized (lock) {
            if (loaded || ApplicationLoader.applicationContext == null) {
                return;
            }
            final SharedPreferences p = prefs();
            hideKeyboardOnChatScroll = p.getBoolean("hideKeyboardOnChatScroll", false);
            disableJumpToNextChannel = p.getBoolean("disableJumpToNextChannel", false);
            disableGreetingSticker = p.getBoolean("disableGreetingSticker", false);
            disableInstantCamera = p.getBoolean("disableInstantCamera", false);
            disableVoiceMessageAutoPlay = p.getBoolean("disableVoiceMessageAutoPlay", false);
            disableProximityEvents = p.getBoolean("disableProximityEvents", false);
            askBeforeCall = p.getBoolean("askBeforeCall", true);
            hideStories = p.getBoolean("hideStories", false);
            disableNumberRounding = p.getBoolean("disableNumberRounding", false);
            preferIPv6 = p.getBoolean("preferIPv6", false);
            loaded = true;
        }
    }

    /** Flips a boolean setting by key and returns the new value. */
    public static boolean toggle(String key) {
        load();
        final boolean value;
        switch (key) {
            case "hideKeyboardOnChatScroll": value = hideKeyboardOnChatScroll = !hideKeyboardOnChatScroll; break;
            case "disableJumpToNextChannel": value = disableJumpToNextChannel = !disableJumpToNextChannel; break;
            case "disableGreetingSticker": value = disableGreetingSticker = !disableGreetingSticker; break;
            case "disableInstantCamera": value = disableInstantCamera = !disableInstantCamera; break;
            case "disableVoiceMessageAutoPlay": value = disableVoiceMessageAutoPlay = !disableVoiceMessageAutoPlay; break;
            case "disableProximityEvents": value = disableProximityEvents = !disableProximityEvents; break;
            case "askBeforeCall": value = askBeforeCall = !askBeforeCall; break;
            case "hideStories": value = hideStories = !hideStories; break;
            case "disableNumberRounding": value = disableNumberRounding = !disableNumberRounding; break;
            case "preferIPv6": value = preferIPv6 = !preferIPv6; break;
            default: throw new IllegalArgumentException("Unknown setting " + key);
        }
        prefs().edit().putBoolean(key, value).apply();
        return value;
    }

    public static boolean get(String key) {
        load();
        switch (key) {
            case "hideKeyboardOnChatScroll": return hideKeyboardOnChatScroll;
            case "disableJumpToNextChannel": return disableJumpToNextChannel;
            case "disableGreetingSticker": return disableGreetingSticker;
            case "disableInstantCamera": return disableInstantCamera;
            case "disableVoiceMessageAutoPlay": return disableVoiceMessageAutoPlay;
            case "disableProximityEvents": return disableProximityEvents;
            case "askBeforeCall": return askBeforeCall;
            case "hideStories": return hideStories;
            case "disableNumberRounding": return disableNumberRounding;
            case "preferIPv6": return preferIPv6;
            default: throw new IllegalArgumentException("Unknown setting " + key);
        }
    }
}
