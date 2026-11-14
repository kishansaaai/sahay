import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
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

    val signingValues = listOf("SAHAY_KEYSTORE", "SAHAY_KEYSTORE_PASSWORD", "SAHAY_KEY_ALIAS", "SAHAY_KEY_PASSWORD")
        .associateWith { providers.environmentVariable(it).orNull }
    val releaseSigning = if (signingValues.values.all { !it.isNullOrBlank() }) signingConfigs.create("distribution") {
        storeFile = file(signingValues.getValue("SAHAY_KEYSTORE")!!)
        storePassword = signingValues.getValue("SAHAY_KEYSTORE_PASSWORD")
        keyAlias = signingValues.getValue("SAHAY_KEY_ALIAS")
        keyPassword = signingValues.getValue("SAHAY_KEY_PASSWORD")
    } else null

    buildTypes {
        release {
            signingConfig = releaseSigning
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

    testOptions { unitTests.isIncludeAndroidResources = true }

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

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.zxing.core)
    implementation(libs.mlkit.latin)
    implementation(libs.mlkit.devanagari)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
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

// AGP wires the generator into all asset consumers, including lint and unit tests.
abstract class ChecksumAssetsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val checksumFile: RegularFileProperty
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    @TaskAction fun generate() {
        val destination = outputDir.file("model_checksums.txt").get().asFile
        destination.parentFile.mkdirs()
        checksumFile.get().asFile.copyTo(destination, overwrite = true)
    }
}
val copyChecksums by tasks.registering(ChecksumAssetsTask::class) {
    checksumFile.set(rootProject.layout.projectDirectory.file("releases/CHECKSUMS.txt"))
    outputDir.set(layout.buildDirectory.dir("generated/assets/checksums"))
}
androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyChecksums, ChecksumAssetsTask::outputDir)
    }
}
