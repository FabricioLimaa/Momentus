# ===================================================================
# Proguard Rules for Momentus App (v12 - The Final Fix)
# ===================================================================

# --- Regra de Correção Definitiva para Colisão de Nomes ---
# Impede a ofuscação (renomeação) de TODAS as classes para evitar o erro
# "Multiple entries with same key". A minificação (remoção de código) ainda ocorre.
-keepnames class ** { *; }

# --- Regras Gerais ---
# Manter anotações e outras assinaturas que bibliotecas usam.
-keepattributes *Annotation*,Signature,InnerClasses

# --- Firebase & Google APIs ---
# Manter as classes de bibliotecas externas para evitar que sejam removidas.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# GSON (dependência da API do Google)
# Manter membros que são usados via reflexão.
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers,allowobfuscation class * {
                                     @com.google.gson.annotations.SerializedName *;
                                   }

# --- Regra de Correção para o Crash de `fetchEvents` ---
# Mantém os nomes e construtores de todas as classes de modelo da API do Calendar,
# o que impede o erro "unable to create new instance".
-keep public class com.google.api.services.calendar.model.** {
    public <init>();
    public *;
}

# --- Outras dependências ---
-keepclasseswithmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable *;
}
-keep class *$$serializer { *; }

-keep public class * extends androidx.glance.appwidget.action.ActionCallback {
   <init>();
}