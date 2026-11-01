# ProGuard rules untuk WA Autopilot
# Default Android rules sudah cukup untuk MVP ini

# Keep Retrofit models
-keep class com.waautopilot.app.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
