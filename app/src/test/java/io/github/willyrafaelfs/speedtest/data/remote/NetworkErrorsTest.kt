package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestError
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NetworkErrorsTest {

    @Test
    fun `exception jaringan dipetakan ke kategori yang sesuai`() {
        val cases = mapOf(
            HttpStatusException(503) to SpeedTestError.ServerError(503),
            UnknownHostException("speed.cloudflare.com") to SpeedTestError.NoConnection,
            ConnectException("Connection refused") to SpeedTestError.NoConnection,
            SocketException("Network is unreachable") to SpeedTestError.NoConnection,
            SocketTimeoutException("timeout") to SpeedTestError.Timeout,
            InterruptedIOException("timeout") to SpeedTestError.Timeout,
            InterruptedIOException("interrupted") to SpeedTestError.Unknown,
            SSLHandshakeException("bad cert") to SpeedTestError.Unknown,
            IllegalStateException("bug") to SpeedTestError.Unknown,
        )

        cases.forEach { (throwable, expected) ->
            assertEquals("Pemetaan untuk $throwable", expected, throwable.toSpeedTestError())
        }
    }

    @Test
    fun `pembatalan tidak dibungkus menjadi error`() {
        val cancellation = CancellationException("user cancel")

        assertSame(cancellation, cancellation.toSpeedTestException())
    }

    @Test
    fun `exception lain dibungkus dengan penyebab aslinya`() {
        val cause = ConnectException("Connection refused")

        val wrapped = cause.toSpeedTestException() as SpeedTestException

        assertEquals(SpeedTestError.NoConnection, wrapped.error)
        assertSame(cause, wrapped.cause)
    }
}
