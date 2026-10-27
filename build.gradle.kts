// Top-level build: los plugins se aplican en el módulo :app
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
