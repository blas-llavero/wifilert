# WIFILERT — Wi-Fi + Alert

**Open-source Android Wi-Fi and internet outage/recovery alerts for phones and compatible smartwatches, useful on trains, in hospital basements, tunnels, and other low-connectivity places.**

> **Name:** WIFILERT was explicitly approved by the project owner on 2026-11-11 as the app name. The owner reports no exact-name results in WIPO, and a screenshot of a TMview “Contains wifilert” query showed “No rows found.” These checks do not constitute legal trademark clearance or verify domain availability; similar marks still need review. One specific app logo was approved on 2026-11-11 and is included; **all other** videos, images, voice recordings, and sound files remain unapproved and excluded.

> **Build status:** This repository contains a source-code prototype and GitHub Actions build workflow. An APK is **not included** until the workflow builds successfully. This version has not yet been tested on a real Android phone or watch.

## Download and install the Android APK

**APK location after a successful GitHub Actions build:**

1. Open the repository on GitHub.
2. Go to **Actions → Android Debug APK**.
3. Open the most recent **successful** run.
4. Under **Artifacts**, download **`wifilert-debug-apk`**.
5. Unzip the downloaded file to find **`app-debug.apk`**.

**Install it on Android:**

1. Transfer `app-debug.apk` to your Android phone, or download the artifact with the phone.
2. Open your Android **Files / Downloads** app and tap `app-debug.apk`.
3. If Android asks, allow **Install unknown apps** for your file manager or browser; then confirm the installation. Disable this permission afterward if you wish.
4. Open **WIFILERT** from the app drawer.
5. Allow **notifications** when asked. In automatic-train mode, also allow **precise location while using the app**, and turn on the phone's Location service.
6. Choose a mode and tap **Start monitoring**. Keep the ongoing monitoring notification visible.
7. Use **Send test notification** to verify Android and watch alerts.

**How to use:** choose **Always monitor** to test anywhere, **Train mode (manual)** before boarding for reliable tunnel alerts, or **Train mode (automatic · experimental)** to alert only after GPS motion and nearby railway tracks suggest the phone is on a train. Toggle Wi-Fi and/or internet alerts before starting. Tap **Stop monitoring** to finish. No login is needed.

### Fitbit Charge 6 notifications (Google Health / Fitbit app)

To receive **WIFILERT notifications on your Fitbit Charge 6**, first make sure Android notifications for WIFILERT are enabled on the phone and that the watch is paired and connected over Bluetooth. Then open the **Fitbit app (Google Health ecosystem)**, select the connected Fitbit Charge 6 and open **Notifications**:

1. Under **General**, leave **Mirror your phone** **OFF**, as shown in the tested configuration. This option mirrors all supported phone notifications and is not required when WIFILERT is selected individually.
2. Open **App notifications** and switch **WIFILERT ON** (blue toggle). Other applications may remain OFF; you do not need to enable all apps.
3. Return to **Notifications**, scroll down to **Vibration**, and switch **Always vibrate ON** (blue toggle). The option is described as sending notifications and vibrating the watch even when the phone is in Do Not Disturb or silent mode. Actual watch behavior can still depend on its settings and connection.
4. **Calls**, **Text messages**, **Calendar events**, **Emails**, **Quick replies**, and **Use transliteration** are optional and are not required to enable WIFILERT alerts.
5. Open **WIFILERT → Send test notification** and check whether the notification appears on the phone **and** the watch. If it only appears on the phone, check Android's WIFILERT notification permission, the Fitbit app's notification-access permission, Bluetooth connectivity, watch notification/silent settings, and battery restrictions on the Fitbit app.

**Important distinction:** These steps configure **watch notifications forwarded from the phone**, not the appearance of notifications on the phone itself. WIFILERT posts standard Android notifications locally; Fitbit/Google's companion software decides whether to deliver them to the watch. Watch delivery is not guaranteed. The UI wording can change with Fitbit app updates.



