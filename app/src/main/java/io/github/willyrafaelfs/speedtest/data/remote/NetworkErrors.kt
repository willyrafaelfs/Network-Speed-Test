package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestError
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

/** Menerjemahkan exception jaringan mentah ke kategori [SpeedTestError]. */
internal fun Throwable.toSpeedTestError(): SpeedTestError = when (this) {
    is SpeedTestException -> error

    is HttpStatusException -> SpeedTestError.ServerError(code)

    is UnknownHostException -> SpeedTestError.NoConnection

    // Termasuk ConnectException, NoRouteToHostException, "Network is unreachable",
    // dan koneksi yang putus di tengah jalan (mis. pindah dari WiFi ke seluler).
    is SocketException -> SpeedTestError.NoConnection

    is SocketTimeoutException -> SpeedTestError.Timeout

    // OkHttp melempar InterruptedIOException("timeout") saat call timeout tercapai.
    is InterruptedIOException -> if (message == "timeout") SpeedTestError.Timeout else SpeedTestError.Unknown

    else -> SpeedTestError.Unknown
}

/**
 * Membungkus exception menjadi [SpeedTestException]. CancellationException dibiarkan
 * apa adanya karena pembatalan bukan error dan harus tetap dipropagasikan.
 */
internal fun Throwable.toSpeedTestException(): Throwable = when (this) {
    is CancellationException, is SpeedTestException -> this
    else -> SpeedTestException(toSpeedTestError(), this)
}
