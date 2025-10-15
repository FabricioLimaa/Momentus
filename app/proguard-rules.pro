# ===================================================================
# Proguard Rules for Momentus
# ===================================================================

# Manter anotações, que são frequentemente usadas por bibliotecas em tempo de execução.
-keepattributes *Annotation*,Signature,InnerClasses

# --- Regras Gerais do Firebase ---
-keep class com.google.firebase.** { *; }

# --- Regras para Autenticação Firebase & Google Sign-In ---
-keep class com.google.android.gms.auth.api.signin.** { *; }
-dontwarn com.google.android.gms.auth.api.signin.**

# --- Regras para Google API Client e dependências (GSON, etc.) ---
# Esta é a seção mais crítica para corrigir o crash pós-login.

# Cliente principal e modelos da API
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
-keep public class com.google.api.services.calendar.model.** { *; }
-dontwarn com.google.api.services.calendar.model.**

# GSON (usado pela Google API Client para JSON)
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# --- Regras para o Kotlinx Serialization ---
-keepclasseswithmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class *$$serializer { *; }

# --- Regras para o Glance (App Widgets) ---
-keep public class * extends androidx.glance.appwidget.action.ActionCallback {
   <init>();
}

# Manter nomes de classes e membros anotados com @Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
