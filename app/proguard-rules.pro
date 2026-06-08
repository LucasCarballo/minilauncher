# MiniLauncher ProGuard rules

# Kotlinx collections immutable
-keep class kotlinx.collections.immutable.** { *; }
-dontwarn kotlinx.collections.immutable.**

# Hilt — keep generated components and injection targets
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# DataStore — uses protobuf reflection
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Compose — compiler plugin handles most rules automatically
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile **;
}

# AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }
-keep class androidx.activity.** { *; }

# App model classes used in state (must be kept for Compose stability)
-keep class com.minilauncher.data.model.** { *; }