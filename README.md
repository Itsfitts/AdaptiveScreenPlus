# Adaptive Screen++ (ExtDisplayRotator)

**Transform portable screens connected to your phone, as well as the cover screens of Galaxy Z Flip / Z Fold, into truly functional secondary displays.**

When holding your phone vertically and placing a portable screen horizontally, mirroring causes black borders all around. This application runs in the background monitoring the phone's orientation: as soon as it turns portrait, the external screen rotates 90° along with it to fill the display; it can also **take over inner screen resolution** (as a replacement for SecondScreen). On top of this, it provides dual-screen concurrency, secondary launcher & pseudo-apps, inner screen full-screen gestures, cover screen sidebar, and a set of cover screen widgets.

- **Supported Devices**: Foldable phones with cover + main screens like Samsung Galaxy Z Flip / Z Fold, and devices supporting USB-C / HDMI external displays.
- **Dependencies**: Features that write system settings (such as display adjustment, dual-screen concurrency, and hiding the 3-button navigation bar) require [Shizuku](https://shizuku.rikka.app/) (rootless, can be started via wireless debugging without plugging into a PC). The cover screen launcher, back, and home gesture features have been refactored to pure accessibility implementations and **do not require Shizuku**.

## Feature Overview

| Category | Feature |
| --- | --- |
| **Display** | Adaptive Rotation (external screen rotates with phone to eliminate black borders) · Inner Screen Resolution Override (replacement for SecondScreen) · Outer Screen Resolution / DPI Override · Force Rotation · Refresh Rate Setting · Remove App Cutout Borders |
| **Dual Screen** | Fold / Unfold State Switch · Inner & Cover Screen Dual-light · One-key Dual-Screen Start · Screen Cast Control (mirrors inner screen to external screen with full-screen support) |
| **Cover Screen** | Secondary Launcher · Sidebar (Notification Panel + Recents) · Cover Launcher (Search / Favorites / Drag & Drop Sort / Appearance / Background Image) |
| **Pseudo Apps** | Cover Touchpad · Screen Cast Control · Status Board · Cover Media Player (takes over currently playing system media) |
| **Inner Gestures** | Edge Slide Back · Bottom Swipe Home · Bottom Swipe & Hold Recents; animation choice of native blur or rainbow light strip |
| **Cover Widgets** | Dual-Screen Quick Panel · Cover Launcher · Slot Machine · Custom Action · Phone Status |
| **System & Efficiency** | Hide 3-Button Navigation Bar · Volume / Side Key Quick Actions · Custom Shell Commands · Open Wireless Debugging · Beta Lab · Online Update (check for new versions and one-click install) |

## Installation

1. Install [Shizuku](https://shizuku.rikka.app/) and start its service (rootless, pick either method):
   - **Without PC**: Turn on "Wireless debugging" in Developer options, select "Start via wireless debugging" in Shizuku, and follow prompts to pair (Android 11+);
   - **With PC**: Connect via USB and run `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`.
2. Download the APK for this app from [Releases](https://github.com/PyMakesMeProud/AdaptiveScreenPlus/releases) and install it on your phone.
3. Open the app, click **"Grant / Re-detect"**, and allow permission in the dialog; then follow the home page prompts to grant all permissions — if permissions are incomplete, some features will not appear.

> If you previously used **SecondScreen** to change inner screen resolution, please turn it off (or at least disable its auto-rules) after installing this app to prevent two apps from modifying display parameters simultaneously and causing misalignments.

## Quick Start: First 3 Steps

### 1. Enable "Adaptive Rotation" — Note Its Use Cases

- **What it does**: The external screen image rotates together with the phone, keeping both orientations aligned without leaving black borders after rotation.
- **When to enable**: When the external display and phone change orientation together (e.g. handheld portable screens).
- **When to disable**: When the external screen is fixed (like a desktop monitor) while the phone rotates independently — enabling it will cause the screen image to tilt with the phone. It is inactive when no external display is attached.

### 2. Calibrate Direction (Required for First Use)

For different portable monitors, turning 90° vs 270° can produce opposite orientations. Use the **Manual Calibration** buttons at the bottom of the "Auto Rotation" page to test each, see which fills the screen correctly, and enter that angle into the **"When phone is portrait, lock external screen to"** dropdown.

### 3. Override Inner Screen Resolution as Needed (Optional, SecondScreen Alternative)

1. Click **"Grant Overlay Permission"** (optional; without it, popup falls back to notification banner);
2. Tap the large button **"Inner Screen Resolution Override"** to enable it; it will immediately switch the inner screen to the configured resolution. Tap again to disable and restore native resolution;
3. Keep the default resolution `1080×1920` — **no need to manually look up the portable screen's pixel resolution**, keep **"Auto Load Resolution"** turned on, and it will automatically calculate it upon connection (e.g. 1920×1080 automatically uses 1080×1920). Entering landscape values is fine as well, as they will be automatically converted to portrait before writing;
4. Every time you plug in the portable monitor afterwards → prompt dialog pops up → tap "Enable"; when unplugged, native resolution is automatically restored.

## Usage Guide

### Dual-Screen Mode & Secondary Launcher

Tap the big green **"Start Dual-Screen Mode"** button on the "Dual Screen" page to switch to dual-screen concurrency in one step and project the secondary launcher to the external display. Once lit, the external screen acts as an interactive desktop (clock / battery / app icon grid); **tapping an icon on it opens the app on that screen**.

To make "Phone + Portable Monitor" function as true dual screens (like DeX, but without DeX), enable **"Non-DeX Desktop Mode"** on the "Dual Screen" page (requires restart) — the external screen becomes an independent desktop with its own mouse pointer while the phone screen operates via touch as usual.

### Inner Screen Gestures

Three invisible hot zones exist on the edges of the inner screen, functioning identically to system full-screen gestures:

| Position | Action |
| --- | --- |
| Left / Right Edges (20dp each) | Slide inward ≥34dp then release = Back |
| Bottom Edge (20dp) | Swipe up = Home |
| Bottom Edge (20dp) | Swipe up & pause for ~140ms before release = Recents |

Animations can be toggled between native blur or rainbow light strip in Beta Lab. Touches that do not trigger a gesture pass through immediately to underlying apps without affecting normal operations.

### Cover Screen Sidebar & Widgets

- **Sidebar**: Swipe inward from the cover screen edge to pull out the notification panel (quick toggles + notifications) and recent tasks.
- **Cover Screen Widgets** (Dual-Screen Quick Panel / Cover Launcher / Slot Machine / Custom Action / Phone Status): Standard "Add to Home Screen" only applies to the inner screen. **Cover screen widgets are separate** and must be added via **Settings → Cover Screen → Widgets** (labeled as "Cover screen → Widgets" on some systems).

## Pseudo Apps

On the secondary desktop of the cover screen, several custom pages are pinned alongside system apps: **Cover Touchpad** (controls inner screen using cover screen as touchpad), **Cast Control**, **Status Board**, and **Cover Media Player** (reads and controls system media playback). Visibility can be toggled individually under "Cover Screen Management" on the app home page.

## Frequently Asked Questions

**Q: Granted permission in Shizuku, but the app still shows "Not Ready"?**
Check if the Shizuku service itself is running (requires restarting after phone reboot), then tap "Grant / Re-detect" in the app once.

**Q: Features stop working after phone reboot?**
Shizuku needs to be restarted, followed by re-granting permission in the app once.

**Q: Rotated 90° but the orientation looks inverted/weird?**
This indicates the 90° / 270° orientation is inverted for your monitor. Follow "Calibrate Direction" above to set the angle to 270°.

**Q: No popup dialog appeared after plugging in the portable screen?**
Check in order: ① Is the phone screen locked/off? (Popups are suppressed on lock screen and fall back to notification banners); ② Was "Display over other apps" permission granted? (Notification banners serve as fallback if missing, clickable directly); ③ To test the pipeline immediately, tap **"Test Dialog"** in the app.

**Q: Does this affect the phone's inner screen?**
Rotation does not — all rotation commands apply exclusively to external displays. Resolution modification does (which is its intended function), but it requires explicit toggles + confirmation popups, with a "Restore Native" button available at any time.

**Q: Can I still keep SecondScreen?**
Yes, but **do not enable automatic mode on both** — they modify the same set of display parameters and will conflict if running simultaneously. Either use this app exclusively, or turn off automatic application in this app and use manual triggers.

## Building from Source

Standard Gradle Android project. Environment requirements: **JDK 17**, **Android SDK** (platform 34 + build-tools 34.0.0).

```bash
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`. You can also open this directory directly in Android Studio (`local.properties` with line `sdk.dir=<your SDK path>`).

> Shizuku client libraries come from Maven Central (`dev.rikka.shizuku:api` / `:provider`). If dependency fetching fails, replace `mavenCentral()` in `settings.gradle` with `https://maven.aliyun.com/repository/public`.

## License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details. Release history is documented in [CHANGELOG.md](CHANGELOG.md).
