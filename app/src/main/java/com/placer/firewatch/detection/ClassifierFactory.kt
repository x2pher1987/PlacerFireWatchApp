package com.placer.firewatch.detection

import android.content.Context
import android.util.Log
import java.io.IOException

object ClassifierFactory {
    private const val TAG = "ClassifierFactory"

    fun create(context: Context): Classifier {
        val modelExists = try {
            context.assets.open(TFLiteClassifier.MODEL_FILE).use { true }
        } catch (e: IOException) {
            false
        }

        if (!modelExists) {
            Log.i(TAG, "No trained model found in assets, using heuristic classifier")
            return HeuristicClassifier()
        }

        return try {
            val tflite = TFLiteClassifier(context)
            if (tflite.isReady) tflite else HeuristicClassifier()
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to heuristic classifier", e)
            HeuristicClassifier()
        }
    }
}