**If Android stops monitoring:** under Android **Settings → Apps → WIFILERT → Battery**, permit unrestricted/background usage as supported by your phone. Manufacturer-specific battery optimization and Do Not Disturb can still delay or silence alerts. If you close the monitoring service, reopen the app and start it again.

## Approved WIFILERT app logo

![WIFILERT official approved app logo](docs/media/wifilert_logo_approved.png)

The **exact original logo approved by the project owner on 2026-11-11** is stored at `docs/media/wifilert_logo_approved.png` (1254 × 1254 pixels). Android launcher icon files are high-quality proportional resizes under `app/src/main/res/mipmap-*/ic_launcher.png`; the app manifest uses `@mipmap/ic_launcher`. These approved files are the **only media permitted** in the repository. GitHub Actions verifies the file list and the SHA-256 hash of each approved image before building. Keep all other images, audio, and videos out of the repository pending review.

The design is optimized here as a legacy launcher icon that shows the full approved composition. Some Android launchers may apply an additional icon mask or scaling. The app is a prototype and must be tested on actual Android devices.

## First-time GitHub repository setup

1. Sign in at [GitHub → New repository](https://github.com/new).
2. Owner: your GitHub account. Name: **`wifilert`**. Choose **Public** and tick **Add a README file**, then press **Create repository**.
3. Let the GitHub-connected assistant commit the extracted source files to the new repository, or use git on a computer. Keep the workflow path **`.github/workflows/android-apk.yml`** exactly intact.
4. The first commit to `main` runs the **Android Debug APK** workflow automatically. If not, open **Actions → Android Debug APK → Run workflow**.
5. Return to **Download and install the Android APK** at the top of this document once a build has succeeded.

Do **not** commit the ZIP as a single GitHub file; the repository needs the actual source tree. Only the approved WIFILERT logo and its derived launcher sizes may be committed. The workflow rejects all other image, video, and audio assets. Keep all media outside GitHub until the owner approves it.

## Features

- **Differentiate two events:** Wi-Fi association disconnects/reconnects versus actual **internet availability** (Android's `NET_CAPABILITY_VALIDATED`). If mobile data takes over after Wi-Fi loss, the Wi-Fi alert can fire even though mobile internet remains available.
- **Wi-Fi without internet** alert when association remains but Android loses internet validation.
- **Phone and compatible-watch notifications**, with a high-importance notification channel and vibration.
- **Background foreground service**, explicitly started/stopped by the user, with an ongoing notification; events settle for about **1.6 seconds** to reduce brief false alarms.
- **Train mode, manual:** arm monitoring just before boarding; no location is shared.
- **Train mode, experimental automatic:** uses device GPS motion and rail lines downloaded from the OpenStreetMap/Overpass public service. Two movement observations near mapped tracks are needed. Train state is retained briefly through GPS gaps and tunnels. This is a heuristic, **not proof of being on a train**, and can be incorrect or delayed.
- **10 user-interface languages, following the Android device language:** English (`en`, fallback), Spanish (`es`), Catalan (`ca`), French (`fr`), German (`de`), Brazilian Portuguese (`pt-BR`), Simplified Chinese (`zh-CN`), Hindi (`hi`), Arabic (`ar`), and Japanese (`ja`). Arabic uses native RTL support.
- **Accessibility:** Android-native controls, scalable text, and screen-reader-readable labels. Spoken-video narration is planned but **not included** before review.
- **Navigation bar insets:** content stays clear of the status bar, gesture bar, and Android's three-button navigation area, including Android 15 edge-to-edge layouts.
- **Open source:** Android framework APIs, no paid API, no ad SDK, no analytics SDK, no closed-source TTS runtime bundled. An optional public, community-hosted Overpass server is used only in experimental automatic train mode.

## Privacy and limitations

- In Always and Manual modes, connection monitoring is local and does not contact external servers.
- Experimental Automatic mode requires location permission and queries a public Overpass server for railway geometry around an **approximately rounded GPS position**. Its operator may see the request and IP address. No location history is saved by this app. The data is © OpenStreetMap contributors and used under ODbL.
- The Overpass instance can rate-limit, become unavailable, or lack railway data. When this happens, automatic train detection may not work, even though connection monitoring continues. Use Manual mode if you need dependable alerts.
- Android's internet validation is **not a continuous independent ping**; it can lag behind a real outage or miss a blocked specific website. If the entire internet connection fails, notifications are generated **locally**; they do not require an online push service.
- When a Wi-Fi network disconnects, a separate cellular connection may keep internet online. Wi-Fi and internet notifications are intentionally distinct.
- **Current prototype scope:** separate monitoring of 5G signal strength or 6G radio coverage is **not implemented**. 6G support, when feasible, would require compatible future devices and Android APIs. The existing internet alert responds to Android validation rather than measuring cellular radio reception.
- If Android stops the app/service, monitoring may stop. Power policies vary by manufacturer. The app is not a safety system.
- On service recovery, Android may disallow location access until the app is opened again; Automatic mode is then suspended until restarted from a visible screen.
- Because this prototype is not device-tested, watch mirroring, auto train detection, battery handling, and build success must be verified before publishing.

## Video and media approval gate

**Do not commit additional media until the owner has personally reviewed and approved every file.** The selected WIFILERT logo is already approved. `.gitignore` blocks video (`.mp4`, `.webm`, `.mov`, `.gif`), audio (`.wav`, `.mp3`, `.ogg`, `.flac`, `.aac`, `.m4a`) and image (`.png`, `.jpg`, `.jpeg`, `.webp`, `.svg`) file types by default. The original approved logo and its launcher-sized versions are explicitly allowlisted; future releases must change this rule *only for individually approved media*.

Planned video requirement, subject to separate media approval: ten videos, **30 seconds maximum each**, in the ten app languages. The preferred language matches the Android device locale and falls back to English. The main demonstration shows a train approaching a tunnel, losing Wi-Fi/internet with **both phone and watch notifications**, and receiving recovery alerts after leaving the tunnel. Provide accessible subtitles and high-quality **open-source voice generation with verified commercial-use permissions for both code and model weights**. No audio, narration, or video assets have been included; only the individually approved application logo is included. See `docs/MEDIA_APPROVAL.md`.

## Project structure

- `app/src/main/java/`: Java sources (`MainActivity`, `ConnectionService`, `RailwayDetector`, `Notifier`).
- `app/src/main/res/`: Android layout, theme, and ten localized string resources.
- `.github/workflows/android-apk.yml`: debug APK automation with approved-media SHA-256 checks.
- `docs/`: naming review, media gate, and real-device acceptance tests.

## Attribution and license

**Conceived and programmed by Blas Llavero** (initial source prototype). Licensed under MIT (see `LICENSE`). Railway data © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), ODbL. Overpass is an independent public service and is not guaranteed to be available.

## Build from source

This section is intentionally placed at the **very end** of the README.

Prerequisites: Java 17, Android SDK platform 35 and build-tools 35.0.0, **Gradle 8.11.1**, internet access to download the Android Gradle Plugin **8.9.1**. An Android Studio install can provide the SDK and Java environment. There is deliberately no binary Gradle wrapper or SDK in the source ZIP.

```bash
# From the Wifilert project directory:
gradle --no-daemon :app:assembleDebug
```

APK output: **`app/build/outputs/apk/debug/app-debug.apk`**.

With Android Debug Bridge (ADB) and USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

You can instead publish the source to a GitHub repository and use **Actions → Android Debug APK → Run workflow**; the workflow installs Gradle and the Android SDK and uploads a debug APK artifact. Before public commercial distribution, review similar trademarks, relevant jurisdictions, and domain availability.
