# kotlinx.serialization — keep generated serializers
-keepclassmembers class it.iterapp.core.** {
    *** Companion;
}
-keepclasseswithmembers class it.iterapp.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class it.iterapp.core.**$$serializer { *; }

# MapLibre Native (JNI surface)
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**
