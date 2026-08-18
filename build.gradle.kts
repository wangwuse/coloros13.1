// 根构建脚本 — 管理 Compose / Kotlin / AGP 版本统一
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// 统一版本号，子模块通过 extra 读取
extra["composeCompilerVersion"] = "1.5.8"
extra["kotlinVersion"] = "1.9.22"
