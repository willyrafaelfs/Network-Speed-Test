package io.github.willyrafaelfs.speedtest.presentation

import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.presentation.component.DialScale
import io.github.willyrafaelfs.speedtest.presentation.component.niceAxisMax
import io.github.willyrafaelfs.speedtest.presentation.format.fromMbps
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitScaleTest {

    @Test
    fun `MB per detik adalah Mbps dibagi delapan`() {
        assertEquals(100.0, SpeedUnit.MBPS.fromMbps(100.0), DELTA)
        assertEquals(12.5, SpeedUnit.MEGABYTES_PER_SECOND.fromMbps(100.0), DELTA)
    }

    @Test
    fun `skala dial non-linear menempatkan tiap breakpoint di sudut yang sama`() {
        val scale = DialScale.MBPS
        val step = 1f / (scale.breakpoints.size - 1)

        assertEquals(0f, scale.fractionOf(0f), DELTA_F)
        assertEquals(step, scale.fractionOf(25f), DELTA_F)
        assertEquals(step * 2.5f, scale.fractionOf(75f), DELTA_F) // tengah 50..100
        assertEquals(1f, scale.fractionOf(5_000f), DELTA_F) // mentok di ujung skala
        assertEquals(0f, scale.fractionOf(-3f), DELTA_F)
    }

    @Test
    fun `skala MB per detik berakhir di 125 MBps alias 1 Gbps`() {
        assertEquals(DialScale.MEGABYTES, DialScale.of(SpeedUnit.MEGABYTES_PER_SECOND))
        assertEquals(1f, DialScale.MEGABYTES.fractionOf(125f), DELTA_F)
        assertEquals("1k", DialScale.MBPS.label(1000f))
        assertEquals("125", DialScale.MEGABYTES.label(125f))
    }

    @Test
    fun `batas sumbu grafik dibulatkan ke 1-2,5-5 kali pangkat sepuluh`() {
        assertEquals(10f, niceAxisMax(0f), DELTA_F)
        assertEquals(25f, niceAxisMax(11f), DELTA_F)
        assertEquals(100f, niceAxisMax(57.5f), DELTA_F)
        assertEquals(250f, niceAxisMax(230f), DELTA_F)
    }

    private companion object {
        const val DELTA = 1e-9
        const val DELTA_F = 1e-5f
    }
}
