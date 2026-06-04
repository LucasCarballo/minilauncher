# Add project specific ProGuard rules here.

# Kotlinx collections immutable
-keep class kotlinx.collections.immutable.** { *; }
-dontwarn kotlinx.collections.immutable.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Compose
-dontwarn androidx.compose.**