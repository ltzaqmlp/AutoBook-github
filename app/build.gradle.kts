plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) // 确保这里有这行
}

android {
    namespace = "com.autobook.lingxi"
    compileSdk = 34 // 必须至少是 34

    defaultConfig {
        applicationId = "com.autobook.lingxi"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++14"
                abiFilters += "arm64-v8a"
            }
        }
    }
    // 【新增/修改】强制使用 Java 17，解决 (1.8) vs (21) 的冲突
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

    }

    // 【新增】让 Kotlin 编译器也输出 Java 17
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    // Kotlin 2.0 不需要 composeOptions，已删除
}

dependencies {
    // 1. 基础 UI 和 Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // 2. 异步
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 3. Room 数据库
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // 【关键修改】如果 ksp(...) 爆红，用 add("ksp", ...) 是 100% 安全的写法
    add("ksp", libs.androidx.room.compiler)

    // 4. WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.rapidocr.android)
}