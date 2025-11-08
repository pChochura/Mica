plugins {
    alias(libs.plugins.application) apply false
    alias(libs.plugins.library) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinComposeCompiler) apply false
    alias(libs.plugins.detekt)
}

apply(plugin = libs.plugins.detekt.get().pluginId)

detekt {
    debug = true
    buildUponDefaultConfig = true
    ignoreFailures = true
    config.setFrom("$rootDir/config/detekt.yml")
    dependencies {
        detektPlugins(libs.versions.detektFormattingPlugin.get())
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
