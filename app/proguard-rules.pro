# Add project specific ProGuard rules here.
# ModLU 1APK BROWSER X - GeckoView & Room Release ProGuard Configuration

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Mozilla GeckoView & Gecko native JNI bridge classes
-keep class org.mozilla.gecko.** { *; }
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.gecko.**
-dontwarn org.mozilla.geckoview.**

# Preserve Room entities and generated implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
