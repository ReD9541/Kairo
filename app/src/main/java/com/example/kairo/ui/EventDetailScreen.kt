package com.example.kairo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import com.example.kairo.domain.model.ScheduleItem
import com.example.kairo.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.example.kairo.ui.theme.KairoTheme

@Composable
fun EventDetailPopup(
    itemId: String,
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val item = uiState.scheduleItems.find { it.id == itemId }

    val formatter = remember { SimpleDateFormat("hh:mm a, EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val isTransit = item is ScheduleItem.Transit

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        KairoTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = if (isTransit) Alignment.Center else Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .then(
                            if (isTransit) Modifier.fillMaxHeight(0.85f) else Modifier.wrapContentHeight()
                        ),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.padding(20.dp).wrapContentHeight()) {
                        if (item == null) {
                            Text(
                                text = "Item not found.",
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            Column(modifier = Modifier.wrapContentHeight()) {
                                // Title and Close Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onDismiss) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isTransit && item is ScheduleItem.Transit) {
                                    // Scrollable list for Transit items
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        item {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = "${timeFormatter.format(Date(item.startTime))} - ${timeFormatter.format(Date(item.endTime))}",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(text = "${item.durationMillis / 60000} minutes commute", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        if (item.legs.isNotEmpty()) {
                                            item {
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Text(
                                                        text = "Route Steps",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )

                                                    // Horizontal Mode Badges Summary
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        item.legs.forEachIndexed { idx, leg ->
                                                            val modeColor = when (leg.transportationType) {
                                                                "Train" -> Color(0xFFE05252)
                                                                "Bus", "Coach" -> Color(0xFF00B2A9)
                                                                "Light Rail" -> Color(0xFFE2231A)
                                                                else -> MaterialTheme.colorScheme.primary
                                                            }
                                                            
                                                            val isWalk = leg.transportationType.equals("Walk", ignoreCase = true) || leg.transportationName.contains("Walk", ignoreCase = true)
                                                            val shortName = if (isWalk) {
                                                                "Walk"
                                                            } else {
                                                                val match = Regex("""\b([A-Z0-9]{2,3})\b""").find(leg.transportationName)
                                                                match?.value ?: leg.transportationType
                                                            }

                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = modeColor.copy(alpha = 0.2f),
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, modeColor)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    val icon = when {
                                                                        leg.transportationType.equals("Train", ignoreCase = true) -> Icons.Rounded.Train
                                                                        leg.transportationType.equals("Bus", ignoreCase = true) || leg.transportationType.equals("Coach", ignoreCase = true) -> Icons.Rounded.DirectionsBus
                                                                        isWalk -> Icons.AutoMirrored.Rounded.DirectionsWalk
                                                                        else -> Icons.Rounded.DirectionsTransit
                                                                    }
                                                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = modeColor)
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(
                                                                        text = shortName,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        color = Color.White
                                                                    )
                                                                }
                                                            }
                                                            if (idx < item.legs.size - 1) {
                                                                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            itemsIndexed(item.legs) { index, leg ->
                                                val modeColor = when (leg.transportationType) {
                                                    "Train" -> Color(0xFFE05252)
                                                    "Bus", "Coach" -> Color(0xFF00B2A9)
                                                    else -> MaterialTheme.colorScheme.primary
                                                }

                                                val depTimeStr = try {
                                                    timeFormatter.format(Date(leg.departureTime.let { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(it)?.time ?: 0L }))
                                                } catch (e: Exception) { "" }

                                                val arrTimeStr = try {
                                                    timeFormatter.format(Date(leg.arrivalTime.let { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(it)?.time ?: 0L }))
                                                } catch (e: Exception) { "" }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min)
                                                ) {
                                                    // Left Vertical Line & Dot
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.width(32.dp).fillMaxHeight()
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(12.dp)
                                                                .clip(CircleShape)
                                                                .background(modeColor)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .weight(1f)
                                                                .background(modeColor.copy(alpha = 0.6f))
                                                        )
                                                        if (index == item.legs.size - 1) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .clip(CircleShape)
                                                                    .background(modeColor)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    // Step Information Card
                                                    Column(
                                                        modifier = Modifier.weight(1f).padding(bottom = 16.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(
                                                                text = leg.origin.substringBefore(","),
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = Color.White,
                                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                            )
                                                            if (depTimeStr.isNotBlank()) {
                                                                Text(text = depTimeStr, style = MaterialTheme.typography.labelMedium, color = modeColor)
                                                            }
                                                        }

                                                        val isTransitLeg = leg.transportationType.equals("Train", ignoreCase = true) || 
                                                                          leg.transportationType.equals("Bus", ignoreCase = true) || 
                                                                          leg.transportationType.equals("Coach", ignoreCase = true) || 
                                                                          leg.transportationType.equals("Light Rail", ignoreCase = true) || 
                                                                          leg.transportationType.equals("Ferry", ignoreCase = true) || 
                                                                          leg.transportationType.equals("Metro", ignoreCase = true) ||
                                                                          leg.transportationName.contains("Train", ignoreCase = true) || 
                                                                          leg.transportationName.contains("Bus", ignoreCase = true) || 
                                                                          leg.transportationName.contains("Line", ignoreCase = true) ||
                                                                          Regex("""\b(T[0-9]|B[0-9]|[0-9]{3})\b""").containsMatchIn(leg.transportationName)
                                                        val isWalkLeg = !isTransitLeg

                                                        if (!isWalkLeg && !leg.originPlatform.isNullOrBlank()) {
                                                            val platformText = if (leg.originPlatform.contains("Platform", ignoreCase = true) || leg.originPlatform.contains("Stand", ignoreCase = true)) {
                                                                leg.originPlatform
                                                            } else {
                                                                "Platform ${leg.originPlatform}"
                                                            }
                                                            Text(
                                                                text = "Get on $platformText",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = modeColor.copy(alpha = 0.15f)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                val isWalkLegInner = !isTransitLeg
                                                                val shortName = if (isWalkLegInner) "Walk" else {
                                                                    Regex("""\b([A-Z0-9]{2,3})\b""").find(leg.transportationName)?.value ?: leg.transportationName.take(15)
                                                                }
                                                                Text(
                                                                    text = "$shortName • ${leg.durationMillis / 60000} mins",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = modeColor,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                                Text(
                                                                    text = leg.destination.substringBefore(","),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                if (!isWalkLeg && !leg.destinationPlatform.isNullOrBlank()) {
                                                                    val destPlatformText = if (leg.destinationPlatform.contains("Platform", ignoreCase = true) || leg.destinationPlatform.contains("Stand", ignoreCase = true)) {
                                                                        leg.destinationPlatform
                                                                    } else {
                                                                        "Platform ${leg.destinationPlatform}"
                                                                    }
                                                                    Text(
                                                                        text = "Get off on $destPlatformText",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }

                                                                // Calculate transfer / wait time between legs
                                                                val nextLeg = item.legs.getOrNull(index + 1)
                                                                if (nextLeg != null) {
                                                                    val currentArrMs = try {
                                                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(leg.arrivalTime)?.time ?: 0L
                                                                    } catch(e: Exception) { 0L }
                                                                    
                                                                    val nextDepMs = try {
                                                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(nextLeg.departureTime)?.time ?: 0L
                                                                    } catch(e: Exception) { 0L }

                                                                    val gapMins = if (currentArrMs > 0 && nextDepMs > currentArrMs) (nextDepMs - currentArrMs) / 60000 else 0L

                                                                    if (gapMins > 0) {
                                                                        val transferText = if (gapMins >= 4) {
                                                                            "Walk 2 mins & wait for ${gapMins - 2} mins"
                                                                        } else {
                                                                            "Wait for ${gapMins} mins"
                                                                        }
                                                                        Spacer(modifier = Modifier.height(6.dp))
                                                                        Surface(
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                                        ) {
                                                                            Text(
                                                                                text = "↳ $transferText",
                                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                                                color = MaterialTheme.colorScheme.primary,
                                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            if (arrTimeStr.isNotBlank()) {
                                                                Text(text = arrTimeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Compact non-transit layout for Events & Focus Blocks
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "${timeFormatter.format(Date(item.startTime))} - ${timeFormatter.format(Date(item.endTime))}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        when (item) {
                                            is ScheduleItem.Event -> {
                                                item.location?.let { loc ->
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Rounded.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(text = loc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                if (!item.description.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = "Description:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                                    Text(text = item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            is ScheduleItem.FocusBlock -> {
                                                val mins = (item.endTime - item.startTime) / 60000
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(text = "$mins minutes session", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                if (item.suggestedActivities.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = "Suggested Activities:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                                    item.suggestedActivities.forEach { act ->
                                                        Text(text = "• $act", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                KairoButton(
                                    text = "Done",
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
