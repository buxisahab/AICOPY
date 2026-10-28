# Add project specific ProGuard rules here.


# ----------------------------
# Kotlin Serialization
# ----------------------------
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault


# ----------------------------
# Ignore Warnings (SSL + Security libs)
# ----------------------------
-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.impl.StaticLoggerBinder


# ----------------------------
# NewPipe Extractor (MOST IMPORTANT)
# ----------------------------
-keep class org.schabi.newpipe.** { *; }
-dontwarn org.schabi.newpipe.**

# Mozilla Rhino JS Engine (required for YouTube cipher decode)
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

-keep class org.mozilla.javascript.engine.** { *; }
-dontwarn org.mozilla.javascript.engine.**

-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn org.mozilla.javascript.tools.**

# jdk dynalink (Rhino dependency)
-keep class jdk.dynalink.** { *; }
-dontwarn jdk.dynalink.**

# javax.script (Rhino dependency)
-keep class javax.script.** { *; }
-dontwarn javax.script.**


# ----------------------------
# OkHttp (Network Client)
# ----------------------------
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okhttp3.internal.**

# ----------------------------
# Jsoup (HTML parser)
# ----------------------------
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Gson removed - not used in project

# ----------------------------
# Kotlin Coroutines
# ----------------------------
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**


# ----------------------------
# Logging Rules - Remove all debug logs in release
# ----------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Remove Timber logs in release
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
    public static void e(...);
}


# ----------------------------
# Android Gradle Plugin Auto-warnings
# ----------------------------
-dontwarn java.beans.**


# ----------------------------
# FFmpeg Kit (CRITICAL)
# ----------------------------
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**
-keep class com.arthenica.smartexception.** { *; }
-dontwarn com.arthenica.smartexception.**

# Keep all native methods (required for JNI)
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}


# ----------------------------
# TensorFlow Lite (AI/DSP Engine)
# ----------------------------
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.**

# Specific rules from R8 missing_rules.txt (auto-generated)
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options$GpuBackend
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options


# ----------------------------
# Media3 / ExoPlayer
# ----------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }

# ----------------------------
# Firebase Auth & Firestore
# ----------------------------
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.firestore.**
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.common.** { *; }


# ----------------------------
# Room Database
# ----------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** sInstance;
}


# ----------------------------
# Hilt / Dagger
# ----------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-dontwarn dagger.hilt.**


# ----------------------------
# Ktor Client
# ----------------------------
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }


# ----------------------------
# Coil Image Loading
# ----------------------------
-keep class coil.** { *; }
-dontwarn coil.**


# ----------------------------
# Multidex
# ----------------------------
-keep class androidx.multidex.** { *; }
-dontwarn androidx.multidex.**


# ----------------------------
# Keep data classes and serializable classes
# ----------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# ----------------------------
# Reflection (Keep all app models/data classes)
# ----------------------------
-keep class com.musifly.android.models.** { *; }
-keep class com.musifly.android.db.entities.** { *; }


# ----------------------------
# Apache Commons Lang3
# ----------------------------
-keep class org.apache.commons.lang3.** { *; }
-dontwarn org.apache.commons.lang3.**


# ----------------------------
# Google Guava
# ----------------------------
-keep class com.google.common.** { *; }
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**

# ----------------------------
# Firebase & Protobuf (CRITICAL for Firestore)
# ----------------------------
# Keep all Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep all Protobuf classes (CRITICAL - Firestore depends on these)
-keep class com.google.protobuf.** { *; }
-keepclassmembers class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# Keep all Google type classes (includes com.google.type.LatLng)
-keep class com.google.type.** { *; }
-keepclassmembers class com.google.type.** { *; }
-dontwarn com.google.type.**

# Keep Firestore classes
-keep class com.google.firestore.** { *; }
-dontwarn com.google.firestore.**

# Explicitly keep protobuf well-known types (required by Firestore)
-keep class com.google.type.LatLng { *; }
-keep class com.google.type.LatLng$* { *; }
-keep class com.google.protobuf.Value { *; }
-keep class com.google.protobuf.Timestamp { *; }
-keep class com.google.protobuf.Any { *; }
-keep class com.google.protobuf.MessageLite { *; }
-keep class com.google.protobuf.MessageLiteOrBuilder { *; }
-keep class com.google.protobuf.GeneratedMessageLite { *; }
-keep class com.google.protobuf.GeneratedMessageV3 { *; }

# Keep Firebase Auth classes
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Keep Google Play Services classes
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**


# ----------------------------
# Compose (Keep important classes, allow optimization)
# ----------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

# Allow optimization of unused Compose code (moved to optimization section)


# ----------------------------
# Keep all enum classes
# ----------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# ----------------------------
# Additional dontwarn for common issues
# ----------------------------
-dontwarn javax.lang.model.element.Modifier
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn java.lang.ClassValue

