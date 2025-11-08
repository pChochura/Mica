import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlinComposeCompiler)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.composeMultiplatform)
}

android {
    compileSdk = libs.versions.targetSdk.get().toInt()
    namespace = "${libs.versions.packageName.get()}.example"

    defaultConfig {
        applicationId = "${libs.versions.packageName.get()}.example"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(JavaVersion.VERSION_17.toString())
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(libs.androidxCore)
    implementation(libs.material)

    implementation(platform(libs.composeBom))
    implementation(libs.composeViewModel)
    implementation(libs.composeMaterial)
    implementation(libs.composeUi)
    implementation(libs.composeUiToolingPreview)

    debugImplementation(libs.composeUiTooling)

    implementation(projects.lib)
}
