# ===================================================================
# Proguard Rules for Momentus App (v3 - Comprehensive)
# ===================================================================

# Manter anotações, que são frequentemente usadas por bibliotecas em tempo de execução.
-keepattributes *Annotation*,Signature,InnerClasses

# --- Regras para o Kotlin Coroutines ---
# Impede que o Proguard interfira na forma como as corrotinas funcionam.
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory { 
    public static final kotlinx.coroutines.MainCoroutineDispatcher c;
}

# --- Regras para o Firebase (Auth e Firestore) ---
# Regras oficiais recomendadas pelo Firebase para garantir que o SDK funcione corretamente.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.measurement.** { *; }
-keepnames class com.google.android.gms.common.api.internal.ListenerHolder$ListenerKey
-dontwarn com.google.firebase.**

# --- Regras para o Google API Client e dependências ---
# Esta é a seção mais crítica para corrigir o crash pós-login.

# Cliente principal e modelos da API
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# Regras para GSON (usado pela Google API Client) - REGRAS OFICIAIS
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
