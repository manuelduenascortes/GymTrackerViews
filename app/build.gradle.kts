plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Plugin para procesar anotaciones de Room
}

android {
    namespace = "com.example.gymtrackerviews" // Tu nombre de paquete
    compileSdk = 34 // O la versión que tengas

    defaultConfig {
        applicationId = "com.example.gymtrackerviews" // Tu nombre de paquete
        minSdk = 24 // O la versión que elegiste
        targetSdk = 34 // Suele coincidir con compileSdk
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
    }
}

dependencies {
    // Definimos una variable para la versión de Room (usa la última estable)
    val room_version = "2.6.1" // Puedes buscar la última versión estable de Room

    // Dependencias Core y Material
    implementation("androidx.core:core-ktx:1.12.0") // Verifica versión
    implementation("androidx.appcompat:appcompat:1.6.1") // Verifica versión
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Verifica versión

    // --- Dependencias de Room AÑADIDAS ---
    implementation("androidx.room:room-runtime:$room_version") // Librería principal de Room
    implementation("androidx.room:room-ktx:$room_version")     // Extensiones Kotlin para Room (Coroutines, Flow)
    kapt("androidx.room:room-compiler:$room_version")          // Procesador de anotaciones de Room (necesario con kapt)

    // --- Dependencias de Coroutines (útiles con Room KTX y Flow) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Verifica versión
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Verifica versión

    // --- Dependencias de Lifecycle (útiles con Coroutines y ViewModels más adelante) ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Verifica versión

    // Dependencias de Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}