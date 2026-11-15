package io.github.willyrafaelfs.speedtest.presentation.component

import io.github.willyrafaelfs.speedtest.domain.model.BufferbloatGrade

/** Teks grade seperti yang lazim ditampilkan alat bufferbloat (mis. "A+"). */
val BufferbloatGrade.label: String
    get() = when (this) {
        BufferbloatGrade.A_PLUS -> "A+"
        else -> name
    }
