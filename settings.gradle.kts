// ARQUIVO: settings.gradle.kts (CÓDIGO COMPLETO E CORRIGIDO)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Sintaxe explícita para Kotlin DSL
        maven("https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Adicionamos aqui também para garantir, caso alguma dependência precise
        maven("https://jitpack.io")
    }
}

rootProject.name = "Momentus"
include(":app")