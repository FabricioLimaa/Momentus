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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES" // Linha que resolve o seu erro
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
    }
}

dependencies {
    // Declaração de variável para a versão do Room
    val room_version = "2.6.1"

    // Bibliotecas Padrão e de UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx) // Garante que temos o KTX para fragmentos

    // ViewModel e LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Biblioteca de Gráficos
    implementation(libs.mpandroidchart) // <-- ADICIONE ESTA LINHA

    // Room (Banco de Dados)
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

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
