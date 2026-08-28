I'm building a personal-use Android app called "Notif Simplifier." It intercepts
Android system notifications, strips them to plain text, stores them locally in
a Room database, and cancels the original notification so I only see a plain-text
list in my own app instead of the normal flashy notification UI — similar in
spirit to how the "Dumbphone" app (com.caaalm.dumbphonelauncher) filters/redirects
notifications with a per-app allowlist, but I only want the notification-filter
mechanic, not its launcher/UI style.

Requirements:
- Kotlin + Jetpack Compose, Material3, minSdk 26, targetSdk 34
- Use NotificationListenerService to capture android.service.notification
  StatusBarNotification objects system-wide
- Extract only EXTRA_TITLE and EXTRA_TEXT, ignore images/actions/colors
- Store captured notifications in a local Room database (no network, no cloud)

Per-app redirect control (core feature):
- Maintain a list of all installed apps that post notifications (use
  PackageManager to enumerate installed apps, filter to launchable/user apps)
- For each app, store a simple flag: "show normally" (default) vs "redirect
  to Notif Simplifier" — apps must be explicitly opted in, nothing is
  redirected until I toggle it on
- Build a settings screen listing every app (name + icon) with a toggle switch
  next to each one, so I can flip individual apps between the two modes
- Persist this per-app setting in the Room database (or DataStore, whichever
  is cleaner) so it survives app restarts

New app detection on launch:
- On each app open (MainActivity), compare the currently installed apps
  (from PackageManager) against the set of app package names already known
  to the database/DataStore — do this diff off the main thread since
  enumerating installed apps can be slow
- For any app installed since the last check that isn't already in the known
  set, show a simple prompt/dialog listing the new app(s) and asking whether
  to turn redirect ON or leave it as "show normally" (default) for each — a
  small Compose AlertDialog or bottom sheet with a toggle per new app is fine
- Whatever I choose gets saved the same way as the settings screen toggles
- After the prompt is dismissed/answered, update the "known apps" set so
  these don't get flagged as new again next launch

OTP / verification code bypass (always show normally, regardless of the
per-app toggle):
- Before applying the redirect logic, check the notification's title + text
  against an OTP-detection heuristic: look for keywords like "OTP", "one-time
  code", "verification code", "security code", "passcode", combined with a
  standalone 4-8 digit number pattern (regex, e.g. \b\d{4,8}\b) — a common
  approach used by notification-cleaner apps
- If it matches, skip the redirect entirely (don't cancel the notification,
  don't reroute it) even if that app is set to "redirect" — OTPs must always
  show normally and immediately, no exceptions
- Keep this keyword/regex list in one place (e.g. a small object/companion)
  so I can easily tweak it later if a code from some app gets missed
- Also support a manual per-app "always show normally, never redirect"
  override as a fallback, in case the regex misses a code format from a
  specific app

Main notification list screen:
- Plain monospace list, newest first, no unread counts, no color-coded
  urgency, no push notifications from this app itself. Should feel boring
  and calm to open, not stimulating. Only two main screens total: this list,
  and the per-app settings toggle screen above — keep it minimal, no extra
  chrome
- A button that opens Settings > Notification access
  (ACTION_NOTIFICATION_LISTENER_SETTINGS) since Android requires manual
  grant of that permission
- A "clear all" button

Constraints:
- This is for my own device only — no Play Store publishing, no obfuscation
  needed, no backend, no secrets/API keys involved anywhere

I already have a skeleton project (Gradle Kotlin DSL, manifest, Room
entities/DAO/database, the listener service, and the Compose UI). Review the
existing structure first, then extend it to add the per-app redirect toggle,
new-app-detection prompt, and OTP bypass logic described above.