# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keep class com.example.burpeelog.data.model.** { *; }
-keep class com.example.burpeelog.data.dao.** { *; }
-keep class com.example.burpeelog.data.database.** { *; }
-dontwarn com.google.errorprone.annotations.**
