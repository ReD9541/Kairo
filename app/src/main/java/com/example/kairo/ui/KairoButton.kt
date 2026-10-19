package com.example.kairo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kairo.ui.theme.ConcertOneFontFamily

@Composable
fun KairoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = "VALIDATING...",
    fontSize: TextUnit = 13.sp,
    letterSpacing: TextUnit = 1.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = if (isPressed && !isLoading) Color.White else Color.Transparent
    val contentColor = if (isPressed && !isLoading) Color.Black else Color.White

    Button(
        onClick = onClick,
        modifier = modifier.height(45.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, if (enabled || isLoading) Color.White else Color.White.copy(alpha = 0.3f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = if (isLoading) Color.White else Color.White.copy(alpha = 0.3f)
        ),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        elevation = null
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = loadingText.uppercase(),
                    letterSpacing = letterSpacing,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ConcertOneFontFamily,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                text = text.uppercase(),
                letterSpacing = letterSpacing,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = ConcertOneFontFamily,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
