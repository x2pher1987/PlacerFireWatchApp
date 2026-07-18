# Placer FireWatch — Milestone Roadmap

Rewritten as of commit `1d0dc26` (2026-07-18). All 14 sections of the
Version 1.0 Product Specification are implemented — see
[`PROJECT_STATUS.md`](PROJECT_STATUS.md) for the section-by-section
table. This roadmap now covers what's left to make v1.0 *verified and
shippable*, not what's left to build.

---

## Milestone A — Verification (blocks calling v1.0 "done")

Nothing in Sections 2–14 has been run on a real device or emulator —
all of it was verified via `./gradlew assembleDebug testDebugUnitTest`
+ GitHub Actions CI only, per explicit instruction to keep developing
through the `INSTALL_FAILED_USER_RESTRICTED` device-install block.

| ID | Task | Depends on |
|---|---|---|
| A-1 | Resolve the HyperOS "Install via USB" developer-option block (or test on a different device/emulator) | — |
| A-2 | Full auth smoke test: Sign Up, Sign In, session persistence, Firestore `users/{uid}` doc creation | A-1 |
| A-3 | End-to-end citizen flow: submit a Fire/Smoke/Suspected Fire report with photo, confirm Storage upload + Firestore write | A-1 |
| A-4 | Responder flow: apply → admin approve → dashboard shows the live report → status update → Get Directions | A-1 |
| A-5 | Verify Section 8 alerts actually fire (channel, priority, vibration pattern) with the dashboard open | A-1 |
| A-6 | Verify Section 12 export produces files Excel/Word actually open (the OOXML writers are hand-rolled and have never been opened in real Office/Google Docs) | A-1 |
| A-7 | Visual pass on Section 14's theme changes — confirm ShapeAppearance overrides didn't distort any existing screen (dialogs, spinners, chips) | A-1 |

**Exit criteria:** every section in the spec has been exercised on a
real screen at least once, not just compiled.

---

## Milestone B — Manual/account decisions

Each of these is explicitly *not* a code change — flagged repeatedly
during development as requiring the account owner's decision.

| ID | Task | Notes |
|---|---|---|
| B-1 | Decide `app/google-services.json` commit vs. `.gitignore` | Blocks nothing functionally, but the repo's current state (locally modified, never committed) isn't sustainable long-term |
| B-2 | Upgrade Firebase project to Blaze plan | Required for B-3 |
| B-3 | Write the Cloud Function that watches `fire_reports` creates and sends FCM pushes | Depends on B-2. Once done, `FireWatchMessagingService.onMessageReceived()` already has the matching data-payload contract (`type`, `incidentId`, `barangay`) |
| B-4 | Facebook Developer app registration/review, if Section 9's deep-link approach needs anything beyond the current `fb://live_camera` intent | Only if App Review is actually required for this integration type — verify first |
| B-5 | Confirm the real BFP Placer emergency contact number with the fire station directly (Settings screen already warns against guessing) | — |

---

## Milestone C — Hardening (carried over, still valid)

The pre-Firebase `PROJECT_STATUS.md`'s Bugs/Technical Debt/Security
sections for the camera-detection pipeline (`MonitoringService`,
classifiers, `AlertSender`) were not re-verified this session. Before
relying on them:

| ID | Task |
|---|---|
| C-1 | Re-audit `MonitoringService`/`MainActivity` camera-provider conflict (previously flagged as breaking active monitoring when the app is opened) |
| C-2 | Re-audit the foreground-service permission gate on Android 14 |
| C-3 | Add CI test stage (`testDebugUnitTest`) so future regressions in tested code actually fail CI |
| C-4 | Expand unit test coverage beyond `Barangays` — `ReportStatus`/`ReportType` filtering logic, `XlsxWriter`/`DocxWriter` XML escaping, `ResponderAlertNotifier` channel selection are all pure-logic and cheap to test |

---

## Summary

```
A-1 → A-2, A-3, A-4, A-5, A-6, A-7   (device verification, currently the biggest open risk)
B-1                                  (repo hygiene decision)
B-2 → B-3                            (real push notifications)
B-4, B-5                             (independent account/verification tasks)
C-1, C-2                             (re-audit old known bugs)
C-3 → C-4                            (test infrastructure)
```

Milestone A is the highest-priority next step: the app compiles and
passes CI, but **has not been seen running** since Section 1 (auth) was
last verified on-device. Everything built in Sections 2–14 is
code-complete but functionally unconfirmed.
