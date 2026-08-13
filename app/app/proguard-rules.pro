# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for readable crash stack traces, then hide the source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures and annotations required by Gson/Retrofit reflection.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes InnerClasses,EnclosingMethod

# ---------------------------------------------------------------------------
# Retrofit
# https://github.com/square/retrofit
# ---------------------------------------------------------------------------
# Retrofit does reflection on generic parameters and service method annotations.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep the API interface(s) and their annotated methods.
-keep interface com.tomfricks.hook.api.** { *; }

# Retrofit optionally uses the platform Optional/annotation types.
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# With R8 full mode, generic signatures are stripped for classes that are
# themselves kept but not used elsewhere, so keep signatures on service methods.
-keep,allowobfuscation,allowshrinking class retrofit2.Retrofit
-keep,allowobfuscation,allowshrinking class retrofit2.http.**

# ---------------------------------------------------------------------------
# OkHttp
# https://square.github.io/okhttp/
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------------------------------------------------------------------------
# Gson
# https://github.com/google/gson
# ---------------------------------------------------------------------------
# Gson uses reflection; keep its internal helpers and the @SerializedName field.
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# ---------------------------------------------------------------------------
# App model / DTO classes used for Gson (de)serialization.
# These are (de)serialized reflectively, so keep the classes and their fields.
# ---------------------------------------------------------------------------
-keep class com.tomfricks.hook.api.** { *; }

# ---------------------------------------------------------------------------
# Kotlin coroutines
# ---------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepclassmembernames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherLoader
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory
-dontwarn kotlinx.coroutines.**

# ServiceLoader support for coroutines' Main dispatcher.
-keepclassmembers class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# ---------------------------------------------------------------------------
# Kotlin metadata / reflection
# ---------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ---------------------------------------------------------------------------
# Jetpack Compose keeps most of what it needs via consumer rules shipped with
# the AndroidX artifacts; the defaults below cover reflective @Composable use.
# ---------------------------------------------------------------------------
-dontwarn androidx.compose.**

# Enum values() / valueOf() are used reflectively for the preference enums.
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
