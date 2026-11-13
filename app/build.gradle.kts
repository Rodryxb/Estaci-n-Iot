// Configuración del módulo principal de la aplicación (:app)
plugins {
    alias(libs.plugins.android.application) // Plugin para compilar una aplicación Android (APK)
    alias(libs.plugins.kotlin.android)      // Soporte para el lenguaje Kotlin en Android
    alias(libs.plugins.kotlin.compose)      // Compilador de Jetpack Compose para interfaces Material Design 3
}

android {
    // Identificador único del paquete de la aplicación para INACAP TI3042
    namespace = "cl.inacap.estacionmeteorologica"
    compileSdk = 35 // Versión del SDK de Android con la que se compila el proyecto

    defaultConfig {
        applicationId = "cl.inacap.estacionmeteorologica"
        minSdk = 26    // Android 8.0+ (garantiza compatibilidad con más del 95% de dispositivos y APIs criptográficas)
        targetSdk = 35 // Optimizado para Android 15
        versionCode = 1
        versionName = "2.0.0-OT"

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

    // Configuración de Java 17 requerida por Android Gradle Plugin 8+
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // Habilitamos Jetpack Compose como motor moderno de UI declarativa (Material Design 3)
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Librerías núcleo de Android KTX y ciclo de vida (Lifecycle)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Bill of Materials (BOM) de Jetpack Compose y Material Design 3 (Criterio 2.1.1.2)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navegación entre pantallas (Login -> Monitoreo -> Activador -> Seguridad/Interconexión)
    implementation(libs.androidx.navigation.compose)

    // Dependencias de pruebas unitarias e instrumentadas
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
