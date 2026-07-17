package com.placer.firewatch.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Optional upgrade path. Drop a trained image classifier named
 * "fire_smoke_model.tflite" into app/src/main/assets/ and this is used
 * automatically instead of HeuristicClassifier (see ClassifierFactory).
 *
 * Expected model shape: 224x224x3 RGB input, normalized 0-1, outputting
 * 3 class probabilities in the order [normal, smoke, fire]. If your model
 * differs, adjust INPUT_SIZE and the output parsing below to match.
 *
 * Easiest way to get a working model with no ML background: Google's
 * "Teachable Machine" (teachablemachine.withgoogle.com) — train an image
 * classifier from a few hundred photos of fire, smoke, and normal scenes,
 * then export as TensorFlow Lite.
 */
class TFLiteClassifier(context: Context, modelPath: String = MODEL_FILE) : Classifier {

    companion object {
        const val MODEL_FILE = "fire_smoke_model.tflite"
        private const val INPUT_SIZE = 224
        private const val TAG = "TFLiteClassifier"
    }

    private var interpreter: Interpreter? = null

    init {
        try {
            val model = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load $modelPath, classifier unavailable", e)
        }
    }

    val isReady: Boolean get() = interpreter != null

    override fun classify(bitmap: Bitmap): DetectionResult {
        val interp = interpreter ?: return DetectionResult(0f, 0f)
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        return try {
            val input = bitmapToByteBuffer(resized)
            val output = Array(1) { FloatArray(3) }
            interp.run(input, output)
            DetectionResult(fireConfidence = output[0][2], smokeConfidence = output[0][1])
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            DetectionResult(0f, 0f)
        } finally {
            if (resized !== bitmap) resized.recycle()
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            buffer.putFloat((pixel and 0xFF) / 255.0f)
        }
        buffer.rewind()
        return buffer
    }

    override fun close() {
        interpreter?.close()
    }
}
