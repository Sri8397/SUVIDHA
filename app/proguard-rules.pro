# ProGuard/R8 rules for Suvidha Expense
# -------------------------------------------

# Preserve annotations and signatures required for generics / coroutines
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Room Database, Entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Native methods (JNI) & secp256k1 crypto library
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class fr.acinq.secp256k1.** { *; }
-dontwarn fr.acinq.secp256k1.**

# OkHttp & Okio (consumer rules are in library, suppress benign platform warnings)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Marmot / Nostr protocol
-dontwarn com.vitorpamplona.**
