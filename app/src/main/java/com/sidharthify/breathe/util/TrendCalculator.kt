package com.sidharthify.breathe.util

import com.sidharthify.breathe.data.HistoryPoint
import kotlin.math.abs

fun calculateChange1h(
    history: List<HistoryPoint>?,
    currentAqi: Int,
    isNaqi: Boolean,
): Int? {
    if (history.isNullOrEmpty()) return null

    val nowSec = System.currentTimeMillis() / 1000L
    val targetTs = nowSec - 3600L
    val toleranceSec = 1800L

    var bestPoint: HistoryPoint? = null
    var bestDiff = Long.MAX_VALUE

    for (point in history) {
        val diff = abs(point.ts - targetTs)
        if (diff <= toleranceSec && diff < bestDiff) {
            bestDiff = diff
            bestPoint = point
        }
    }

    bestPoint ?: return null

    val pastAqi = if (isNaqi) bestPoint.aqi else (bestPoint.usAqi ?: bestPoint.aqi)
    return currentAqi - pastAqi
}
