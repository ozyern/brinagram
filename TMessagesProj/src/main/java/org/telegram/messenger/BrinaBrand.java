package org.telegram.messenger;

import java.util.regex.Pattern;

/**
 * Shows the app as "Brinagram" everywhere in the UI.
 *
 * <p>Telegram downloads its UI strings from the server at runtime, so editing the bundled
 * strings.xml alone is not enough; every string lookup passes through {@link #apply}.
 * Only the capitalized product word is replaced: links such as {@code telegram.org} and
 * {@code t.me} are lowercase and stay intact.
 */
public final class BrinaBrand {

    public static final String APP_NAME = "Brinagram";

    private static final Pattern WORD = Pattern.compile("\bTelegram\b");
    private static final Pattern WORD_UPPER = Pattern.compile("\bTELEGRAM\b");

    private BrinaBrand() {}

    public static String apply(String value) {
        if (value == null) {
            return null;
        }
        if (value.indexOf("Telegram") >= 0) {
            value = WORD.matcher(value).replaceAll(APP_NAME);
        }
        if (value.indexOf("TELEGRAM") >= 0) {
            value = WORD_UPPER.matcher(value).replaceAll("BRINAGRAM");
        }
        return value;
    }
}
