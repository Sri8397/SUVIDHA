import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.just_sri.suvidha.expenses"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.just_sri.suvidha.expenses"
        minSdk = 26
    targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) {
            FileInputStream(f).use { load(it) }
        }
    }
    val keystorePath = (project.findProperty("SUVIDHA_KEYSTORE_PATH") as? String)
        ?: localProps.getProperty("SUVIDHA_KEYSTORE_PATH")
        ?: System.getenv("SUVIDHA_KEYSTORE_PATH")
    val keystorePassword = (project.findProperty("SUVIDHA_KEYSTORE_PASSWORD") as? String)
        ?: localProps.getProperty("SUVIDHA_KEYSTORE_PASSWORD")
        ?: System.getenv("SUVIDHA_KEYSTORE_PASSWORD")
    val keyAlias = (project.findProperty("SUVIDHA_KEY_ALIAS") as? String)
        ?: localProps.getProperty("SUVIDHA_KEY_ALIAS")
        ?: System.getenv("SUVIDHA_KEY_ALIAS")
    val keyPassword = (project.findProperty("SUVIDHA_KEY_PASSWORD") as? String)
        ?: localProps.getProperty("SUVIDHA_KEY_PASSWORD")
        ?: System.getenv("SUVIDHA_KEY_PASSWORD")

    signingConfigs {
        if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
