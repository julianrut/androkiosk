-keepclassmembers class com.androkiosk.sequence.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.internal.** { *; }

-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
