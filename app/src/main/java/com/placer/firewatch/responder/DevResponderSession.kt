package com.placer.firewatch.responder

import android.content.Context
import com.placer.firewatch.report.Incident
import com.placer.firewatch.report.ReportStatus
import java.util.Date

/**
 * ⚠ DEVELOPMENT ONLY — lets the responder dashboard be tested before a real
 * Firebase project exists. Bypasses Firebase Auth entirely (there's no
 * project to sign in against yet) and feeds the dashboard 3 hardcoded
 * sample incidents instead of a live Firestore listener.
 *
 * Every call site that touches this is already gated behind
 * BuildConfig.DEBUG (see LandingActivity, SignInActivity, ResponderDashboardActivity),
 * so none of this is reachable in a release build — the button that starts
 * a dev session simply doesn't render. That said, with isMinifyEnabled =
 * false the code itself still ships inside the release APK, just dead and
 * unreachable; before any real production/Play-adjacent release, either
 * delete this file and its call sites outright, or enable R8 minification
 * so the unreachable branches get stripped.
 *
 * Storing the flag in SharedPreferences (not Firebase) is deliberate: it
 * must work with zero backend configured.
 */
object DevResponderSession {
    private const val PREFS_NAME = "dev_responder_session"
    private const val KEY_ACTIVE = "active"

    fun isActive(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)

    fun start(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, true).apply()
    }

    fun end(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
    }

    /**
     * 3 sample incidents covering the status buckets the dashboard/map
     * distinguish: an untouched report, one being actively worked, and one
     * resolved. There's no literal "Resolved" status in ReportStatus (see
     * that file) — FIRE_OUT is used as the resolved example, same mapping
     * LiveFireMapActivity uses for its green marker bucket. Locations are
     * clearly-fake placeholders, not real barangays.
     */
    fun sampleIncidents(): List<Incident> {
        val now = System.currentTimeMillis()
        return listOf(
            Incident(
                id = "dev-sample-pending",
                latitude = 12.3550,
                longitude = 123.3520,
                timestamp = Date(now),
                userId = "dev-reporter-001",
                photoUrl = null,
                note = "Heavy smoke reported near the sample market area, spreading toward nearby houses.",
                status = ReportStatus.PENDING,
                barangay = "Sample Barangay A"
            ),
            Incident(
                id = "dev-sample-responding",
                latitude = 12.3480,
                longitude = 123.3610,
                timestamp = Date(now - 15 * 60 * 1000L),
                userId = "dev-reporter-002",
                photoUrl = null,
                note = "Fire crew dispatched, currently en route.",
                status = ReportStatus.RESPONDING,
                barangay = "Sample Barangay B"
            ),
            Incident(
                id = "dev-sample-resolved",
                latitude = 12.3620,
                longitude = 123.3455,
                timestamp = Date(now - 2 * 60 * 60 * 1000L),
                userId = "dev-reporter-003",
                photoUrl = null,
                note = "Small grass fire, fully extinguished.",
                status = ReportStatus.FIRE_OUT,
                barangay = "Sample Barangay C"
            )
        )
    }
}
