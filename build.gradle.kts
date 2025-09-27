// ARQUIVO: build.gradle.kts (na raiz do projeto)

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
}