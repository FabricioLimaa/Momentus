# ===================================================================
# Proguard Rules for Momentus App (v7 - Final & Focused)
# ===================================================================

# --- Regras Gerais ---
-keepattributes *Annotation*,Signature,InnerClasses

# --- Correção para a colisão de nomes (Hilt / Proguard) ---
# Impede que o Proguard ofusque os nomes das classes de dados (model) e DAOs,
# que são as mais prováveis de causar o erro "Multiple entries with same key".
-keepnames class br.com.fabriciolima.momentus.data.model.** { *; }
-keepnames class br.com.fabriciolima.momentus.data.database.** { *; }

# --- Firebase & Google APIs ---
# Manter as classes principais dos SDKs do Google é crucial.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# GSON (dependência da API do Google)
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName *;
}

# --- Outras dependências ---
-keepclasseswithmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable *;
}
-keep class *$$serializer { *; }

-keep public class * extends androidx.glance.appwidget.action.ActionCallback {
   <init>();
}
