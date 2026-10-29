plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
}

android {
    namespace = "com.example.mimusica"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.mimusica"
        // Propuesta: 26 (ver ARCHITECTURE.md §22). Sujeta a validación en FASE 10.
        minSdk = 26
        // Conservador: 36 (ver ARCHITECTURE.md §22). Fija targetSdk explícito (no heredado de compileSdk).
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Room: exporta el esquema a /schemas para migraciones futuras (doc oficial Room).
// Nota: con Kotlin integrado de AGP 9.4 el accesor "room { }" no se genera;
// se configura la extensión del plugin de Room explícitamente por su tipo.
configure<androidx.room3.gradle.RoomExtension> {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    // Persistencia local: Room 3 (KMP) con SQLite embebido
    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.androidx.sqlite.bundled)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // Tests JVM: variante desktop del driver embebido (el artifact Android
    // intenta cargar una librería .so nativa de Android que la JVM no incluye).
    testRuntimeOnly(libs.androidx.sqlite.bundled.jvm)
}
