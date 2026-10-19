package com.example.kairo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SleepScheduleDial(
    wakeUpTime: String,
    sleepTime: String,
    onWakeUpChange: (String) -> Unit,
    onSleepChange: (String) -> Unit
) {
    // Parse times
    val (sleepHour, sleepMin) = sleepTime.split(":").map { it.toInt() }
    val (wakeHour, wakeMin) = wakeUpTime.split(":").map { it.toInt() }

    var sleepAngle by remember(sleepTime) { mutableFloatStateOf(timeToAngle(sleepHour, sleepMin)) }
    var wakeAngle by remember(wakeUpTime) { mutableFloatStateOf(timeToAngle(wakeHour, wakeMin)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondaryContainer

        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val dragAngle = calculateAngle(center, change.position)
                
                // Determine which handle is closer
                val dSleep = Math.abs(angleDiff(dragAngle, sleepAngle))
                val dWake = Math.abs(angleDiff(dragAngle, wakeAngle))
                
                if (dSleep < dWake && dSleep < 45f) {
                    sleepAngle = dragAngle
                    val (h, m) = angleToTime(sleepAngle)
                    onSleepChange(String.format("%02d:%02d", h, m))
                } else if (dWake <= dSleep && dWake < 45f) {
                    wakeAngle = dragAngle
                    val (h, m) = angleToTime(wakeAngle)
                    onWakeUpChange(String.format("%02d:%02d", h, m))
                }
            }
        }) {
            val radius = size.minDimension / 2f - 20.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Draw background circle
            drawCircle(
                color = secondaryColor,
                radius = radius,
                style = Stroke(width = 10.dp.toPx())
            )

            // Draw track (sweep)
            val sweep = if (wakeAngle >= sleepAngle) wakeAngle - sleepAngle else 360f - (sleepAngle - wakeAngle)
            drawArc(
                color = primaryColor,
                startAngle = sleepAngle - 90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Draw Sleep Handle
            val sleepRad = Math.toRadians(sleepAngle.toDouble() - 90.0)
            val sleepX = center.x + radius * cos(sleepRad).toFloat()
            val sleepY = center.y + radius * sin(sleepRad).toFloat()
            drawCircle(color = primaryColor, radius = 15.dp.toPx(), center = Offset(sleepX, sleepY))

            // Draw Wake Handle
            val wakeRad = Math.toRadians(wakeAngle.toDouble() - 90.0)
            val wakeX = center.x + radius * cos(wakeRad).toFloat()
            val wakeY = center.y + radius * sin(wakeRad).toFloat()
            drawCircle(color = Color(0xFFFF9800), radius = 15.dp.toPx(), center = Offset(wakeX, wakeY))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Bed: $sleepTime", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Wake: $wakeUpTime", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF9800))
        }
    }
}

private fun timeToAngle(hour: Int, min: Int): Float {
    // 24 hour mapped to 360 degrees. 0:00 is at top (0 degrees).
    val minutes = hour * 60 + min
    return (minutes / (24f * 60f)) * 360f
}

private fun angleToTime(angle: Float): Pair<Int, Int> {
    var normalized = angle % 360f
    if (normalized < 0) normalized += 360f
    val minutes = (normalized / 360f) * 24 * 60
    val h = (minutes / 60).toInt() % 24
    val m = ((minutes % 60) / 5).toInt() * 5 // Snap to 5 mins
    return Pair(h, m)
}

private fun calculateAngle(center: Offset, point: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
    if (angle < 0) angle += 360f
    return angle
}

private fun angleDiff(a: Float, b: Float): Float {
    val diff = (a - b) % 360f
    return if (diff > 180f) diff - 360f else if (diff < -180f) diff + 360f else diff
}
