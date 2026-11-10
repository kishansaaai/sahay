plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.sahay.app"
    compileSdk = extra["compileSdk"] as Int

    defaultConfig {
        applicationId = "com.sahay.app"
        minSdk = extra["minSdk"] as Int
        targetSdk = extra["targetSdk"] as Int
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

    composeOptions {
        kotlinCompilerExtensionVersion = extra["kotlinVersion"].toString()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(extra["composeBom"].toString()))
    implementation(androidx.activity.compose.activity)
    implementation(androidx.compose.ui.ui)
    implementation(androidx.compose.ui.uiGraphics)
    implementation(androidx.compose.material.material3)
    implementation(androidx.compose.ui.uiToolingPreview)
    implementation(androidx.lifecycle.lifecycleRuntimeKtx)
    implementation(projects.engine)
}
