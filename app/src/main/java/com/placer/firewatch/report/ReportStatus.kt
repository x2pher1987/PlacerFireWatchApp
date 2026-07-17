package com.placer.firewatch.report

/** Shared between the one-tap report flow (always starts at PENDING) and the responder dashboard. */
object ReportStatus {
    const val PENDING = "Pending"
    const val ACCEPTED = "Accepted"
    const val RESPONDING = "Responding"
    const val ARRIVED = "Arrived"
    const val FIRE_OUT = "Fire Out"
    const val FALSE_ALARM = "False Alarm"

    /** The status transitions a responder can apply, in the order shown in the dashboard's action menu. */
    val RESPONDER_ACTIONS = listOf(ACCEPTED, RESPONDING, ARRIVED, FIRE_OUT, FALSE_ALARM)
}
