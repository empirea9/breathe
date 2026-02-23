package com.sidharthify.breathe.util

import com.sidharthify.breathe.data.HistoryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrendCalculatorTest {

    private val nowSec: Long get() = System.currentTimeMillis() / 1000L

    @Test
    fun `null history returns null`() {
        assertNull(calculateChange1h(null, 100, isNaqi = true))
    }

    @Test
    fun `empty history returns null`() {
        assertNull(calculateChange1h(emptyList(), 100, isNaqi = true))
    }

    @Test
    fun `no point within tolerance returns null`() {
        // Point is 2 hours ago — outside ±30 min window
        val history = listOf(HistoryPoint(ts = nowSec - 7200L, aqi = 80, usAqi = null))
        assertNull(calculateChange1h(history, 100, isNaqi = true))
    }

    @Test
    fun `NAQI mode - AQI worsened returns positive delta`() {
        val history = listOf(HistoryPoint(ts = nowSec - 3600L, aqi = 80, usAqi = null))
        assertEquals(20, calculateChange1h(history, 100, isNaqi = true))
    }

    @Test
    fun `NAQI mode - AQI improved returns negative delta`() {
        val history = listOf(HistoryPoint(ts = nowSec - 3600L, aqi = 120, usAqi = null))
        assertEquals(-20, calculateChange1h(history, 100, isNaqi = true))
    }

    @Test
    fun `NAQI mode - no change returns 0`() {
        val history = listOf(HistoryPoint(ts = nowSec - 3600L, aqi = 100, usAqi = null))
        assertEquals(0, calculateChange1h(history, 100, isNaqi = true))
    }

    @Test
    fun `US AQI mode - reads usAqi field`() {
        val history = listOf(HistoryPoint(ts = nowSec - 3600L, aqi = 80, usAqi = 90))
        assertEquals(10, calculateChange1h(history, 100, isNaqi = false))
    }

    @Test
    fun `US AQI mode - falls back to aqi when usAqi is null`() {
        val history = listOf(HistoryPoint(ts = nowSec - 3600L, aqi = 80, usAqi = null))
        assertEquals(20, calculateChange1h(history, 100, isNaqi = false))
    }

    @Test
    fun `picks closest point within tolerance window`() {
        val closer = HistoryPoint(ts = nowSec - 3500L, aqi = 60, usAqi = null)
        val farther = HistoryPoint(ts = nowSec - 3700L, aqi = 50, usAqi = null)
        val history = listOf(farther, closer)
        // Should pick the closer point (aqi=60), delta = 100 - 60 = 40
        assertEquals(40, calculateChange1h(history, 100, isNaqi = true))
    }

    @Test
    fun `point at boundary of tolerance is included`() {
        // Exactly at 30 min (1800s) boundary — should be included
        val history = listOf(HistoryPoint(ts = nowSec - 3600L - 1800L, aqi = 70, usAqi = null))
        assertEquals(30, calculateChange1h(history, 100, isNaqi = true))
    }

    @Test
    fun `point outside tolerance is excluded`() {
        // 1801s outside the 1h target — just beyond tolerance
        val history = listOf(HistoryPoint(ts = nowSec - 3600L - 1801L, aqi = 70, usAqi = null))
        assertNull(calculateChange1h(history, 100, isNaqi = true))
    }

    @Test
    fun `dense hourly history - picks point closest to 1h ago`() {
        // Simulate 24 hourly data points; the one at ~1h ago should be selected
        val history = (0 until 24).map { i ->
            HistoryPoint(ts = nowSec - i * 3600L, aqi = 50 + i * 2, usAqi = null)
        }
        // Point at index 1 is exactly 1h ago: aqi = 50 + 1*2 = 52
        assertEquals(100 - 52, calculateChange1h(history, 100, isNaqi = true))
    }
}
