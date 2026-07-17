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

## One-tap fire reporting (Firebase setup)

The big red **🔥 REPORT FIRE** button on the home screen is a separate
reporting path from the SMS alerting described above: it grabs a fresh GPS
fix, lets you optionally attach a photo and a short note, and on **Send**
writes a report document to **Firebase Firestore** (with the photo, if
any, uploaded to **Firebase Storage** first so its download URL can be
included). Each report includes GPS coordinates, a server timestamp, the
submitter's Firebase Auth UID, the photo URL (if attached), the note, and
a `status` field starting as `"Pending"`.

BFP staff see and act on those reports through the **responder
dashboard**: tap **Responder Login** on the home screen, sign in with an
email/password account (see "Create responder accounts" below — there's
no self-registration), and the dashboard shows a live, real-time-updating
list of incidents — barangay, coordinates, time, reporter, photo, note,
and status. Tapping **Update Status** on an incident lets a responder set
it to Accepted, Responding, Arrived, Fire Out, or False Alarm; the change
is written straight to Firestore and every responder's dashboard updates
immediately (no refresh needed) because the list is backed by a live
Firestore listener, not a one-time fetch. The same feed is also available
as a map (**Map View** on the dashboard) — see "Live Fire Map" below.

**This repo ships with a placeholder `app/google-services.json`** — a
structurally valid file with obviously fake values
(`REPLACE-WITH-YOUR-FIREBASE-PROJECT-ID`, etc.), the same pattern already
used here for the unconfirmed BFP phone number and the optional TFLite
model. It's there so the project **compiles and builds** without you
doing anything first. It does **not** point at a real backend — reports
will fail to submit (you'll see the "could not submit" toast) until you
create a real Firebase project and swap it in. This requires your own
Google account — nobody but you can do this part.

### 1. Create the project

1. Go to the [Firebase console](https://console.firebase.google.com/) and
   sign in.
2. **Add project** → name it (e.g. "PlacerFireWatch") → you can disable
   Google Analytics for this project (not used by the app) or leave it on,
   your call → **Create project**.

### 2. Connect the Android app

1. On the project Overview page, click the **Android icon** to add an app.
2. **Android package name**: `com.placer.firewatch` (must match exactly —
   this is the app's `applicationId`).
3. App nickname is optional. Skip the debug SHA-1 field — it's only
   needed for Google Sign-In/Phone Auth/Dynamic Links, none of which this
   app uses.
4. **Register app**, then **Download google-services.json**.
5. Click through the remaining "add the SDK" steps and **Continue to
   console** — the Gradle wiring for all of this is already in this repo.

### 3. Configure Authentication

1. Left sidebar → **Build → Authentication → Get started**.
2. **Sign-in method** tab → enable **both**:
   - **Anonymous** — every install signs in anonymously on first use (see
     `AuthManager.kt`) purely to get a stable UID to tag *reports* with,
     not a real account.
   - **Email/Password** — used by the **responder dashboard's login
     screen**. There's no self-registration; see "Creating responder
     accounts" below for how you add BFP staff logins.

### 4. Configure Firestore

1. **Build → Firestore Database → Create database**.
2. Pick a location close to the deployment area — `asia-southeast1`
   (Singapore) is a reasonable default for the Philippines.
3. Start in **test mode** for now (open read/write for 30 days) so you can
   verify things work immediately, then apply the real rules below.

### 5. Configure Firebase Storage

1. **Build → Storage → Get started**.
2. Same location choice as Firestore.
3. Start in **test mode** for now, same reasoning.

### 6. Configure Cloud Messaging

Nothing to click — Cloud Messaging activates automatically the moment you
add an Android app (step 2). There's no server yet that sends anything
through it; the app is wired to receive (`FireWatchMessagingService`)
purely as infrastructure for a future push-based alert channel (see
`ROADMAP.md`, item V2-6). **Build → Messaging** in the console just
confirms it's active.

### 7. Create responder accounts

The **"🔥 REPORT FIRE"** flow (reporters) and the **responder dashboard**
(BFP staff) use different identities on purpose — anyone can submit a
report anonymously, but only allowlisted accounts can see and manage the
incident feed. For each responder:

1. **Build → Authentication → Users → Add user** → enter their email and
   a password. Copy the **User UID** shown after creation.
2. **Build → Firestore Database → Start collection** (or use an existing
   one) named `responders` → create a document whose **Document ID is
   that exact UID** → give it any field, e.g. `role: "responder"` (the
   content doesn't matter — `firestore.rules` only checks that the
   document *exists*) → **Save**.
3. That person can now tap **Responder Login** in the app, sign in with
   the email/password from step 1, and see the live incident dashboard.

There's no in-app way to create or remove responder accounts — this is a
deliberate, manually-gated allowlist for an emergency-services tool.

### 8. Lock down security rules

Test mode above is open to anyone who has your API key — fine for initial
verification, not for real deployment. This repo includes
[`firestore.rules`](firestore.rules) and [`storage.rules`](storage.rules).
The Firestore rules require the anonymous-auth UID from step 3 for
*creating* reports, and require a `responders/{uid}` allowlist entry
(step 7) to *read the feed or update status* — and even then, a status
update can only change the `status` field to one of the five responder
actions, nothing else. Once you've verified writes work (step 9 below),
paste each file's contents into **Firestore Database → Rules** /
**Storage → Rules** in the console and **Publish**.

Note: incident photos still display in the dashboard even though
`storage.rules` says `allow read: if false` — Firebase Storage's
`getDownloadURL()` embeds an access token in the URL itself that bypasses
security rules by design, so possessing that specific URL (which only
this app's upload path produces) is what grants read access, not the
rule.

### 9. Install the real config

Replace `app/google-services.json` in this repo with the file you
downloaded in step 2, rebuild, and the app writes real report
documents/photos to your project instead of failing against the
placeholder.

## Live Fire Map (Google Maps setup)

From the responder dashboard, **Map View** opens a map showing every
incident as a colored marker — red for Pending, orange for an in-progress
response (Accepted/Responding/Arrived), green once resolved (Fire
Out/False Alarm) — updating live as Firestore changes. Tapping a marker
opens the same incident details and status actions as the dashboard list.
It's behind the same responder login as the dashboard, not public.

This needs a **Google Maps API key**, which is a separate credential from
Firebase (different console, different product). Unlike
`google-services.json`, this repo does **not** ship even a placeholder
file for it — it's read from `local.properties`, which is already
gitignored (it holds your Android SDK path) and never committed, so
there's no risk of a real key accidentally landing in git history. If the
key is missing, the build still succeeds (see `app/build.gradle.kts`) —
the map screen just won't load tiles until you add one.

1. Go to the [Google Cloud console](https://console.cloud.google.com/)
   and select the **same project** your Firebase project created (Firebase
   projects are Google Cloud projects — look for it in the project
   picker by the name you gave it) — this keeps everything under one
   project instead of creating a second, unrelated one.
2. **APIs & Services → Library** → search **"Maps SDK for Android"** →
   **Enable**.
3. **APIs & Services → Credentials → Create credentials → API key**.
   Copy the generated key.
4. (Recommended) Click the new key → **Restrict key** → **Application
   restrictions: Android apps** → add package name `com.placer.firewatch`
   with your debug/release signing certificate's SHA-1 (Android Studio:
   Gradle panel → app → Tasks → android → `signingReport`). This stops
   the key from being usable by anyone who extracts it from the APK.
5. In this repo, open (or create) `local.properties` and add a line:
   ```
   MAPS_API_KEY=your_real_key_here
   ```
6. Rebuild. The map now loads real tiles instead of a blank/gray screen.

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
- **One-tap report**: the **"🔥 REPORT FIRE"** button is a separate path
  from the above — it writes a report (location, timestamp, optional
  photo/note, status `"Pending"`) to Firebase instead of sending SMS. See
  "One-tap fire reporting (Firebase setup)" above.

## Permissions used, and why

| Permission | Why |
|---|---|
| Camera | reads the live feed for detection, and captures the optional fire-report photo |
| Location (fine/coarse) | tags alerts and fire reports with GPS coordinates |
| Send SMS | sends the alert text itself |
| Internet / network state | required by Firebase Firestore/Storage for one-tap fire reporting |
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
├── auth/AuthManager.kt      — anonymous Firebase Auth sign-in for report attribution
├── location/LocationProvider.kt — GPS fix (cached + fresh) via FusedLocationProviderClient
├── messaging/FireWatchMessagingService.kt — Cloud Messaging plumbing (no active feature yet)
├── notification/NotificationHelper.kt — foreground service notification
├── report/
│   ├── FireReport.kt        — one-tap report draft data class
│   ├── FireReportRepository.kt — writes reports to Firestore, photos to Storage
│   ├── ReportStatus.kt      — shared status string constants
│   ├── Incident.kt          — Firestore-mapped read model for the dashboard
│   └── IncidentRepository.kt — live incident listener + status updates
├── responder/
│   ├── ResponderLoginActivity.kt — email/password login for BFP staff
│   ├── ResponderDashboardActivity.kt — live incident list, logout, map entry point
│   ├── IncidentAdapter.kt   — RecyclerView list + per-incident status action menu
│   ├── IncidentDetailsView.kt — incident binding + status menu, shared by list and map
│   └── LiveFireMapActivity.kt — colored real-time markers, tap for incident details
└── util/                    — SharedPreferences wrapper, image conversion
```

## If you'd like changes

I can adjust this if any of these are true instead:
- You already have (or want to buy) dedicated smoke/gas sensor hardware
  rather than pure camera vision.
- You want this to cover several fixed locations at once reporting to a
  shared dashboard, not just one phone/one place.
- You'd like an email or Messenger alert channel added alongside SMS.
