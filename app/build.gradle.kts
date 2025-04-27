plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Aplicamos Kapt aquí
    id("androidx.navigation.safeargs.kotlin") // Aplicamos Safe Args aquí
}

android {
    namespace = "com.example.gymtrackerviews" // Tu paquete
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gymtrackerviews" // Tu paquete
        minSdk = 24
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
    }
    // Configuración Kapt (opcional, pero buena práctica para Room)
    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}

dependencies {
    // Variables de versión (usa las últimas estables compatibles)
    val room_version = "2.6.1"
    val lifecycle_version = "2.7.0"
    val coroutines_version = "1.7.3"
    val navigation_version = "2.7.7" // Asegúrate que coincide con el plugin
    val appcompat_version = "1.6.1"
    val core_ktx_version = "1.12.0"
    val material_version = "1.11.0"
    val constraint_layout_version = "2.1.4"
    val fragment_ktx_version = "1.6.2" // Necesaria para by viewModels

    // Core & UI
    implementation("androidx.core:core-ktx:$core_ktx_version")
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("com.google.android.material:material:$material_version")
    implementation("androidx.constraintlayout:constraintlayout:$constraint_layout_version")
    implementation("androidx.fragment:fragment-ktx:$fragment_ktx_version")

    // Room (Base de datos)
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    // Lifecycle (ViewModel, LiveData, LifecycleScope, SavedStateHandle)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:$navigation_version")
    implementation("androidx.navigation:navigation-ui-ktx:$navigation_version")

    // Dependencias de Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