# ----------------------------
# Aggressive Optimization Settings
# ----------------------------
# Enable aggressive code shrinking
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove unused code aggressively
-dontpreverify

# Inline methods where possible
-optimizations !code/simplification/arithmetic

# Remove unused parameters and optimize
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}


# ----------------------------
# Dex2C Compatibility Rules
# CRITICAL: These classes MUST remain in DEX (NOT Dex2C processed)
# ----------------------------

# Application class - MUST stay in DEX
-keep class com.musifly.android.App { *; }
-keep class com.musifly.android.App$* { *; }

# All Activities - MUST stay in DEX
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }
-keep class com.musifly.android.MainActivity { *; }
-keep class com.musifly.android.MainActivity$* { *; }

# All Services - MUST stay in DEX
-keep class * extends android.app.Service { *; }
-keep class * extends androidx.media3.session.MediaLibraryService { *; }
-keep class com.musifly.android.playback.MusicService { *; }
-keep class com.musifly.android.playback.MusicService$* { *; }
-keep class com.musifly.android.download.DownloadManagerService { *; }
-keep class com.musifly.android.download.DownloadManagerService$* { *; }
-keep class com.musifly.android.streaming.StreamingPlayerService { *; }
-keep class com.musifly.android.streaming.StreamingPlayerService$* { *; }
-keep class com.musifly.android.playback.ExoDownloadService { *; }
-keep class com.musifly.android.playback.ExoDownloadService$* { *; }
-keep class com.musifly.android.sync.firestore.FirestoreLikesService { *; }
-keep class com.musifly.android.sync.firestore.FirestorePlaylistService { *; }
-keep class com.musifly.android.sync.firestore.FirestoreAutoInitService { *; }
-keep class com.musifly.android.auth.AuthService { *; }
-keep class com.musifly.android.utils.PlaylistImportExportService { *; }

# All BroadcastReceivers - MUST stay in DEX
-keep class * extends android.content.BroadcastReceiver { *; }

# All ViewModels - MUST stay in DEX (use coroutines, lifecycle)
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.musifly.android.viewmodels.** { *; }

# MediaSession - MUST stay in DEX
-keep class androidx.media3.session.** { *; }
-keep class com.musifly.android.playback.MediaLibrarySessionCallback { *; }
-keep class com.musifly.android.playback.MediaLibrarySessionCallback$* { *; }

# ExoPlayer / Media3 - MUST stay in DEX
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.datasource.** { *; }

# Jetpack Compose UI - MUST stay in DEX
-keep class androidx.compose.** { *; }
-keep class com.musifly.android.ui.** { *; }

# Coroutines - MUST stay in DEX
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }

# Secure Wrapper - MUST stay in DEX (bridges to Dex2C-safe code)
-keep class com.musifly.android.secure.SecureWrapper { *; }
-keep class com.musifly.android.secure.SecureWrapper$* { *; }

# Dex2C-safe package - CAN be Dex2C processed, but keep symbols for linking
# These classes contain pure logic and can be converted to native C
-keep class com.musifly.android.secure.StringDecode { *; }
-keep class com.musifly.android.secure.StringDecode$* { *; }
-keep class com.musifly.android.secure.UrlBuilder { *; }
-keep class com.musifly.android.secure.UrlBuilder$* { *; }

# Keep all public methods in secure package for Dex2C symbol resolution
-keepclassmembers class com.musifly.android.secure.** {
    public *;
}

# Innertube Utils - Dex2C-safe pure logic functions
# These functions are pure string manipulation and can be Dex2C processed
# Keep symbols for linking but allow Dex2C conversion
-keep class com.musifly.innertube.utils.UtilsKt { *; }
-keepclassmembers class com.musifly.innertube.utils.UtilsKt {
    public static java.lang.String nSigDecode(java.lang.String);
    public static java.lang.String sigDecode(java.lang.String);
    public static java.lang.String createUrl(java.lang.String, java.lang.String);
}

# Reflection usage - keep classes that use reflection
-keep class com.musifly.android.utils.ReflectionUtils { *; }

# Hilt / Dagger - MUST stay in DEX (uses reflection)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room Database - MUST stay in DEX (uses reflection, annotation processing)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# DataStore - MUST stay in DEX (uses coroutines, Context)
-keep class androidx.datastore.** { *; }

# Firebase - MUST stay in DEX (uses reflection, JNI)
-keep class com.google.firebase.** { *; }

# Threading - keep thread-related classes
-keep class java.util.concurrent.** { *; }

# JNI / Native - keep all native method signatures
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep all classes that extend Android framework classes
-keep class * extends android.** { *; }

# Keep all classes in packages that use Android Context
-keep class com.musifly.android.** { 
    <init>(...);
}

# But allow Dex2C processing of secure package (pure logic only)
# The secure package should NOT use Context, coroutines, or Android APIs
