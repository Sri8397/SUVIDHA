# Default ProGuard/R8 rules for Suvidha
# -------------------------------------------

# Keep Kotlin metadata for reflection
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep data classes used with Firestore/serialization
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}
-keep class com.just_sri.suvidha.expenses.** { *; }

# Keep native libraries
-keep class * { native <methods>; }

# OkHttp / WebSocket
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Crypto / secp256k1
-keep class fr.nickel.secp256k1.** { *; }
-keep class org.bitcoin.** { *; }
