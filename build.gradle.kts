// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Asegúrate que la versión de AGP es compatible con tu Android Studio
    id("com.android.application") version "8.2.2" apply false
    // Asegúrate que la versión de Kotlin es compatible
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // Plugin Safe Args (verifica la versión, debe coincidir con las dependencias de navigation)
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
    // Plugin Kapt (necesario para Room)
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false // Usa la misma versión que kotlin.android
    id("com.google.gms.google-services") version "4.4.2" apply false
}
