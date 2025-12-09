package com.sidharthify.breathe.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

fun createBlobIcon(context: Context, color: Int): Drawable {
    val radius = 32f
    val bitmap = Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true }

    paint.color = color
    canvas.drawCircle(radius, radius, radius, paint)

    return BitmapDrawable(context.resources, bitmap)
}

fun formatPollutantName(key: String): String {
    return when(key.lowercase()) {
        "pm2_5", "pm2.5" -> "PM2.5"
        "pm10" -> "PM10"
        "no2" -> "NO₂"
        "so2" -> "SO₂"
        "co" -> "CO"
        "o3" -> "O₃"
        else -> key.uppercase()
    }
}

fun getAqiColor(aqi: Int): Color {
    return when (aqi) {
        in 0..50 -> Color(0xFF55A84F)
        in 51..100 -> Color(0xFFA3C853)
        in 101..200 -> Color(0xFFFDD74B)
        in 201..300 -> Color(0xFFFB9A34)
        in 301..400 -> Color(0xFFE93F33)
        else -> Color(0xFFAF2D24)
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "Just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        else -> ">1d ago"
    }
}