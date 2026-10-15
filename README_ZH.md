# Adaptive Screen++ (ExtDisplayRotator)

**Turn your phone's portable external screen, as well as the Galaxy Z Flip / Z Fold cover screen, into a truly usable second display.**

When the phone is held vertically and the portable screen is placed horizontally, the mirrored view would normally leave a black border. This app monitors the phone's orientation in the background and rotates the external screen 90° when the phone switches to portrait, filling the screen again. It can also **take over the internal screen resolution** (a replacement for SecondScreen).

On top of that, it provides dual‑screen concurrent mode, secondary‑screen desktop with pseudo‑apps, internal‑screen full‑screen gestures, an external‑screen sidebar, and a set of external‑screen widgets.

- **Supported devices**: Samsung Galaxy Z Flip / Z Fold and other foldable phones with a cover screen, as well as any device that can connect an external screen via USB‑C / HDMI.
- **Dependencies**: Features that require system settings (display adjustments, dual‑screen concurrency, hiding the three‑button navigation) rely on [Shizuku](https://shizuku.rikka.app/) (no root needed, works via wireless debugging). The external‑screen launcher, back, and home actions have been reimplemented using pure accessibility services and **do not require Shizuku**.

## Feature Overview

| Category | Features |
|---|---|
| **Display** | Adaptive rotation (external screen rotates together with the phone, eliminating black borders) · Internal‑screen resolution takeover (can replace SecondScreen) · External‑screen resolution / DPI override · Forced rotation · Refresh‑rate settings · Remove app black borders |
| **Dual‑Screen** | Fold / unfold state switching · Simultaneous illumination of internal and external screens · One‑tap dual‑screen activation · Screen‑casting control (mirror internal screen to external screen, supports fullscreen) |
| **External‑Screen** | Secondary‑screen desktop · Sidebar (notification panel + recent tasks) · External‑screen launcher (search / favorites / drag‑reorder / appearance / background image) |
| **Pseudo‑Apps** | Secondary‑screen touchpad · Screen‑casting control · Status display cabinet · Secondary‑screen music player (controls currently playing music) |
| **Internal‑Screen Gestures** | Edge swipe back · Bottom swipe to home · Swipe up and hold to open recent apps (animation can be native blur or colorful light trail) |
| **External‑Screen Widgets** | Dual‑screen quick panel · External‑screen launcher · Slot machine · Custom actions · Phone status |
| **System & Efficiency** | Hide three‑button navigation · Volume / side‑key shortcuts · Custom commands · Open wireless debugging · Beta laboratory |

## Installation

1. Install [Shizuku](https://shizuku.rikka.app/) and start its service (no root, two options):
   - **Without a computer**: Enable *Wireless debugging* in Developer options, then in Shizuku choose *Start via wireless debugging* and follow the pairing instructions (Android 11+).
   - **With a computer**: Connect via USB and run `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`.
2. Download the APK from the [Releases](https://github.com/PyMakesMeProud/AdaptiveScreenPlus/releases) page and install it on your phone.
3. Open the app, tap **"Authorize / Re‑detect"**, grant the permissions in the dialog, then follow the on‑screen prompts to grant all required permissions.

> If you previously used **SecondScreen** to adjust the internal resolution, disable or stop it after installing this app. Running both apps simultaneously will cause display conflicts.

## Getting Started: First Three Steps

### 1. Enable **Adaptive Rotation** – understand its use cases
- **What it does**: The external screen rotates together with the phone, keeping both sides aligned. No black borders after rotation.
- **When to enable**: When the external screen moves together with the phone (e.g., portable screen). For a fixed desktop monitor, leave it off because the phone’s rotation would misalign the display.
- **Note**: It has no effect when no external screen is attached.

### 2. Calibrate Orientation (required once)
Different portable screens have opposite effects for *Rotate 90°* and *Rotate 270°*. Use the **Manual Calibration** button on the *Auto‑rotate* page, tap each option once, see which fills the screen correctly, then set the proper angle in the **"When phone is portrait, lock external screen to"** dropdown.

### 3. (Optional) Take Over Internal‑Screen Resolution
1. Tap **"Grant overlay permission"** (the app works without it, but the overlay improves the experience during screen insertion).
2. Tap the large **"Internal‑screen resolution takeover"** button to enable it; the screen switches immediately to the current resolution. Tap again to restore the native resolution.
3. Keep the default resolution `1080×1920`. With **"Auto‑load resolution"** enabled, the app will automatically calculate the correct value when a portable screen is connected (e.g., a 1920×1080 screen results in `1080×1920`).
4. After plugging a portable screen, you’ll get a popup to **Enable** the takeover; unplugging restores the native resolution automatically.

## Common Operations

### Dual‑Screen Mode & Secondary‑Screen Desktop
Tap the green **"Activate Dual‑Screen Mode"** button on the *Dual‑screen* page: it simultaneously lights up both screens and projects the secondary‑screen desktop onto the external screen. The external desktop shows a clock, battery indicator, and app icons; tapping an icon launches the app on that screen.

To make the phone + portable screen behave like a true dual‑screen (similar to DeX but without DeX), enable **"DeX‑free mode"** on the *Dual‑screen* page (requires a reboot).

### Internal‑Screen Gestures
The internal screen has three invisible edge zones that mimic the system’s full‑screen gestures:

| Position | Action |
|---|---|
| Left / right edges (≈20dp each) | Swipe inward ≥34dp then release = Back |
| Bottom edge (≈20dp) | Swipe up = Home |
| Bottom edge (≈20dp) | Swipe up, pause ~140ms, then release = Open recent apps |

Animations can be switched between native blur or a colorful light trail in the Beta lab.

### External‑Screen Sidebar & Widgets
- **Sidebar**: Swipe from the edge on the external screen to pull down the notification panel (quick toggles + notifications) and recent tasks.
- **Widgets** (Dual‑screen quick panel, external launcher, slot machine, custom actions, phone status): Adding a widget to the *home screen* places it on the internal screen; the external screen has its own set of widgets, added via **Settings → External screen → Widgets**.

## FAQ

**Q: Shizuku shows "Not ready" even after authorisation?**
Check that the Shizuku service is running (it may need to be restarted after a phone reboot), then tap **"Authorize / Re‑detect"** again.

**Q: Features stop working after a phone reboot?**
Shizuku must be restarted, then re‑authorize in the app.

**Q: Rotation 90° looks odd?**
Your screen’s 90°/270° orientation may be reversed. Use the calibration step to set it to 270°.

**Q: No popup after connecting a portable screen?**
1. Ensure the phone is not locked or sleeping (the popup is suppressed on lock screen).\
2. Verify the **"Display over other apps"** permission is granted (otherwise you’ll only see a notification bar).\
3. Use the **"Test popup"** button in the app to verify the pipeline.

**Q: Does this affect the phone’s internal screen?**
Rotation only affects the external screen. Resolution changes are explicit toggles with confirmation popups; you can always restore the native settings via the **"Restore native"** button.

**Q: Can I keep SecondScreen installed?**
Yes, but **do not enable both apps’ automatic modes simultaneously**, as they both modify the same display parameters and will conflict. Use either this app or SecondScreen, or disable SecondScreen’s auto‑config and manually trigger it.

## Building from Source

Standard Gradle Android project.
Environment: **JDK 17**, **Android SDK** (platform 34 + build‑tools 34.0.0).

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Open the directory with Android Studio if you prefer.

> Shizuku client libraries come from Maven Central (`dev.rikka.shizuku:api` / `:provider`). If dependencies fail to resolve, replace `mavenCentral()` with `https://maven.aliyun.com/repository/public` in `settings.gradle`.

## License

This project is released under the **MIT License**; see [LICENSE](LICENSE). Change log is available in [CHANGELOG.md](CHANGELOG.md).
