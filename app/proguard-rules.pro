# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /proguard-android-optimize.txt

# 保留 Compose 相关
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 保留 Kotlin 协程
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 保留 MediaSession 回调
-keep class android.media.session.** { *; }

# 保留 NotificationListenerService
-keep class android.service.notification.** { *; }

# 保留动态代理
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses, EnclosingMethod