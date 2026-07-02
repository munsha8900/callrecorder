# Call Recorder (Android)

A microphone-based call recorder for your own business calls. Auto-detects
regular phone calls and WhatsApp calls, records to `.m4a`, and saves into
your phone's `Music/CallRecorder` folder.

## How it works (and the one rule you must know)

This app records the **microphone**. Since Android 10, third-party apps cannot
tap the protected call-audio stream, and WhatsApp audio runs inside its own
encrypted, sandboxed process. No non-root app can read those streams directly.

**To capture BOTH sides of any call — regular or WhatsApp — put the call on
speakerphone.** The other person's voice then comes out the speaker and into the
mic, so it gets recorded. On the earpiece, you'll only capture your own side.

## Features

- Manual record button (works anywhere, anytime)
- Auto-record regular incoming/outgoing calls (phone-state trigger)
- Auto-record WhatsApp calls (via WhatsApp's ongoing-call notification)
- Persistent "Recording" notification while active
- Recordings list: play, share, delete
- Files saved to `Music/CallRecorder/Call_YYYYMMDD_HHMMSS.m4a`

## Setup on the phone (first run)

1. Grant **Microphone**, **Phone**, and **Notifications** permissions when asked.
2. Toggle **Auto-record phone calls** on.
3. Tap **Enable WhatsApp call detection** and grant **Notification access** to
   "Call Recorder" in the system screen that opens. (Needed only for WhatsApp.)

## Build (via GitHub Actions — no Android Studio needed)

1. Create a new GitHub repo and push this whole folder to it.
2. Go to the repo's **Actions** tab — the "Build APK" workflow runs on push.
3. When it finishes, open the run and download the **call-recorder-apk**
   artifact. Inside is `app-debug.apk`.
4. Copy the APK to your phone and install it (enable "install unknown apps"
   for your file manager/browser).

You can also trigger it manually from **Actions → Build APK → Run workflow**.

## Known limitations (Android platform, not fixable in app code)

- **Both-sides audio requires speaker** (explained above).
- **Auto-start on Android 12+** can be blocked when the app is fully in the
  background — the OS restricts background service starts. The manual button is
  the reliable fallback; auto is best-effort.
- **WhatsApp detection** depends on WhatsApp's ongoing-call notification, which
  can change between WhatsApp versions.

## Output format

Records `.m4a` (AAC) — high quality, small files, plays everywhere. Android's
built-in recorder does not natively encode **.mp3**. If you specifically need
`.mp3`, two options:
- Batch-convert with ffmpeg later (you already use ffmpeg), or
- Bundle a LAME encoder into the app (needs the NDK). Ask and I'll wire it in.

## Legal note

Recording calls without the other party's knowledge is illegal in many places
(all-party-consent regions include much of the EU, and US states like
California and Florida). The persistent recording notification helps, but for
compliance you should tell the other party they're being recorded. This is your
responsibility, not the app's.
