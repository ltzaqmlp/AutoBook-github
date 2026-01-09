plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.autobook.lingxi"
    compileSdk = 34

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
}

dependencies {
    // 1. Android 核心 & Compose UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // 🔥 必须有的图标扩展库 (解决 Icons.Default.AutoAwesome 报错)
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    // 🔥 必须有的 ViewModel 适配库 (解决 viewModel() 报错)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    // 2. 异步任务
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.androidx.work.runtime.ktx)

    // 3. Room 数据库 (已清理重复项)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 4. OCR 引擎
    implementation(libs.rapidocr.android)
}