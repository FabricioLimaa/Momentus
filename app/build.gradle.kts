plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.gms.services)
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "br.com.fabriciolima.momentus"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.fabriciolima.momentus"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.5.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
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

    // Glance for App Widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.composeReorderable)

    // Coil para carregamento de imagens
    implementation(libs.coilCompose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.firebaseBom))
    implementation(libs.firebaseAnalytics)

    implementation(libs.playServicesAuth)

    implementation(libs.googleApiClientAndroid) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.googleApiServicesCalendar) {
        exclude(group = "org.apache.httpcomponents")
    }
    
    implementation(libs.kizitonwoseCalendarCompose)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
