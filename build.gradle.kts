// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false // Verifica/usa tu versión de AGP
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // Verifica/usa tu versión de Kotlin
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false // Verifica/usa tu versión de Navigation
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false // KSP (Alternativa futura a Kapt si se usa)
    // Si no usas KSP, puedes quitar la línea anterior. Si usas Kapt, no necesitas KSP aquí.
}