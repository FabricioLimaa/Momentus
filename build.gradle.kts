// ARQUIVO: build.gradle.kts (na raiz do projeto)

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    // CORREÇÃO: Usando o alias para resolver o conflito de versão
    alias(libs.plugins.google.gms.services) apply false
    // ADICIONADO: Plugin do Hilt
    alias(libs.plugins.dagger.hilt.android) apply false
    // ADICIONADO: Plugin de serialização do Kotlin
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}
