# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in core/consumer-rules.pro
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI bridge classes
-keep class com.github.yumelira.yumebox.core.bridge.** { *; }
-keep class com.github.yumelira.yumebox.core.Clash { *; }

# Keep data models for serialization
-keep class com.github.yumelira.yumebox.core.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

-dontwarn kotlinx.serialization.**

