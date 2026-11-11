# Media release and accessibility policy

**Only the WIFILERT logo approved by the project owner on 2026-11-11 and its resized launcher icons may be published.** All other video, image, and audio assets remain blocked until separate owner review. The GitHub Actions workflow checks exact filenames and SHA-256 fingerprints of approved media.

## Proposed video production (not started)

1. Verify the name and check trademark conflicts before creating branded content.
2. Prepare one **English** storyboard of **at most 30 seconds**: user on a train → enters a tunnel → Wi-Fi/internet disconnected → notification on phone **and** mirrored to a watch → train exits tunnel → connection restored → both devices receive the recovery notification.
3. Create a reviewable English draft **outside GitHub**. Demonstration notifications must reflect actual app behavior, not fake watch pairing functionality.
4. Choose a voice synthesis engine and multilingual model weights whose licensing is actually open and compatible with redistribution and use. Natural English speech, and distinct natural voices for all other languages, are required. Verify each voice/model license; a permissive code license alone is insufficient.
5. Provide **accurate captions** and optional audio descriptions. No music without a compatible license; avoid third-party copyrighted visuals.
6. Only after the owner approves the English draft, make nine localized versions for Spanish, Catalan, French, German, Brazilian Portuguese, Simplified Chinese, Hindi, Arabic, and Japanese, matching the on-device locale with English fallback.
7. Review **every** localized artifact and its license before allowing the final app package or GitHub commit to contain it.

## Planned in-app playback

Localized video selection via `Locale.getDefault()` / Android resources. If a locale's video is absent, use English. Audio narration and captions must be accessible independently of the device's screen reader. Video assets have **not** been included or downloaded at this stage.
