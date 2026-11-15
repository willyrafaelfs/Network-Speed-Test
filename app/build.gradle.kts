plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.willyrafaelfs.speedtest"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.willyrafaelfs.speedtest"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Semua temuan lint dianggap error agar CI menolak regresi kualitas.
        warningsAsErrors = true
        abortOnError = true
        // Cek "versi baru tersedia" dimatikan: hasilnya berubah sendiri tiap ada rilis
        // library baru sehingga CI bisa merah tanpa ada perubahan kode.
        disable += setOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion",
            "OldTargetApi",
        )
    }
}

room {
    // Skema tiap versi database di-commit agar migrasi bisa direview dan diuji.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Lifecycle: ViewModel + Compose (collectAsStateWithLifecycle, viewModel())
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Networking (OkHttp)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Room (local storage untuk riwayat hasil test)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt (dependency injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Unit test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Server HTTP lokal untuk menguji engine download/upload/ping tanpa internet.
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // MigrationTestHelper untuk menguji migrasi skema Room.
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
