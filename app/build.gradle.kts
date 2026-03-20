import java.io.FileInputStream
import java.util.Properties

plugins {
    id("tokidokiroppou.android.application")
    id("tokidokiroppou.compose")
    id("tokidokiroppou.hilt")
    id("tokidokiroppou.kotlin.serialization")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "blue.starry.tokidokiroppou"

    defaultConfig {
        applicationId = "blue.starry.tokidokiroppou"
        versionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull() ?: 1
        versionName = project.findProperty("versionName")?.toString() ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        sarifReport = true
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["android_keystore_path"] as String)
                storePassword = keystoreProperties["android_keystore_password"] as String
                keyAlias = keystoreProperties["android_keystore_alias"] as String
                keyPassword = keystoreProperties["android_keystore_alias_password"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    firebaseAppDistribution {
        artifactType = "APK"
        serviceCredentialsFile = rootProject.file("firebase-service-account.json").path
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:settings"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    debugImplementation(libs.compose.ui.tooling)
}
