# Brinagram

**Your messages, short n' sweet.**

A liquid glass messenger for Android, made with love for Sabrina Carpenter.

Brinagram is an unofficial fan project. It is not affiliated with or endorsed by Sabrina Carpenter or Telegram.

## What's different

- **Liquid glass everywhere:** selections swell into a glass lens as they move, with bent light, colour fringes and a little push on the track. You'll see it on profile, folder and attach tabs, and on the dock.
- **Popups that follow your finger:** one pill slides between menu rows, and you can slide and release to pick an item. Menus spring out of the corner they were opened from.
- **A cleaner chat:** a round plus beside a glass message field, a larger avatar over a name pill, curved bubble tails, and a plain background you can turn off.
- **Rebuilt Settings:** a large title, a search field, a profile card and grouped rows with coloured icons.
- **Profiles tinted by the photo:** the page takes its colour from the person's picture, with a large avatar and round glass buttons.
- **A nicer sign-in:** a grouped phone number card, a country picker with its search bar at the bottom, and a glass number pad.
- **Pages that slide:** screens glide fully across, with a slower page behind.
- **Brinagram settings:** your own switches for stories, keyboard behaviour, instant camera, call confirmation and more, under Settings → Brinagram.

## Download

Signed APKs for tagged versions are on the [Releases](https://github.com/ozyern/BrinaGram/releases) page. Every push also builds an APK, attached to its run on the [Actions](https://github.com/ozyern/BrinaGram/actions) page.

Requires Android 6.0 or newer.

## Building

The easiest way is GitHub Actions: `.github/workflows/release.yml` builds a signed APK on every push. It needs these repository secrets:

| Secret | What it is |
|---|---|
| `TELEGRAM_APP_ID`, `TELEGRAM_APP_HASH` | Your own API credentials from [my.telegram.org](https://core.telegram.org/api/obtaining_api_id) |
| `BRINA_KEYSTORE_BASE64`, `BRINA_KEYSTORE_PASSWORD`, `BRINA_KEY_ALIAS`, `BRINA_KEY_PASSWORD` | Your signing key; `bash Tools/ci/setup_signing.sh` creates and uploads these |

To build by hand you need JDK 17, Android SDK 36, NDK 27.2.12479018 and CMake 3.22.1:

```bash
git clone --recursive --shallow-submodules https://github.com/ozyern/BrinaGram.git brinagram
cd brinagram
./gradlew :TMessagesProj_App:assembleAfatRelease -PbrinaAbis=arm64-v8a,armeabi-v7a
```

The same secrets are read from environment variables of the same names, except `BRINA_KEYSTORE_BASE64`: locally, point `BRINA_KEYSTORE_FILE` at your keystore.

## Credits and licence

Brinagram is based on [Telegram for Android](https://github.com/DrKLO/Telegram) and is licensed under the GNU GPL v2 or later, like the code it comes from. See [LICENSE](LICENSE) and [NOTICE](NOTICE). The source for every build is published here, as that licence requires.

Translations come from Telegram's shared language packs, so text specific to Brinagram is in English for now.
