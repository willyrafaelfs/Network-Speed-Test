plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.kover)
}

android {
    namespace = "io.github.willyrafaelfs.speedtest"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.willyrafaelfs.speedtest"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "1.4.0"

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
    androidResources {
        // Bahasa per-aplikasi (Android 13+): daftar locale dibuat otomatis dari folder values-*.
        generateLocaleConfig = true
    }
    testOptions {
        // Robolectric butuh resource Android (string, font, tema) saat test JVM.
        unitTests.isIncludeAndroidResources = true
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

    // lifecycle-viewmodel-compose membawa kotlinx-serialization 1.7.3, sedangkan room-migration
    // (lewat room-testing) dikompilasi untuk 1.8.x. AGP mengunci versi di APK test ke versi APK
    // aplikasi, jadi versi aplikasi diselaraskan agar test migrasi tidak gagal (AbstractMethodError).
    implementation(platform(libs.kotlinx.serialization.bom))
    // Route navigasi type-safe berupa objek @Serializable.
    implementation(libs.kotlinx.serialization.core)

    // Navigasi, splash screen, dan navigasi adaptif (bottom bar / rail sesuai lebar layar)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material3.adaptive)

    // Preferensi user (Settings)
    implementation(libs.androidx.datastore.preferences)

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
    // Flow assertion untuk ViewModel.
    testImplementation(libs.turbine)
    // Android di JVM: Room in-memory, Compose UI test, dan screenshot test tanpa emulator.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // MigrationTestHelper untuk menguji migrasi skema Room.
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Coverage (Kover). Kode hasil generator (Hilt, Room, Compose) dan pratinjau dikecualikan
// agar angka mencerminkan kode yang memang ditulis tangan.
kover {
    reports {
        filters {
            excludes {
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                    "dagger.Module",
                    "dagger.hilt.android.HiltAndroidApp",
                    "javax.annotation.processing.Generated",
                )
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R\$*",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_HiltModules*",
                    "*_MembersInjector",
                    "*Hilt_*",
                    "*_Impl",
                    "*_Impl\$*",
                    "*ComposableSingletons*",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.internal.aggregatedroot.codegen.*",
                )
            }
        }
    }
}

// Ringkasan coverage per layer dari laporan XML Kover, dengan target minimum dari rencana
// portfolio. Dipakai CI: tabel dicetak ke log (GitLab membaca baris "Total line coverage")
// dan task gagal bila salah satu layer di bawah target.
tasks.register("coverageSummary") {
    group = "verification"
    description = "Mencetak dan memverifikasi coverage baris per layer (domain, data, ViewModel)."
    dependsOn("koverXmlReportDebug")
    val report = layout.buildDirectory.file("reports/kover/reportDebug.xml")
    inputs.file(report)
    doLast {
        val base = "io/github/willyrafaelfs/speedtest/"
        val layers = linkedMapOf<String, Pair<Double, (String) -> Boolean>>(
            "Domain" to (90.0 to { name -> name.startsWith(base + "domain/") }),
            "Data" to (70.0 to { name -> name.startsWith(base + "data/") && !name.startsWith(base + "data/di/") }),
            "ViewModel" to (
                80.0 to { name ->
                    name.substringAfterLast('/').substringBefore('$').endsWith("ViewModel")
                }
                ),
        )
        val totals = mutableMapOf<String, IntArray>()
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            // Laporan JaCoCo merujuk DTD eksternal; jangan diunduh.
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }.newDocumentBuilder().parse(report.get().asFile)
        val classes = document.getElementsByTagName("class")
        for (i in 0 until classes.length) {
            val element = classes.item(i) as org.w3c.dom.Element
            val name = element.getAttribute("name")
            val counters = element.getElementsByTagName("counter")
            for (j in 0 until counters.length) {
                val counter = counters.item(j) as org.w3c.dom.Element
                if (counter.getAttribute("type") != "LINE") continue
                val missed = counter.getAttribute("missed").toInt()
                val covered = counter.getAttribute("covered").toInt()
                val keys = layers.filterValues { it.second(name) }.keys + "Total"
                keys.forEach { key ->
                    val sum = totals.getOrPut(key) { IntArray(2) }
                    sum[0] += covered
                    sum[1] += missed
                }
            }
        }
        fun percent(key: String): Double {
            val (covered, missed) = totals[key]?.let { it[0] to it[1] } ?: (0 to 0)
            return if (covered + missed == 0) 0.0 else covered * 100.0 / (covered + missed)
        }
        val failures = mutableListOf<String>()
        println("Layer       Coverage  Target")
        layers.forEach { (key, value) ->
            val actual = percent(key)
            val ok = actual >= value.first
            println("%-10s %8.1f%%  %5.0f%% %s".format(key, actual, value.first, if (ok) "OK" else "BELOW"))
            if (!ok) failures += "%s %.1f%% < %.0f%%".format(key, actual, value.first)
        }
        println("Total line coverage: %.1f%%".format(percent("Total")))
        if (failures.isNotEmpty()) throw GradleException("Coverage di bawah target: " + failures.joinToString())
    }
}
