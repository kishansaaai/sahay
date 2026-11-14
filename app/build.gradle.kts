plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sahay.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sahay.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        // No INTERNET permission — on-device only (see B02, README rule #4)
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        // Prompt templates (content/prompts) and model_checksums.txt are read from assets; the model itself never ships in the APK.
        getByName("main") {
            assets.srcDirs("../content", "src/main/assets")
            if (providers.gradleProperty("sahay.executorch").isPresent) java.srcDir("src/executorch/java")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlinx.coroutines.android)
    implementation(projects.engine)

    // ExecuTorch runtime (A04). Opt in with -Psahay.executorch=true once A01 has pinned the tag; see docs/setup-runtime.md.
    if (providers.gradleProperty("sahay.executorch").isPresent) {
        implementation("org.pytorch:executorch-android:${providers.gradleProperty("sahay.executorchVersion").getOrElse("0.7.0")}")
    }
}

// Bundle releases/CHECKSUMS.txt as an asset so the app can verify the model at load (A19).
val copyChecksums by tasks.registering(Copy::class) {
    from(rootProject.file("releases/CHECKSUMS.txt"))
    rename { "model_checksums.txt" }
    into(layout.buildDirectory.dir("generated/assets/checksums"))
}
android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/assets/checksums"))
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach { dependsOn(copyChecksums) }
