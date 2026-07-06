package com.example.networkspeedtest.domain.model

/**
 * Tahapan yang dilalui saat satu sesi test kecepatan berjalan.
 * Dipakai UI untuk menentukan indikator/gauge mana yang ditonjolkan.
 */
enum class SpeedTestPhase {
    IDLE,
    PING,
    DOWNLOAD,
    UPLOAD,
    FINISHED,
}
