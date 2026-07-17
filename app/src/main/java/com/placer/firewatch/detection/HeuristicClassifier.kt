package com.placer.firewatch.detection

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Works out of the box with no trained model. Downsamples each frame to a
 * small grid and scores it against known HSV color signatures for open
 * flame (warm hue, high saturation/brightness) and diffuse smoke (low
 * saturation, mid-brightness gray/white). This rule-based approach is a
 * real, if approximate, baseline used in early fire-detection research —
 * it trades accuracy for needing zero setup.
 *
 * DetectionTracker (see that file) requires several consecutive positive
 * frames before it will fire an alert, which filters out most one-off false
 * positives (sunset light, a passing headlight, a orange shirt).
 *
 * For meaningfully better accuracy, add a trained TensorFlow Lite model —
 * see TFLiteClassifier and the README for how (Google's Teachable Machine
 * is the easiest no-code route).
 */
class HeuristicClassifier : Classifier {

    private val gridSize = 48
    private val hsv = FloatArray(3)

    override fun classify(bitmap: Bitmap): DetectionResult {
        val scaled = Bitmap.createScaledBitmap(bitmap, gridSize, gridSize, true)
        var firePixels = 0
        var smokePixels = 0
        val total = gridSize * gridSize

        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                val pixel = scaled.getPixel(x, y)
                Color.colorToHSV(pixel, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                val isFireHue = hue <= 45f || hue >= 335f
                if (isFireHue && sat > 0.45f && value > 0.55f) {
                    firePixels++
                }

                val isSmokeTone = sat < 0.12f && value in 0.25f..0.85f
                if (isSmokeTone) {
                    smokePixels++
                }
            }
        }

        if (scaled !== bitmap) scaled.recycle()

        return DetectionResult(
            fireConfidence = firePixels.toFloat() / total,
            smokeConfidence = smokePixels.toFloat() / total
        )
    }
}
