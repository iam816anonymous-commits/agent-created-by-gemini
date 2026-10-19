# LocalAgent Proguard Rules for Shrinking & Optimization

# Room Persistence
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.data.local.** { *; }

# Moshi JSON Serialization
-keepclassmembers class com.example.core.model.** { *; }
-keep class com.example.core.model.** { *; }
-keep class com.example.data.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json(name=*) <fields>;
}

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Android Accessibility Service & System Overlays
-keep class com.example.service.LocalAgentAccessibilityService { *; }
-keep class com.example.service.LocalAgentOverlayService { *; }
-keep class com.example.MainActivity { *; }

# JNI & Native Inference Bridge
-keepclasseswithmembernames class * {
    native <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
