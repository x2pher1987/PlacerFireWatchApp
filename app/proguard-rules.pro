# Add project specific ProGuard rules here.
# Default rules are applied via proguard-android-optimize.txt.
# Keep TensorFlow Lite GPU/NNAPI delegate classes if you later enable them.
-keep class org.tensorflow.lite.** { *; }
