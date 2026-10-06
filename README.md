# FawaNews Android

Official-style client for [fawanews.sc](http://www.fawanews.sc/) — live sports schedule, HLS playback (ExoPlayer), and sports news links.

## Builds

| Variant | Command | Install target |
|---------|---------|----------------|
| Phone | `cd android && .\gradlew assembleMobileDebug` | Phone / emulator |
| Android TV | `cd android && .\gradlew assembleTvDebug` | TV / TV emulator |

Open the `android/` folder in Android Studio for Run configurations (`mobileDebug`, `tvDebug`).

## How it works

1. Loads the site homepage and parses event cards (same HTML as the website).
2. Live rows open the event page and reads `var videos = ["…m3u8"]` for the stream URL.
3. Playback uses **Media3 ExoPlayer** (native HLS), not a WebView player.
4. **TV** build uses a leanback launcher, focusable grid cards, and D-pad friendly player (Back exits).

## Notes

- Streams may be **HTTP** (cleartext allowed in manifest). UK/Canada users may need VPN — same as the website.
- News items without a stream open the article in the browser.
- This repo is not affiliated with FawaNews unless they endorse it; the app shows their official domain notice.
