package com.placer.firewatch.detection

/**
 * Requires several consecutive positive frames before raising an alert.
 * This filters out one-off false positives (a passing headlight, a red
 * shirt, a sunset reflection) at the cost of a few seconds of latency.
 *
 * `threshold` is derived from the user's sensitivity setting in
 * MonitoringService — lower threshold = more sensitive = more alerts
 * (and more false positives).
 */
class DetectionTracker(
    private val requiredConsecutiveHits: Int = 3,
    private val threshold: Float = 0.15f
) {
    private var consecutiveFireHits = 0
    private var consecutiveSmokeHits = 0

    fun update(result: DetectionResult): AlertTrigger? {
        consecutiveFireHits = if (result.fireConfidence > threshold) consecutiveFireHits + 1 else 0
        consecutiveSmokeHits = if (result.smokeConfidence > threshold) consecutiveSmokeHits + 1 else 0

        return when {
            consecutiveFireHits >= requiredConsecutiveHits -> AlertTrigger.FIRE
            consecutiveSmokeHits >= requiredConsecutiveHits -> AlertTrigger.SMOKE
            else -> null
        }
    }

    fun reset() {
        consecutiveFireHits = 0
        consecutiveSmokeHits = 0
    }
}
