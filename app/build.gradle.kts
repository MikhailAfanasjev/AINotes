plugins {
    alias(libs.plugins.compose.compiler)
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("realm-android")
}

android {
    namespace = "com.example.linguareader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.linguareader"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation (libs.ui.tooling.preview) // Для Preview
    implementation (libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.core.ktx)  // Основная библиотека Android KTX
    implementation(libs.androidx.lifecycle.runtime.ktx)  // Жизненный цикл KTX
    implementation(libs.androidx.activity.compose)  // Поддержка компонентов Activity для Compose
    implementation(libs.androidx.constraintlayout.compose)
    implementation(platform(libs.androidx.compose.bom))  // BOM для управления версиями Compose
    implementation(libs.androidx.ui)  // Основной модуль Compose UI
    implementation(libs.androidx.ui.graphics)  // Библиотека графики Compose
    implementation(libs.androidx.ui.tooling.preview)  // Поддержка предпросмотра в Android Studio
    implementation(libs.androidx.material3)  // Material Design 3 для Jetpack Compose
    implementation(libs.material)
    implementation (libs.androidx.hilt.navigation.compose.v100)
    implementation (libs.androidx.foundation)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.appcompat)
    implementation(libs.protolite.well.known.types)
    implementation(libs.androidx.ui.test.android)

    implementation(libs.androidx.core.ktx.v1160)
    implementation (libs.androidx.navigation.compose.v289)
    implementation(libs.androidx.work.runtime.ktx)

    implementation (libs.androidx.datastore.preferences)

    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.androidx.datastore.preferences)

    //Realm
    implementation (libs.realm.android.library)

    //Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.ui.test.android)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // PdfRenderer
    implementation(libs.pdfbox.android)

    implementation(libs.hilt.android.v252)
    implementation (libs.androidx.datastore.preferences)    // ↓ см. примечание
    implementation (libs.androidx.datastore.core.android)  // для core‑API
    kapt (libs.dagger.hilt.android.compiler)
    kapt(libs.hilt.compiler.v252)
    // Gson
    implementation (libs.gson)

    // Тестирование
    testImplementation(libs.junit)  // JUnit для юнит-тестирования
    androidTestImplementation(libs.androidx.junit)  // JUnit для UI тестирования
    androidTestImplementation(libs.androidx.espresso.core)  // Espresso для UI тестов
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)  // JUnit 4 для тестирования UI Compose

    // Дебаг зависимости
    debugImplementation(libs.androidx.ui.tooling)  // Предпросмотр инструментов
    debugImplementation(libs.androidx.ui.test.manifest)  // Манифест для тестирования

    // Retrofit и конвертер Gson для работы с сетью
    implementation(libs.retrofit.v2100)
    implementation(libs.converter.gson.v2100)
    implementation (libs.logging.interceptor)
    implementation (libs.androidx.security.crypto)
}
kapt {
    correctErrorTypes = true
}