package com.sidharthify.breathe.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import com.sidharthify.breathe.data.HistoryPoint

@Composable
fun AqiHistoryGraph(history: List<HistoryPoint>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) return

    val graphColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                "24 Hour Trend",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            val density = LocalDensity.current
            val labelWidth = with(density) { 35.dp.toPx() }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .pointerInput(history) {
                        detectTapGestures(
                            onPress = { offset ->
                                val graphWidth = size.width.toFloat() - labelWidth
                                val touchX = (offset.x - labelWidth).coerceAtLeast(0f)
                                val fraction = (touchX / graphWidth).coerceIn(0f, 1f)
                                val index = (fraction * (history.size - 1)).roundToInt()
                                selectedIndex = index
                                tryAwaitRelease()
                                selectedIndex = null
                            }
                        )
                    }
                    .pointerInput(history) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val graphWidth = size.width.toFloat() - labelWidth
                                val touchX = (offset.x - labelWidth).coerceAtLeast(0f)
                                val fraction = (touchX / graphWidth).coerceIn(0f, 1f)
                                val index = (fraction * (history.size - 1)).roundToInt()

                                if (index != selectedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIndex = index
                                }
                            },
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null },
                            onHorizontalDrag = { change, _ ->
                                val graphWidth = size.width.toFloat() - labelWidth
                                val touchX = (change.position.x - labelWidth).coerceAtLeast(0f)
                                val fraction = (touchX / graphWidth).coerceIn(0f, 1f)
                                val index = (fraction * (history.size - 1)).roundToInt()

                                if (index != selectedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIndex = index
                                }
                            }
                        )
                    }
            ) {
                val width = size.width - labelWidth
                val height = size.height

                val maxAqi = history.maxOf { it.aqi }.toFloat().coerceAtLeast(100f)
                val minAqi = history.minOf { it.aqi }.toFloat().coerceAtMost(0f)
                val range = maxAqi - minAqi

                fun getX(index: Int): Float = labelWidth + (index.toFloat() / (history.size - 1)) * width
                fun getY(aqi: Int): Float = height - ((aqi - minAqi) / range * height)

                val path = Path()

                history.forEachIndexed { i, point ->
                    val x = getX(i)
                    val y = getY(point.aqi)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = getX(i - 1)
                        val prevY = getY(history[i - 1].aqi)

                        val controlX = prevX + (x - prevX) / 2
                        path.cubicTo(controlX, prevY, controlX, y, x, y)
                    }
                }

                val fillPath = Path()
                fillPath.addPath(path)
                fillPath.lineTo(size.width, height)
                fillPath.lineTo(labelWidth, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(graphColor.copy(alpha = 0.3f), graphColor.copy(alpha = 0.0f))
                    )
                )

                drawPath(
                    path = path,
                    color = graphColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas

                    val textPaint = Paint().apply {
                        color = labelColor
                        textSize = 30f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    textPaint.textAlign = Paint.Align.LEFT

                    // Max AQI
                    nativeCanvas.drawText("${maxAqi.toInt()}", 0f, 30f, textPaint)

                    // Mid AQI
                    val midAqi = (maxAqi + minAqi) / 2
                    nativeCanvas.drawText("${midAqi.toInt()}", 0f, height / 2 + 10f, textPaint)

                    // Min AQI
                    nativeCanvas.drawText("${minAqi.toInt()}", 0f, height - 10f, textPaint)

                    if (selectedIndex == null) {
                        textPaint.textAlign = Paint.Align.CENTER
                        textPaint.typeface = Typeface.DEFAULT

                        val indicesToLabel = listOf(0, history.size / 2, history.size - 1)
                        indicesToLabel.forEach { i ->
                            if (i < history.size) {
                                val date = Date(history[i].ts * 1000)
                                val label = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)

                                textPaint.textAlign = when(i) {
                                    0 -> Paint.Align.LEFT
                                    history.size - 1 -> Paint.Align.RIGHT
                                    else -> Paint.Align.CENTER
                                }

                                val xPos = getX(i)
                                nativeCanvas.drawText(label, xPos, height + 45f, textPaint)
                            }
                        }
                    }
                }

                selectedIndex?.let { index ->
                    if (index in history.indices) {
                        val point = history[index]
                        val x = getX(index)
                        val y = getY(point.aqi)

                        drawLine(
                            color = highlightColor.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = highlightColor, radius = 4.dp.toPx(), center = Offset(x, y))

                        drawIntoCanvas { canvas ->
                            val textPaint = Paint().apply {
                                color = highlightColor.toArgb()
                                textSize = 32f
                                typeface = Typeface.DEFAULT_BOLD
                            }
                            
                            val date = Date(point.ts * 1000)
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                            val label = "AQI ${point.aqi} @ $timeStr"
                            
                            val textWidth = textPaint.measureText(label)
                            val padding = 20f
                            val boxWidth = textWidth + (padding * 2)
                            val boxHeight = 70f
                            
                            var boxX = x - (boxWidth / 2)
                            if (boxX < labelWidth) boxX = labelWidth
                            if (boxX + boxWidth > size.width) boxX = size.width - boxWidth
                            
                            val boxY = -60f

                            val paintBackground = Paint().apply {
                                color = surfaceColor.toArgb()
                                setShadowLayer(12f, 0f, 4f, android.graphics.Color.argb(50, 0, 0, 0))
                            }

                            canvas.nativeCanvas.drawRoundRect(
                                boxX, boxY, boxX + boxWidth, boxY + boxHeight,
                                16f, 16f, paintBackground
                            )
                            
                            textPaint.textAlign = Paint.Align.LEFT
                            canvas.nativeCanvas.drawText(label, boxX + padding, boxY + boxHeight - 22f, textPaint)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}