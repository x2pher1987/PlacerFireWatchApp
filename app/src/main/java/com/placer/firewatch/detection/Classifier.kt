package com.placer.firewatch.detection

import android.graphics.Bitmap

data class DetectionResult(
    val fireConfidence: Float,
    val smokeConfidence: Float
)

enum class AlertTrigger { FIRE, SMOKE }

interface Classifier {
    fun classify(bitmap: Bitmap): DetectionResult
    fun close() {}
}
