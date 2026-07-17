# Placer FireWatch

An Android app that watches a camera feed for smoke/fire and SMS-alerts
Bureau of Fire Protection (BFP) contacts with GPS location, built for use
in Placer, Masbate, Philippines.

## What this is, honestly

- **Detection**: phones don't have physical smoke sensors, so this uses the
  camera. Out of the box it runs a color/shape heuristic (works
  immediately, no setup, moderate accuracy). There's a drop-in upgrade path
  to a trained TensorFlow Lite model for real machine-learning accuracy
  (see "Improving detection accuracy" below).
- **Best deployment model**: an old/spare Android phone mounted in a fixed
  spot with a view of what you want watched (a market, a cluster of wooden
  houses, a forest edge), kept on continuous power, running 24/7 as a
  dedicated sensor — not something you'd carry in your pocket.
- **This is a supplementary tool, not certified fire-safety equipment.**
  It should sit alongside, not replace, real smoke detectors, community
  vigilance, and people calling BFP directly when they see a fire.
- I can't compile this into an APK myself — I genuinely tried (real Android
  build tools are installable, but the one non-negotiable piece, the dex
  compiler that turns Java bytecode into something Android can run, can
  only be obtained from Google's own servers, which I can't reach). What's
  here is a complete, real Android Studio project, plus a free automated
  way to get a real APK without installing anything yourself — see
  "Get a debug APK for free via GitHub Actions" below.

## About the BFP Placer, Masbate contact number — please read this

I searched for a specific, verified phone number for the BFP fire station
in Placer, Masbate and could not confirm one I'd trust to hardcode into a
safety app. I found real leads, but no verified direct number:

- A **"Placer Masbate FireStation"** Facebook page exists (found via
  another station's linked-pages list) — search Facebook for that name.
- **BFP R5 – Masbate Provincial Office** has a Facebook page that may be
  able to confirm the Placer station's number directly.
- The official **BFP Region 5 directory** (`region5.bfp.gov.ph/about-us/directory/`)
  likely lists every municipal station in Bicol including Placer, but the
  site blocked automated fetching — open it in a normal browser.
- **LGU Placer, Masbate** has a Facebook page that may list or link to it.
- Calling or visiting the Placer municipal hall / MDRRMO (Municipal
  Disaster Risk Reduction and Management Office) directly is the most
  reliable way to get the number, and to ask how they'd like automated
  alerts formatted.

**What I pre-configured instead:** the app ships with **911** as the
default alert number — the Philippines' unified national emergency
hotline (police/fire/medical, nationwide, 24/7, and confirmed to support
Cebuano and other local dialects, which matters since Placer is
Cebuano-speaking). This is a real, verified, working number, so the app
is never configured with zero contacts. **Add the direct BFP Placer
number in Settings the moment you have it verified** — a direct line is
faster than routing through a national call center.

**Before relying on this for real:** talk to BFP Placer directly about
how they want to receive automated reports. Many fire departments prefer
a verified human phone call over automated SMS blasts, specifically to
avoid false dispatches. Consider proposing this app's alerts as a
"heads-up" a human then confirms by calling — that's how the in-app
"Call BFP / 911" button is intended to be used alongside the automated
SMS.

## Get a debug APK for free via GitHub Actions

This is the "cost effective beta" path — it produces a real, installable
APK without you installing Android Studio, and without paying for
anything. It works because GitHub's build servers (unlike the sandbox I
run code in) can freely reach Google's Android SDK servers. A included
workflow file (`.github/workflows/build-apk.yml`) does the actual build.

