package com.placer.firewatch.util

import android.content.Context

/**
 * Thin wrapper around SharedPreferences for all user-configurable settings.
 *
 * NOTE ON DEFAULT_NUMBERS: 911 is the Philippines' unified national emergency
 * hotline (police/fire/medical, nationwide, 24/7, multi-dialect support).
 * It is used here only as a safe starting default so the app is never
 * configured with zero contacts. Replace/add the direct BFP Placer, Masbate
 * number in Settings as soon as you have it verified — see the README for
 * where to find it.
 */
object Prefs {
    private const val PREFS_NAME = "placer_firewatch_prefs"
    private const val KEY_BFP_NUMBERS = "bfp_numbers"
    private const val KEY_LOCATION_LABEL = "location_label"
    private const val KEY_SENSITIVITY = "sensitivity"
    private const val KEY_MONITORING_ENABLED = "monitoring_enabled"

    private const val DEFAULT_NUMBERS = "911"

    // Must be one of Barangays.ALL (see barangay/Barangays.kt) — Settings and
    // Responder Registration both select from that fixed list now, not free text.
    private const val DEFAULT_LOCATION_LABEL = "Poblacion"
    private const val DEFAULT_SENSITIVITY = 50

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBfpNumbers(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_BFP_NUMBERS, DEFAULT_NUMBERS) ?: DEFAULT_NUMBERS
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setBfpNumbers(context: Context, numbers: String) {
        prefs(context).edit().putString(KEY_BFP_NUMBERS, numbers).apply()
    }

    fun getLocationLabel(context: Context): String =
        prefs(context).getString(KEY_LOCATION_LABEL, DEFAULT_LOCATION_LABEL) ?: DEFAULT_LOCATION_LABEL

    fun setLocationLabel(context: Context, label: String) {
        prefs(context).edit().putString(KEY_LOCATION_LABEL, label).apply()
    }

    fun getSensitivity(context: Context): Int =
        prefs(context).getInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)

    fun setSensitivity(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SENSITIVITY, value).apply()
    }

    fun isMonitoringEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MONITORING_ENABLED, false)

    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }
}
