// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

// Format kode: ktlint (+ aturan khusus Jetpack Compose) untuk semua file Kotlin & skrip Gradle.
// Jalankan `./gradlew spotlessApply` untuk merapikan, `spotlessCheck` dipakai di CI.
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val ktlintRules =
        mapOf(
            "ktlint_code_style" to "android_studio",
            "max_line_length" to "120",
            // Pertahankan trailing comma yang sudah dipakai di seluruh kode.
            "ij_kotlin_allow_trailing_comma" to "true",
            "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
            // Composable memakai PascalCase sesuai konvensi Jetpack Compose.
            "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
            // File boleh berisi beberapa top-level declaration (mis. model + mapper).
            "ktlint_standard_filename" to "disabled",
            // Rule berikut menulis ulang struktur kode (mis. memecah `class X @Inject constructor(...)`
            // dan memindahkan setiap ekspresi multi-baris ke baris baru) tanpa menambah keterbacaan.
            "ktlint_standard_class-signature" to "disabled",
            "ktlint_standard_function-signature" to "disabled",
            "ktlint_standard_multiline-expression-wrapping" to "disabled",
            "ktlint_standard_chain-method-continuation" to "disabled",
        )
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
            .editorConfigOverride(ktlintRules)
            .customRuleSets(listOf(libs.compose.rules.ktlint.get().toString()))
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintRules)
    }
}

// Static analysis. Konfigurasi = default Detekt + override di config/detekt/detekt.yml.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    source.setFrom(files("app/src/main/java", "app/src/test/java"))
    parallel = true
}

// Detekt 1.23.x dikompilasi dengan Kotlin 2.0.21 dan menolak berjalan dengan versi
// compiler lain. Kotlin plugin proyek menyelaraskan versi org.jetbrains.kotlin ke versi
// proyek, jadi khusus classpath Detekt dipaksa kembali ke versi yang didukungnya.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "11"
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}
