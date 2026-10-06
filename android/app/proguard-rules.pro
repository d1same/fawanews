-keep class androidx.media3.** { *; }

# Rhino loads its built-in JavaScript objects by class name.
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn java.beans.**
-dontwarn javax.lang.model.**
-dontwarn jdk.dynalink.**