1. Create a free account at [github.com](https://github.com) if you don't have one.
2. Create a **new repository** (the "+" icon top-right → New repository).
   It can be public (simplest — public repos get unlimited free Actions
   minutes) or private (small free monthly minutes allowance, still
   enough for this).
3. On the new repo's page, use **"uploading an existing file"** (a link
   on the empty repo's page) and drag in every file/folder from this
   `PlacerFireWatch` folder, keeping the folder structure intact
   (including the hidden `.github` folder — if your file browser hides
   it, show hidden files first, or use GitHub Desktop instead of the web
   uploader). Commit the upload.
4. Go to the **Actions** tab of your repo. A workflow run should start
   automatically (or click **"Build Debug APK" → "Run workflow"** if not).
5. Wait a few minutes for it to finish (green checkmark).
6. Click the finished run → scroll to **Artifacts** → download
   **PlacerFireWatch-debug-apk**. That's a zip containing `app-debug.apk`.
7. Copy `app-debug.apk` to your phone (email it to yourself, use a USB
   cable, Google Drive, whatever's easiest), tap it, and allow "install
   from unknown sources" when prompted. That's the real app, installed.

This is a **debug build**: self-signed automatically by Gradle, not
signed for the Play Store. That's normal and expected for a beta you're
sideloading onto your own device — it is not a lesser or broken version
of the app, just not Play-Store-distributable as-is.

Every time you (or I) change the code and re-upload, this rebuilds
automatically — so this doubles as a free, repeatable build pipeline for
future updates, not just a one-time export.

## Setup (Android Studio)

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open Android Studio → **Open** → select the `PlacerFireWatch` folder.
3. Let Gradle sync (first time may take a few minutes; it downloads
   dependencies). If Android Studio prompts about a missing Gradle
   wrapper jar, accept its offer to regenerate one — this is normal for
   hand-assembled projects.
4. Connect a real Android phone via USB with USB debugging enabled (the
   emulator has no real camera feed, so testing detection needs a real
   device), or place a physical Android device where you plan to deploy.
5. Click **Run**. Grant camera, location, and SMS permissions when
   prompted.
6. Open **Settings** in the app and:
   - Enter the verified BFP Placer number(s) once you have them (comma-
     separated if more than one; 911 stays as a safe fallback).
   - Enter a location label (e.g. "Barangay Poblacion, Placer, Masbate").
   - Tap **Send Test Alert** to confirm SMS actually reaches your
     configured numbers — it's clearly labeled TEST so it won't be
     mistaken for a real report.
7. Tap **Start Monitoring** to begin continuous detection (runs as a
   foreground service, so it keeps running with the screen off or the
   app backgrounded).

## Improving detection accuracy (optional)

The built-in heuristic looks at flame-colored and smoke-colored regions
in each frame. It will produce false positives (sunsets, orange clothing,
fog, dust) and can miss fires that don't match its color assumptions.
For real accuracy, train a small image classifier:

1. Go to [teachablemachine.withgoogle.com](https://teachablemachine.withgoogle.com/) (free, no coding required).
2. Create an **Image Project** → Standard image model.
3. Create 3 classes: `normal`, `smoke`, `fire`. Feed it a few hundred
   photos/video frames of each — ideally taken from the actual camera and
   location you're deploying, in different lighting/weather, since a
   model trained on generic stock photos will generalize worse than one
   trained on your actual scene.
4. Train, then **Export Model → TensorFlow Lite → Floating point**.
5. Rename the exported file to `fire_smoke_model.tflite` and place it in
   `app/src/main/assets/`.
6. Rebuild and reinstall the app. It detects the file automatically and
   switches from the heuristic to your model (see `ClassifierFactory.kt`).

If you already have a hardware smoke/gas sensor setup (e.g. an ESP32 or
Arduino with an MQ-2 sensor broadcasting over Wi-Fi/Bluetooth/MQTT),
that's a more traditional and often more reliable approach than camera
vision — tell me your hardware and I can wire up a receiver for it
instead of, or alongside, the camera path.

## How alerting works

- **Automatic**: when the detector flags fire or smoke for several
  consecutive frames (filtering out one-off false triggers), the service
  fetches the last known GPS fix and sends an SMS to every configured
  number, including a Google Maps link and a note that it's an automated,
  unverified alert. A 5-minute cooldown prevents repeat SMS spam for one
  ongoing event.
- **Manual**: the **"Report Fire Now"** button on the main screen sends
  the same kind of SMS immediately, worded as a resident's manual report
  instead of an automated one.
- **Direct call**: the **"Call BFP / 911"** button opens the phone dialer
  pre-filled with the first configured number — it does not place the
  call automatically, so a human always confirms before it's dialed.

## Permissions used, and why

| Permission | Why |
|---|---|
| Camera | reads the live feed for detection |
| Location (fine/coarse) | tags alerts with GPS coordinates |
| Send SMS | sends the alert text itself |
| Foreground service (+ camera type) | keeps detection running with the screen off |
| Post notifications | shows the required "monitoring active" notification |
| Wake lock | keeps the CPU from sleeping mid-monitoring |
| Receive boot completed | resumes monitoring automatically after a power cycle |

## Known limitations

- Camera-based detection depends entirely on the camera having a clear,
  unobstructed view of the area at risk — it detects nothing outside its
  field of view.
- The heuristic classifier has real false-positive/false-negative rates;
  test it extensively in your actual deployment conditions (day, night,
  rain) before trusting it operationally.
- SMS delivery depends on cell signal at the monitoring location — test
  this specifically if the site is remote.
- `SEND_SMS` is a sensitive permission on the Google Play Store (subject
  to the default-handler policy); this project is meant for direct/side-
  loaded installation on a dedicated device, not Play Store distribution.

## Project structure

```
app/src/main/java/com/placer/firewatch/
├── MainActivity.kt          — camera preview UI, permissions, manual report/call
├── SettingsActivity.kt      — BFP numbers, location label, sensitivity, test alert
├── MonitoringService.kt     — foreground service: continuous capture + detection + alerting
├── BootReceiver.kt          — resumes monitoring after reboot
├── detection/
│   ├── Classifier.kt        — shared interface + result types
│   ├── HeuristicClassifier.kt — zero-setup color-based detector (default)
│   ├── TFLiteClassifier.kt  — optional trained-model detector
│   ├── ClassifierFactory.kt — picks whichever is available
│   └── DetectionTracker.kt  — requires consecutive hits before alerting
├── alert/AlertSender.kt     — composes and sends the SMS
├── location/LocationProvider.kt — GPS fix via FusedLocationProviderClient
├── notification/NotificationHelper.kt — foreground service notification
└── util/                    — SharedPreferences wrapper, image conversion
```

## If you'd like changes

I can adjust this if any of these are true instead:
- You already have (or want to buy) dedicated smoke/gas sensor hardware
  rather than pure camera vision.
- You want this to cover several fixed locations at once reporting to a
  shared dashboard, not just one phone/one place.
- You'd like an email or Messenger alert channel added alongside SMS.
