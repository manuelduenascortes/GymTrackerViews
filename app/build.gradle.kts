plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Plugin para procesar anotaciones de Room
    id("androidx.navigation.safeargs.kotlin") // Plugin para Safe Args
    // id("com.google.devtools.ksp") // Descomentar si cambias a KSP en lugar de Kapt para Room
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
    // Necesario si usas Kapt o KSP
    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    /* // Alternativa si usas KSP en lugar de Kapt para Room
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    */
}

dependencies {
    // Definimos variables para versiones (ajústalas si es necesario)
    val room_version = "2.6.1"
    val lifecycle_version = "2.7.0"
    val coroutines_version = "1.7.3"
    val navigation_version = "2.7.7"
    val appcompat_version = "1.6.1"
    val core_ktx_version = "1.12.0"
    val material_version = "1.11.0"
    val constraint_layout_version = "2.1.4"

    // Core & UI
    implementation("androidx.core:core-ktx:$core_ktx_version")
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("com.google.android.material:material:$material_version")
    implementation("androidx.constraintlayout:constraintlayout:$constraint_layout_version")
    implementation("androidx.fragment:fragment-ktx:1.6.2") // Necesaria para 'by viewModels'

    // Room (Base de datos)
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Extensiones Kotlin (Flow, suspend)
    kapt("androidx.room:room-compiler:$room_version")     // Procesador de anotaciones con Kapt
    // implementation("androidx.room:room-compiler-processing:$room_version") // Descomentar si usas KSP
    // ksp("androidx.room:room-compiler:$room_version") // Descomentar si usas KSP

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    // Lifecycle (ViewModel, LiveData, LifecycleScope, repeatOnLifecycle)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version") // Para SavedStateHandle

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:$navigation_version")
    implementation("androidx.navigation:navigation-ui-ktx:$navigation_version")

    // Dependencias de Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}