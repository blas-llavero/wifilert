# Real-device acceptance checks (not yet run)

- [ ] Build a debug APK through GitHub Actions or a local Android SDK. Confirm Android installation.
- [ ] Android 10, 13, 14, 15+: no buttons hidden under status/navigation bars; test gesture and three-button navigation, portrait and landscape, and enlarged fonts.
- [ ] Enable notifications; press the notification test button; confirm a loud/visible notification on the phone and mirrored vibration on a paired Fitbit Charge 6.
- [ ] Start **Always monitor** while connected to Wi-Fi; disable Wi-Fi; confirm Wi-Fi disconnected and restored events. With mobile data on, internet may never be lost.
- [ ] Turn off mobile data and Wi-Fi; confirm Internet lost; turn internet back on; confirm Internet restored.
- [ ] Remain connected to Wi-Fi but block its internet path (e.g. router WAN); confirm Wi-Fi still connected while internet validation changes; note that Android can take time to detect this.
- [ ] Start **Train mode (manual)**, lock the screen and travel through a tunnel; verify notifications locally on the phone and watch.
- [ ] Start **Train mode (automatic)** after consenting to precise GPS location; check railway proximity detection, false positives near roads, and stable status through GPS loss in a tunnel.
- [ ] Check denied location permissions, missing GPS, Overpass server outage and offline starts; app must never falsely assert automatic train detection.
- [ ] Test Arabic right-to-left display and all 10 resource locales. Confirm TalkBack reads buttons and status clearly.
- [ ] Test background restrictions/Do Not Disturb on Oppo, Xiaomi, Samsung, and Google Pixel devices.
- [ ] Confirm airplane mode, restart, Android service recovery, and repeated start/stop do not crash the app.
- [ ] Before release, review app license, trademark searches, and all media one by one. No media is approved at this time.
