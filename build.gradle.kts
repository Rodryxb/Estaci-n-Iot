// Archivo de configuración de nivel superior (Root Build Gradle)
// Declara los plugins globales (Android Application, Kotlin y Jetpack Compose) sin aplicarlos en la raíz
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
