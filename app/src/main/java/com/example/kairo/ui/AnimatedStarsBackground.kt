package com.example.kairo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.kairo.ui.theme.DarkBackgroundEnd
import com.example.kairo.ui.theme.DarkBackgroundStart
import kotlin.random.Random

@Composable
fun AnimatedStarsBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // We will generate the stars once. We need random coordinates between 0 and 1 (relative to screen size)
    val random = remember { Random(42) }
    
    // Layer 1: Small stars, fast (50s)
    val layer1Stars = remember { List(400) { Offset(random.nextFloat(), random.nextFloat()) } }
    // Layer 2: Medium stars, medium speed (100s)
    val layer2Stars = remember { List(150) { Offset(random.nextFloat(), random.nextFloat()) } }
    // Layer 3: Large stars, slow (150s)
    val layer3Stars = remember { List(50) { Offset(random.nextFloat(), random.nextFloat()) } }

    val infiniteTransition = rememberInfiniteTransition(label = "stars")

    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset2"
    )

    val offset3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(DarkBackgroundEnd, DarkBackgroundStart),
                    radius = 2000f,
                    center = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) // Fakes "ellipse at bottom"
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Helper to draw a layer
            fun drawStarLayer(stars: List<Offset>, radius: Float, progress: Float) {
                stars.forEach { normalizedOffset ->
                    val x = normalizedOffset.x * width
                    // Base y
                    val baseY = normalizedOffset.y * height
                    
                    // Shift y up by progress. Progress goes 0 -> 1, so we shift up to 1 full screen height
                    var shiftedY = baseY - (progress * height)
                    
                    // If it goes above the screen, wrap it to the bottom
                    if (shiftedY < 0) shiftedY += height
                    
                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        center = Offset(x, shiftedY)
                    )
                }
            }

            drawStarLayer(layer1Stars, 1f, offset1)
            drawStarLayer(layer2Stars, 1.5f, offset2)
            drawStarLayer(layer3Stars, 2.5f, offset3)
        }

        content()
    }
}
