# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-ignorewarnings
-printmapping mapping.txt

-dontwarn android.support.v4.**
# 保留support下的所有类及其内部类
-keep class android.support.** {*;}

# 保留继承的
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.support.annotation.**

-keep class android.content.pm.*{*;}
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Appliction
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class com.android.vending.licensing.ILicensingService

-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

-keep class **.R$* {*;}
-keep class **.R{*;}

-keep class xyz.eulix.space.bean.** {*;}

-keep class xyz.eulix.space.interfaces.EulixKeep
-keep class * extends xyz.eulix.space.interfaces.EulixKeep {*;}

-keep class java.* {*;}
-keep class java.* {*;}

-keep class org.xmlpull.v1.** { *;}

-dontwarn org.xmlpull.v1.**

#---EventBus start---
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# And if you use AsyncExecutor:
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
#---EventBus end---

#---Glide start---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
#noinspection ShrinkerUnresolvedReference
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}
#---Glide end---

#---Zxing---
-keep class com.google.zxing.client.android.** {*;}
#---Zxing---


-keepclassmembers class xyz.eulix.space.ui.EulixWebViewActivity$EulixJavascriptInterface {
   public *;
}

-keepclassmembers class xyz.eulix.space.ui.authorization$GranteeJavascriptInterface {
   public *;
}

-dontwarn com.google.*.*
-keep class com.google.** {*;}
-keep class android.**{*;}
-keep class org.* {*;}

-keep class com.google.protobuf.** { *; }
-keep class * extends com.google.protobuf.** { *; }


-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class **.R$* {
    public static <fields>;
}
-keepclasseswithmembers class * {     # 保持 native 方法不被混淆
    native <methods>;
}
-keepclassmembers enum * {
    **[] $VALUES;
    public *;
}

-keepclassmembers enum * {                  # 保持枚举 enum 类不被混淆
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

-keep class * implements android.os.Parcelable {    # 保持Parcelable不被混淆
  public static final android.os.Parcelable$Creator *;
}

## ---------Retrofit混淆方法 start---------------
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
# OkHttp3
-dontwarn okhttp3.logging.**
-keep class okhttp3.internal.**{*;}
-dontwarn okio.**
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
# RxJava RxAndroid
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# Gson
-keep class com.google.gson.stream.** { *; }
-keepattributes EnclosingMethod

# Gson
#-keep class com.demo.demo1.service.bean.**{*;} # 自定义数据模型的bean目录
## ---------Retrofit混淆方法 end---------------

# bouncycastle
-keep class org.bouncycastle.** { *; }

#---Lottie start---
-keep class com.airbnb.lottie.samples.** { *; }
#---Lottie end---

#---bugly start---
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.** {*;}
#---bugly end---

#---smart refreshLayout start---
-keep class com.scwang.** {*;}
#---smart refreshLayout end---

#---pdf viewer start---
-keep class com.shockwave.**
#---pdf viewer end---

#---office viewer start---
-keep class com.wxiwei.office.** {*;}
#---office viewer end---

-keep class xyz.eulix.space.applet.bridge.NativeMethodsLib {*;}


