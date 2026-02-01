plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mygeofenceapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.mygeofenceapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Android核心扩展库
    implementation("androidx.preference:preference-ktx:1.2.1")
    // OSMDroid - 核心地图引擎 [1, 9]
    // 该库提供了MapView组件及离线瓦片支持
    implementation("org.osmdroid:osmdroid-android:6.1.10")
    // Kotlin 协程 - 用于在后台线程处理地理围栏计算，避免阻塞主线程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}