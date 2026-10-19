package com.example.kairo.ui

import android.Manifest
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import com.example.kairo.R
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kairo.domain.model.ScheduleItem
import com.example.kairo.viewmodel.DashboardViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onEventClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val calendarPermissionState = rememberPermissionState(
        Manifest.permission.READ_CALENDAR
    )

    LaunchedEffect(calendarPermissionState.status) {
        if (!calendarPermissionState.status.isGranted) {
            calendarPermissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!calendarPermissionState.status.isGranted) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Calendar permission is required to view your schedule.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { calendarPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        } else if (uiState.isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading your schedule...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (uiState.error != null) {
            Text(
                text = "Error: ${uiState.error}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.scheduleItems.isEmpty()) {
            Text(
                text = "No events scheduled for today.",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    val now = System.currentTimeMillis()
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greeting = when {
                        currentHour < 12 -> "Good Morning"
                        currentHour < 17 -> "Good Afternoon"
                        else -> "Good Evening"
                    }

                    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
                    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                    
                    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
                    val context = LocalContext.current

                    val selectedCal = remember(selectedDateMillis) {
                        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                    }

                    val mondayCal = remember(selectedDateMillis) {
                        Calendar.getInstance().apply {
                            firstDayOfWeek = Calendar.MONDAY
                            timeInMillis = selectedDateMillis
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    }

                    val sundayCal = remember(mondayCal) {
                        (mondayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
                    }

                    val weekDays = remember(mondayCal) {
                        (0..6).map { dayOffset ->
                            (mondayCal.clone() as Calendar).apply {
                                add(Calendar.DAY_OF_YEAR, dayOffset)
                            }
                        }
                    }

                    fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
                        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
                    }

                    val rangeFormatter = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
                    val weekRangeText = "${rangeFormatter.format(mondayCal.time)} - ${rangeFormatter.format(sundayCal.time)}"

                    // Monday-Sunday Date Carousel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        weekDays.forEach { dayCal ->
                            val isSelected = isSameDay(dayCal, selectedCal)
                            val isToday = isSameDay(dayCal, Calendar.getInstance())
                            val dateText = SimpleDateFormat("EEE\nd", Locale.getDefault()).format(dayCal.time)

                            Surface(
                                modifier = Modifier.clickable { viewModel.setSelectedDate(dayCal.timeInMillis) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                border = when {
                                    isSelected -> null
                                    isToday -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                }
                            ) {
                                Text(
                                    text = dateText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Action Controls Row (Choose Week & Ongoing Week)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val pickedCal = Calendar.getInstance().apply {
                                            set(y, m, d, 12, 0, 0)
                                        }
                                        viewModel.setSelectedDate(pickedCal.timeInMillis)
                                    },
                                    selectedCal.get(Calendar.YEAR),
                                    selectedCal.get(Calendar.MONTH),
                                    selectedCal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.DateRange, contentDescription = "Choose Week", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Week", style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        }

                        Text(
                            text = weekRangeText,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { viewModel.resetToCurrentWeek() },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Ongoing Week", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ongoing Week", style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = dateFormatter.format(Date(now)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = timeFormatter.format(Date(now)),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    val listState = rememberLazyListState()
                    val currentIndex = remember(uiState.scheduleItems) {
                        uiState.scheduleItems.indexOfFirst { it.endTime >= now }.coerceAtLeast(0)
                    }

                    LaunchedEffect(uiState.scheduleItems) {
                        if (uiState.scheduleItems.isNotEmpty() && currentIndex >= 0) {
                            listState.scrollToItem(currentIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.scheduleItems, key = { it.id }) { item ->
                            ScheduleItemTimelineCard(
                                item = item,
                                currentTimeMillis = now,
                                onClick = { onEventClick(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun ScheduleItemTimelineCard(
    item: ScheduleItem,
    currentTimeMillis: Long,
    onClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val startTimeStr = formatter.format(Date(item.startTime))
    val endTimeStr = formatter.format(Date(item.endTime))

    val isPast = item.endTime < currentTimeMillis
    val isActive = currentTimeMillis in item.startTime..item.endTime

    val (containerColor, contentColor) = when (item) {
        is ScheduleItem.Event -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        is ScheduleItem.Transit -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        is ScheduleItem.FocusBlock -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .alpha(if (isPast) 0.45f else 1.0f)
    ) {
        // Timeline Column - Bigger font and wider column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(78.dp).fillMaxHeight()
        ) {
            Text(
                text = startTimeStr,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.5.dp)
                    .padding(vertical = 4.dp)
                    .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Card Content - Sharper corners
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(10.dp),
            border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = when (item) {
                        is ScheduleItem.Event -> Icons.Rounded.Event
                        is ScheduleItem.Transit -> Icons.Rounded.DirectionsTransit
                        is ScheduleItem.FocusBlock -> Icons.Rounded.Psychology
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).padding(top = 2.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1.2f)
                        )
                        if (isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "NOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$startTimeStr - $endTimeStr",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
