# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep Xposed entry point
-keep class my.shanunid.linespoofer.MainHook { *; }

# Keep all hook classes
-keep class my.shanunid.linespoofer.hooks.** { *; }

# Keep utility classes
-keep class my.shanunid.linespoofer.utils.** { *; }

# Keep data models and serialization classes
-keep class my.shanunid.linespoofer.data.** { *; }
