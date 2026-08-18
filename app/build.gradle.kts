plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.compose")
}

android {
    namespace = "com.dynamicisland.coloros"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dynamicisland.coloros"
        minSdk = 26          // Android 8.0（PixelCopy 最低要求）
        targetSdk = 34        // Android 14
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false   // 调试阶段关闭混淆
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── AndroidX 核心 ──
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // ── Compose BOM（统一管理版本）──
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")

    // ── Lifecycle & ViewModel ──
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ── 协程 ──
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ── 媒体控制 ──
    implementation("androidx.media:media:1.7.0")

    // ── 调试 ──
    debugImplementation("androidx.compose.ui:ui-tooling")
}