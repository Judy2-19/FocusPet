import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.focuspets"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.focuspets"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // 云端后端地址：从 local.properties 读取 CLOUD_BASE_URL，缺省回退模拟器回环
        val props = Properties().also { p ->
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { p.load(it) }
        }
        val baseUrl = props.getProperty("CLOUD_BASE_URL", "http://10.0.2.2:8080/")
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        buildConfigField("String", "CLOUD_BASE_URL", "\"$normalized\"")

        // 调试开关：默认 main=false（生产构建不调 DebugHelper），test 分支开发者打包时用
        //   ./gradlew assembleDebug -PisTestBuild=true
        // 覆盖为 true，则 MainActivity 在首启自动调 ensureTestSetup（解锁全部宠物 + 补 100 万积分）。
        // BuildConfig.DEBUG 在 release 构建会自动为 false，构成第二道防线。
        val isTestBuild = (project.findProperty("isTestBuild") as? String)?.toBoolean() ?: false
        buildConfigField("boolean", "IS_TEST_BUILD", isTestBuild.toString())
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

    buildFeatures {
        viewBinding = true
        // 云端后端地址需要 BuildConfig 字段（CLOUD_BASE_URL）
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Room（KSP 编译注解处理器）
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // 云端同步 / 排行榜：HTTP 客户端（Retrofit + OkHttp + Gson）
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
