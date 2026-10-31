import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.just_sri.suvidha.expenses"
    compileSdk = 37
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

    defaultConfig {
        applicationId = "com.just_sri.suvidhaapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "1.0.3"
        manifestPlaceholders["appLabel"] = "Suvidha Expenses"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { debugSymbolLevel = "FULL" }
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
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("com.vitorpamplona.marmot:marmot-jvm:2.2.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    ksp("androidx.room:room-compiler:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:2.7.0")
    androidTestImplementation("androidx.work:work-testing:2.12.0")
}
