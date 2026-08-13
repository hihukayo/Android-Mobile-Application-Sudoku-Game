import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.sudoku"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sudoku"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.1"
    }

    signingConfigs {
        create("release") {
            val ksFile = File(rootProject.rootDir, "keystore/sudoku-release.jks")
            if (ksFile.exists()) {
                val props = Properties().apply {
                    val pf = File(rootProject.rootDir, "keystore/keystore-password.txt")
                    if (pf.exists()) pf.inputStream().use { load(it) }
                }
                storeFile = ksFile
                storePassword = props.getProperty("storepass", "")
                keyAlias = props.getProperty("alias", "sudoku")
                keyPassword = props.getProperty("keypass", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 存在本地 keystore 时签名，否则输出 unsigned 供本地调试
            if (File(rootProject.rootDir, "keystore/sudoku-release.jks").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
