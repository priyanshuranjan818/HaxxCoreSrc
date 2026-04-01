-keep class com.Jagdish.Loader.floating.ESPView { *; }
-keep class com.Jagdish.Loader.floating.Overlay { *; }
-keep class com.Jagdish.Loader.floating.FloatLogo { *; }
-keep class com.Jagdish.Loader.floating { *; }
-keep class com.Jagdish.Loader.activity { *; }
-keep class com.Jagdish.Loader.utils { *; }
-dontobfuscate
-dontoptimize
# Keep SLF4J logging classes
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

