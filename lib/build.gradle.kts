import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.pointlessapps.mica"
version = "0.1.0"

kotlin {
    jvm()
    androidLibrary {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        namespace = group.toString()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "mica", version.toString())

    pom {
        name = "Mica"
        description = "A simple yet powerful interpreted language"
        inceptionYear = "2025"
        url = "https://github.com/pChochura/Mica"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit"
                distribution = "https://opensource.org/license/mit"
            }
        }
        developers {
            developer {
                id = "pChochura"
                name = "Paweł Chochura"
                url = "https://github.com/pChochura"
            }
        }
        scm {
            url = "https://github.com/pChochura/Mica"
            connection = "scm:git:git://github.com/pChochura/Mica.git"
            developerConnection = "scm:git:ssh://git@github.com/pChochura/Mica.git"
        }
    }
}
