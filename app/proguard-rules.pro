# ===================================================================
# Proguard Rules for Momentus App
# Refined Security & Obfuscation
# ===================================================================

# --- Obfuscation Strategy ---
# Removed global -keepnames. Now allowing full obfuscation for application logic
# except for classes annotated with @Keep or specific library requirements.

# --- Rules for Hilt/Dagger (Critical for DI) ---
-keep class * { @dagger.hilt.android.AndroidEntryPoint <fields>; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }

# --- Rules for Room (Persistence) ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Rules for Gamification Logic Protection ---
# We want to obfuscate the implementation but keep some entry points if necessary.
# By default, usecases and repositories in br.com.fabriciolima.momentus will be obfuscated.
# This makes it harder to find where points are calculated or achievements unlocked.
-keep class br.com.fabriciolima.momentus.data.model.Achievement { *; }

# --- Firebase & Firestore ---
# Firestore uses reflection to map fields. Most models use @Keep already.
-keepattributes *Annotation*,Signature,InnerClasses
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Google Calendar API ---
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
-keep public class com.google.api.services.calendar.model.** {
    public <init>();
    public *;
}

# --- Kotlin Serialization ---
-keepclasseswithmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable *;
}
-keep class *$$serializer { *; }

# --- Glance (Widgets) ---
-keep public class * extends androidx.glance.appwidget.action.ActionCallback {
   <init>();
}

# --- SQLCipher ---
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**
