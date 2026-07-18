# Placer FireWatch — Project Status

Rewritten as of commit `1d0dc26` (2026-07-18), after implementing every
section of the Version 1.0 Product Specification (Sections 1–14). This
supersedes the earlier version of this document, which described the
pre-Firebase, single-device camera-monitoring prototype only.

---

## What the app is now

A Firebase-backed Android app for fire/smoke reporting in Placer,
Masbate, with three roles (citizen, responder, admin), real-time
incident tracking, and the original camera-based detection/alerting
still present as a citizen-facing feature. Package layout by feature:

```
com.placer.firewatch/
├── LandingActivity        — launcher: routes signed-in users by role, otherwise Sign In/Create Account
├── auth/                  — SignInActivity, CreateAccountActivity (Firebase Auth email/password)
├── MainActivity            — citizen home: typed report buttons, camera monitoring, Call BFP, Facebook Live
├── SettingsActivity, AboutActivity
├── admin/                  — AdminHomeActivity: responder approvals, BFP number, report export
├── responder/
│   ├── ResponderDashboardActivity — live incident feed, status filter chips, alerts
│   ├── LiveFireMapActivity        — Google Map with type-colored, status-dimmed markers
│   ├── apply/                     — ResponderApplicationActivity (citizen → responder application)
│   └── DevResponderSession        — debug-only Firestore bypass for UI testing
├── report/
│   ├── Incident, ReportType, ReportStatus, IncidentRepository, FireReportRepository
│   └── export/                    — XlsxWriter, DocxWriter, ReportExportRepository (Section 12)
├── alert/                  — AlertSender (SMS), ResponderAlertNotifier (in-app notification channels)
├── facebook/FacebookLive.kt
├── barangay/Barangays.kt   — the 35 barangays of Placer, Masbate
├── settings/AppSettingsRepository — admin-configured BFP number (app_settings/config)
├── messaging/FireWatchMessagingService — FCM receiver, wired to ResponderAlertNotifier
├── detection/, MonitoringService, BootReceiver — original camera-based fire/smoke detection (unchanged)
└── util/, notification/
```

**Backend:** Firebase project `placer-firewatch`. Firebase Auth
(email/password) for all three roles. Firestore collections: `users`,
`responder_applications`, `fire_reports`, `app_settings`. Firebase
Storage for report photos and responder application documents (ID,
barangay certification, selfie). Security rules
(`firestore.rules`/`storage.rules`) enforce role checks and field-level
write restrictions — see the rules file for the exact policy per
collection.

**Build/CI:** Gradle (AGP 8.4.0, Kotlin 1.9.24, compileSdk/targetSdk 34,
minSdk 24). GitHub Actions builds `assembleDebug` on every push. Unit
tests exist (`app/src/test/`) and run locally via
`./gradlew assembleDebug testDebugUnitTest`; CI does not yet run the
test task (see Gaps below).

---

## Section-by-section status (Version 1.0 spec)

| # | Section | Status | Notes |
|---|---|---|---|
| 1 | User Authentication | ✅ Done | Firebase Auth email/password, `LandingActivity` role-routing gate |
| 2 | User Roles | ✅ Done | `users/{uid}.role` ∈ {citizen, responder, admin}, enforced in `firestore.rules` |
| 3 | Responder Registration | ✅ Done | Full application form + document/selfie upload + admin review |
| 4 | Admin | ✅ Done | Approve/reject responders, set BFP number, export reports |
| 5 | Barangays | ✅ Done | 35-barangay dropdown, `Barangays.kt` |
| 6 | Report Types | ✅ Done | Fire / Smoke / Suspected Fire, separate buttons, threaded through the whole pipeline |
| 7 | Google Maps | ✅ Done | Live Fire Map (type-colored, status-dimmed markers) + turn-by-turn "Get Directions" |
| 8 | Emergency Alerts | ⚠ Partial | Client-side notification channels/vibration patterns fully built; **no server-side trigger** — see Known Gaps |
| 9 | Facebook Live | ✅ Done | Deep-link launch (`fb://live_camera`) with Play Store fallback |
| 10 | BFP Call Button | ✅ Done | Admin-configurable number, dialer pre-fill (no auto-dial) |
| 11 | Responder Dashboard | ✅ Done | Live feed, status actions, All/Pending/Active/Resolved filter chips |
| 12 | BFP Reports | ✅ Done | Admin export to `.xlsx`/`.docx`, hand-rolled OOXML writers (no Apache POI) |
| 13 | About Page | ✅ Done | Static page, developer credit, KJV Proverbs 11:11 |
| 14 | UI Design | ✅ Done | App-wide rounded-corner theme (ShapeAppearance overrides), card elevation, list entrance animation |

---

## Known Gaps

- **Section 8 has no real push trigger.** `ResponderAlertNotifier` builds
  correct per-type notification channels, priorities, and vibration
  patterns, and `ResponderDashboardActivity` fires them for new Pending
  incidents *while the dashboard is open*. A true background push
  requires a Cloud Function watching `fire_reports` writes, which
  requires upgrading the Firebase project to the **Blaze (pay-as-you-go)
  plan** — a manual account decision, not a code change.
  `FireWatchMessagingService` is already wired to reuse the same
  notifier the moment that exists.
- **`app/google-services.json` commit status is still undecided.** It
  contains a real Firebase API key. Every commit in this session
  deliberately excluded it, so it remains locally modified/untracked
  from git's perspective — **needs an explicit decision**: commit it (Google's
  own guidance is that this key is not a secret requiring protection,
  since Firestore/Storage access is actually gated by security rules,
  not this key) or add it to `.gitignore` and document the manual setup
  step in the README instead.
- **Physical-device install is blocked.** `adb install` fails with
  `INSTALL_FAILED_USER_RESTRICTED` on the test Xiaomi phone (HyperOS
  "Install via USB" developer option, separate from "USB debugging").
  Per explicit instruction, all Section 2–14 work was verified via
  `./gradlew assembleDebug testDebugUnitTest` + GitHub Actions CI only,
  not on-device. **Nothing in Sections 2–14 has been visually confirmed
  on a real screen.**
- **CI does not run unit tests.** `testDebugUnitTest` passes locally but
  the GitHub Actions workflow only runs `assembleDebug`; a regression in
  test-covered logic (currently just `Barangays`) wouldn't fail CI.
- **No architecture layer / DI**, as previously noted — this is still
  true and now spans a larger surface (7+ repositories, all constructed
  directly in their activities).

---

## Everything from the original (pre-Firebase) status doc

The original camera-based detection pipeline (`MonitoringService`,
`HeuristicClassifier`/`TFLiteClassifier`, `DetectionTracker`, SMS
alerting) is unchanged by this session's work and still has the bugs,
technical debt, and performance issues catalogued in git history prior
to commit `f89f5c4`. That analysis was not re-verified this session and
may be stale; re-audit before relying on it.
