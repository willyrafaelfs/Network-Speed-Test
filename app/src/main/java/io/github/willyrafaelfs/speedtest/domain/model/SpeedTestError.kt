package io.github.willyrafaelfs.speedtest.domain.model

/**
 * Penyebab kegagalan test yang dipahami domain. Data layer menerjemahkan exception
 * jaringan mentah (OkHttp/java.net) ke salah satu kategori ini, sehingga UI bisa
 * menampilkan pesan yang ramah tanpa tahu detail library jaringan.
 *
 * Pembatalan oleh user sengaja tidak termasuk di sini: itu bukan error, melainkan
 * pembatalan coroutine biasa.
 */
sealed interface SpeedTestError {
    /** Perangkat tidak punya koneksi, DNS gagal, atau server tidak bisa dijangkau. */
    data object NoConnection : SpeedTestError

    /** Koneksi tersambung tetapi server berhenti merespons dalam batas waktu. */
    data object Timeout : SpeedTestError

    /** Server menjawab dengan status HTTP gagal (4xx/5xx). */
    data class ServerError(val httpCode: Int) : SpeedTestError

    /** Kegagalan lain yang tidak terklasifikasi. */
    data object Unknown : SpeedTestError
}

/** Exception yang membawa [SpeedTestError]; dilempar oleh implementasi SpeedTestRepository. */
class SpeedTestException(
    val error: SpeedTestError,
    cause: Throwable? = null,
) : RuntimeException("Speed test gagal: $error", cause)
