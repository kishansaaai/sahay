buildscript {
    extra.set("agpVersion", "8.5.2")
    extra.set("kotlinVersion", "2.0.21")
    extra.set("composeBom", "2024.10.00")
    extra.set("compileSdk", 35)
    extra.set("minSdk", 24)
    extra.set("targetSdk", 35)
}

plugins {
    alias(libs.plugins.android.application) version extra["agpVersion"].toString() apply false
    alias(libs.plugins.android.library) version extra["agpVersion"].toString() apply false
    alias(libs.plugins.kotlin.android) version extra["kotlinVersion"].toString() apply false
    alias(libs.plugins.kotlin.multiplatform) version extra["kotlinVersion"].toString() apply false
}

allprojects {
    group = "com.sahay"
    version = "1.0.0"
}

tasks.register("clean") {
    delete(rootProject.buildDir)
}
