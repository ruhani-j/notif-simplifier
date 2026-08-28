# Notif Simplifier — User Guide

> Take back control of your notification tray.

---

## What is this?

**Notif Simplifier** is a free Android app that acts as a gatekeeper for your notifications.

Most phones receive dozens of notifications a day from apps you didn't ask to hear from. Notif Simplifier lets you decide, per app, whether a notification should:

- **appear in your tray as normal** (Instant), or
- **be quietly captured in a clean in-app list** (Redirect) — out of your way until you choose to look.

Everything stays on your device. No accounts, no cloud sync, no ads, no analytics.

---

## Why you might want this

- You get too many notifications and can't easily silence specific apps without losing them entirely.
- You want to check WhatsApp/Instagram/email on your own schedule, not theirs.
- You're tired of marketing notifications cluttering your tray but don't want to miss real order updates.
- You want OTPs and payment alerts to always come through immediately, no matter what.
- You want a clean tray that only shows things that actually need your attention right now.

---

## Key features

### Per-app control
Every app on your phone can be set to **Redirect** or **Instant**. First time a new app sends a notification, Notif Simplifier pops up and asks you to choose — once, and it remembers.

### Smart filters that just work
You don't need to configure everything manually. Out of the box:

| Filter | What it does |
|---|---|
| **OTP bypass** | Bank/auth codes always go straight to your tray, never get buried |
| **Important bypass** | Delivery alerts, transactions, flight updates, and security warnings always pass through |
| **Ongoing filter** | Music players and download progress bars are ignored entirely |
| **System filter** | System-level notifications are left alone |
| **Marketing filter** (opt-in) | Promo blasts from shopping apps get redirected automatically; real order confirmations still come through |

### Never Redirect list
Pin any app so its notifications always pass through, no questions asked. Authenticator apps (Google Authenticator, Authy, etc.) are added here automatically on first launch.

### In-app notification list
Redirected notifications are stored in a clean, scrollable list:
- Newest first
- Shows app name, time, title, and body
- Tap a notification to jump straight into that app
- Swipe to dismiss one, or clear all at once

### Theming
Light, Dark, or follow-system — your choice.

---

## What it does NOT do

- It does not send any data anywhere. No internet permission.
- It does not read message contents beyond what Android's notification system exposes.
- It does not modify or delete notifications from apps that are in Instant mode.
- It is not a spam blocker or firewall — it only manages where notifications appear, not whether apps can send them.

---

## How to install it (for early users)

Right now Notif Simplifier is not on the Play Store. To install it you'll need the APK file and about 2 minutes.

### Step 1 — Get the APK

Ask the developer for the latest `app-debug.apk` file and transfer it to your phone (via USB, Google Drive, email — whatever's easiest).

### Step 2 — Allow installs from unknown sources

On Android 8+:

1. Open **Settings → Apps → Special app access → Install unknown apps**
2. Find the app you'll use to open the APK (e.g. Files, Chrome) and toggle **Allow from this source**

### Step 3 — Install

Open the APK file on your phone and tap **Install**.

### Step 4 — Grant notification access

1. Open Notif Simplifier
2. Tap **Grant notification access** in Settings
3. Android will open **Settings → Notification access** — find Notif Simplifier and enable it
4. Confirm the permission prompt

That's it. The app is now active. The next time any app sends a notification, Notif Simplifier will ask you what to do with it.

---

## Tips

- **Start with Redirect on your noisiest apps** (social media, news, shopping). You'll be surprised how much quieter your day gets.
- **Keep your bank and messaging apps on Instant** if you need immediate alerts from them — or use the **Never Redirect** list to guarantee they're always excluded.
- **Turn on the Marketing filter** in Settings if you shop online a lot. It's good at catching promotional blasts while letting real order updates through.
- You can change any app's mode at any time in **Manage apps**.

---

## Privacy

- All data is stored locally in a Room (SQLite) database on your device.
- No network requests are made. The app has no internet permission.
- No analytics, crash reporting, or telemetry of any kind.
- Uninstalling the app removes all stored data.

---

## Requirements

- Android 8.0 (Oreo) or newer
- Notification access permission (prompted on first launch)

---

## Feedback / contributing

This is a personal-use project shared openly. If you try it and hit a bug or have a suggestion, open an issue or reach out directly.
