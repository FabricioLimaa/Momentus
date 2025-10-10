// ARQUIVO: build.gradle.kts (na raiz do projeto)

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.3" apply false
    // ADICIONADO: Plugin do Hilt
    alias(libs.plugins.dagger.hilt.android) apply false

}
