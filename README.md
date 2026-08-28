# Notif Simplifier

A minimal personal-use Android app that intercepts your system notifications,
strips them to plain text, stores them locally, and cancels the original
(flashy) notification so you only see a boring plain-text list instead.

## How it works

- `service/MyNotificationListener.kt` — a `NotificationListenerService` that
  Android calls every time any app posts a notification. It pulls out just
  the title + text, saves it to a local Room database, and cancels the
  original notification so it never shows a banner/badge/sound.
- `data/` — Room database (`AppDatabase`, `NotificationDao`,
  `NotificationEntity`) storing captured notifications on-device only.
  No network calls, no external services.
- `ui/NotificationListScreen.kt` — a plain monospace list UI (Jetpack
  Compose), newest first, no unread counts or color-coded urgency.
- `MainActivity.kt` — wires it together and has a button that jumps to the
  system settings screen where you grant "Notification access" (Android
  requires this to be done manually — it can't be auto-granted).

## Setup / build steps

1. Open this folder in Android Studio (File → Open → select `NotifSimplifier`).
2. Let Gradle sync (it will download the dependencies listed in
   `app/build.gradle.kts`).
3. Connect your phone via USB with USB debugging enabled, or use an
   emulator, and click Run. This installs a debug-signed APK directly —
   no Play Store account needed.
4. On first launch, tap **"Grant notification access"** in the app, which
   opens Settings → Notification access. Find "Notif Simplifier" in the
   list and enable it.
5. Notifications from other apps should now stop showing their normal
   banners and instead appear as plain text entries in this app.

## Things you'll probably want to tweak

- `ignoredPackages` in `MyNotificationListener.kt` — add package names of
  apps you don't want captured (it already ignores itself). You can find a
  package name via `adb shell dumpsys notification` or just by testing.
- Currently *every* notification gets intercepted and cancelled. You may
  want to whitelist only specific apps (e.g. just messaging apps) rather
  than blacklisting — flip the logic in `onNotificationPosted` if so.
- No auto-delete/retention policy yet — notifications accumulate until you
  tap "Clear all". Add a scheduled cleanup in `NotificationDao` if you want
  automatic pruning after N days.
- App icon/launcher icon uses the default; drop a real icon into
  `res/mipmap-*` folders if you want.

## Notes on "personal use only"

- This build has `isMinifyEnabled = false` (no ProGuard/R8) since it's not
  being distributed — fine for your own device.
- No secrets, API keys, or network permissions are used anywhere in this
  app, so there's nothing to secure beyond the on-device data itself.
- The generated APK is signed with Android Studio's default debug key,
  which is fine for installing on your own device via USB/ADB.
