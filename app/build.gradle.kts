import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.gms.services)
    alias(libs.plugins.dagger.hilt.android)
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.jetbrainsCompose)
}

// Força a resolução de conflitos de dependências do Compose
configurations.all {
    resolutionStrategy {
        force("androidx.compose.runtime:runtime:1.6.7")
        force("androidx.compose.foundation:foundation:1.6.7")
        force("androidx.compose.foundation:foundation-layout:1.6.7")
        force("androidx.compose.animation:animation:1.6.7")
    }
}

// 1. Carrega as propriedades do keystore de forma segura
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "br.com.fabriciolima.momentus"
    compileSdk = 36

    // Configuração de assinatura para release com os dados corretos
    signingConfigs {
        create("release") {
            storeFile = if (keystoreProperties.getProperty("storeFile") != null) {
                rootProject.file(keystoreProperties.getProperty("storeFile"))
            } else {
                null
            }
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    defaultConfig {
        applicationId = "br.com.fabriciolima.momentus"
        minSdk = 26
        targetSdk = 36
        versionCode = 35
        versionName = "1.3.71"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk.debugSymbolLevel = "FULL"

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/license.md"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

// Adicionando o argumento do KSP para o Room no nível superior
ksp {
    arg("room.schemaDirectory", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.androidx.fragment.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler) // ADICIONADO: Processador para o Worker do Hilt

    // Play Core & In-App Updates
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.google.android.play:integrity:1.3.0")

    // Glance for App Widgets
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material)
    implementation(libs.androidx.glance.material3)

    // DataStore
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.rxjava2)
    implementation(libs.androidx.datastore.preferences.rxjava3)
    implementation(libs.androidx.datastore.rxjava2)
    implementation(libs.androidx.datastore.rxjava3)

    implementation(libs.androidx.graphics.shapes)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // ADICIONADO: Window Size Class para layouts adaptativos
    implementation("androidx.compose.material3:material3-window-size-class")

    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.constraintlayout.compose) // Adicionado
    implementation("androidx.navigation:navigation-compose:2.7.7") // ADICIONADO

    implementation(libs.composeReorderable)

    // Coil para carregamento de imagens
    implementation(libs.coilCompose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-inappmessaging-display:22.0.2")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation(libs.playServicesAuth)

    implementation(libs.googleApiClientAndroid) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.googleApiServicesCalendar) {
        exclude(group = "org.apache.httpcomponents")
    }

    implementation(libs.kizitonwoseCalendarCompose)

    // Lottie
    implementation(libs.lottie.compose)

    // WorkManager
    implementation(libs.androidx.work.manager)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin) // Adicionado para os testes
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
