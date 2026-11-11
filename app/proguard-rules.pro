# Whisk ProGuard / R8 Keep Rules

# Keep Kotlinx Serialization DTOs and Serializers
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}
-keep class kotlinx.serialization.** { *; }

# Keep Room Database Entities and DAOs
-keep class com.whisk.data.local.** { *; }
-dontwarn androidx.room.**

# Keep Domain Data Models & DTOs
-keep class com.whisk.domain.model.** { *; }
-keep class com.whisk.data.repository.** { *; }

# Keep OkHttp & Network Data Models
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep LiteRT-LM, MediaPipe, and ONNX Runtime Natives and Models
-keep class com.google.ai.edge.** { *; }
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**
-dontwarn kotlin.reflect.**
