// build.gradle.kts (Module :app - MODIFICADO CON UNA VERSIÓN ANTERIOR DEL FIREBASE BOM)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Para Room
    id("androidx.navigation.safeargs.kotlin") // Para Safe Args
    id("com.google.gms.google-services") // Plugin para Firebase
}

android {
    namespace = "com.example.gymtrackerviews" // Asegúrate que "com.example" sea tu namespace real
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gymtrackerviews" // Y aquí tu applicationId real
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

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}

dependencies {
    // Variables de versión (de tu commit estable)
    val room_version = "2.6.1"
    val lifecycle_version = "2.7.0"
    val coroutines_version = "1.7.3"
    val navigation_version = "2.7.7"
    val appcompat_version = "1.6.1"
    val core_ktx_version = "1.12.0"
    val material_version = "1.11.0"
    val constraint_layout_version = "2.1.4"
    val fragment_ktx_version = "1.6.2"

    // Core & UI
    implementation("androidx.core:core-ktx:$core_ktx_version")
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("com.google.android.material:material:$material_version")
    implementation("androidx.constraintlayout:constraintlayout:$constraint_layout_version")
    implementation("androidx.fragment:fragment-ktx:$fragment_ktx_version")

    // Room
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:$navigation_version")
    implementation("androidx.navigation:navigation-ui-ktx:$navigation_version")

    // MPAndroidChart (para gráficas)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Firebase
    // CAMBIAMOS LA VERSIÓN DEL BOM a una que podría ser más compatible con Kotlin 1.9.22
    implementation(platform("com.google.firebase:firebase-bom:32.8.1")) // <<< VERSIÓN DEL BOM CAMBIADA AQUÍ

    // Dependencia para Firebase Authentication (login y registro)
    implementation("com.google.firebase:firebase-auth-ktx")

    // Dependencia para Firebase Analytics (opcional pero recomendada)
    implementation("com.google.firebase:firebase-analytics-ktx")


    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}