plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "br.com.fabriciolima.momentus"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.fabriciolima.momentus"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        // --- MODIFICAÇÃO INICIA AQUI ---
        // 1. Habilitamos o Jetpack Compose no projeto.
        compose = true
        // --- MODIFICAÇÃO TERMINA AQUI ---
    }
    // --- MODIFICAÇÃO INICIA AQUI ---
    // 2. Definimos a versão do compilador do Compose.
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES" // Linha que resolve o seu erro
        }
    }
}

dependencies {
    // Declaração de variável para a versão do Room
    val room_version = "2.6.1"
    val compose_bom_version = "2024.05.00"

    // Core e UI (Views)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Componentes de Arquitetura (ViewModel, LiveData)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.2")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:$compose_bom_version"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.compose.runtime:runtime-livedata") // <-- BIBLIOTECA ADICIONADA

    // Room (Banco de Dados)
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Biblioteca de Gráficos
    //implementation(libs.mpandroidchart) // <-- ADICIONE ESTA LINHA

    // Room (Banco de Dados)
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // Google Sign-In
    implementation(libs.play.services.auth)

    // --- CONJUNTO COMPLETO E CORRETO DAS BIBLIOTECAS DA API DO GOOGLE ---
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    // As dependências que estavam faltando e causando os erros de "unresolved reference"
    implementation("com.google.api-client:google-api-client-gson:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.oauth-client:google-oauth-client:1.34.1")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    // --- FIM DO CONJUNTO DE BIBLIOTECAS DO GOOGLE ---

    // --- SEÇÃO DE TESTES ---
    // As duas primeiras geralmente já vêm com o projeto.
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Testes
    testImplementation(libs.androidx.core.testing) // <-- GARANTA QUE ESTA LINHA EXISTA
    testImplementation(libs.kotlinx.coroutines.test)

    // --- MODIFICAÇÃO: ADICIONE AS BIBLIOTECAS ABAIXO ---
    // Para testes de componentes de arquitetura (como ViewModel e LiveData)
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    // Para nos ajudar a trabalhar com Coroutines em testes
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
}

// AQUI ESTÁ A MUDANÇA: Usando os arquivos locais
//implementation(files("libs/google-api-client-android-2.2.0.jar"))
//implementation(files("libs/google-api-services-calendar-v3-rev20220715-2.0.0.jar"))
//implementation(files("libs/google-api-client-2.2.0.jar"))
//implementation(files("libs/google-http-client-1.44.2.jar"))
// implementation(files("libs/gson-2.13.2.jar"))

// Google APIs
// COMENTE AS LINHAS ABAIXO POR ENQUANTO
//     implementation(libs.google.api.client.android) {
//          exclude(group = "org.apache.httpcomponents")
//      }
//      implementation(libs.google.api.services.calendar) {
//          exclude(group = "org.apache.httpcomponents")
//      }