package com.example.kairo.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.example.kairo.ui.theme.ConcertOneFontFamily

@Composable
fun KairoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isFilled = value.isNotEmpty()
    val isActive = isFocused || isFilled

    val labelProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "labelProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
    ) {
        // The background and border canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val cornerRadius = 8.dp.toPx()
                    val color = Color.White
                    
                    if (isActive) {
                        drawRoundRect(
                            color = color,
                            size = size,
                            cornerRadius = CornerRadius(cornerRadius),
                            style = Stroke(width = strokeWidth)
                        )
                    } else {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(0f, size.height - cornerRadius)
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    0f, size.height - cornerRadius * 2,
                                    cornerRadius * 2, size.height
                                ),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = -90f,
                                forceMoveTo = false
                            )
                            lineTo(size.width, size.height)
                        }
                        drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
                    }
                }
                .padding(12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = interactionSource,
                singleLine = singleLine,
                minLines = minLines,
                maxLines = maxLines,
                readOnly = readOnly,
                textStyle = TextStyle(
                    color = Color.White, 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ConcertOneFontFamily
                )
            )
        }

        // Floating Label
        val hBias = -1f + (labelProgress * 1.8f) // Move from left to right
        val yOffset = 12.dp * (1f - labelProgress) - (22.dp * labelProgress)
        
        val labelBgColor = if (isActive) Color.White else Color.Transparent
        val labelTextColor = if (isActive) Color.Black else Color.White
        val letterSpc = if (isActive) 2.sp else 3.sp
        val fontSize = if (isActive) 12.sp else 14.sp

        Box(
            modifier = Modifier
                .align(androidx.compose.ui.BiasAlignment(hBias, -1f))
                .offset(y = yOffset)
                .background(labelBgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = if (isActive) 10.dp else 4.dp, vertical = if (isActive) 5.dp else 0.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = labelTextColor,
                fontSize = fontSize,
                letterSpacing = letterSpc,
                fontWeight = FontWeight.Bold,
                fontFamily = ConcertOneFontFamily
            )
        }
        
        trailingIcon?.let { icon ->
            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                icon()
            }
        }
    }
}
