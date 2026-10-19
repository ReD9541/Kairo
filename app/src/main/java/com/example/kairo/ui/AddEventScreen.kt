package com.example.kairo.ui

import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kairo.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventPopup(
    onDismiss: () -> Unit,
    viewModel: DashboardViewModel
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf(12) }
    var startMin by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(13) }
    var endMin by remember { mutableStateOf(0) }
    
    val isAddingEvent by viewModel.isAddingEvent.collectAsState()
    val addEventError by viewModel.addEventError.collectAsState()
    val locationSuggestions by viewModel.locationSuggestions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(addEventError) {
        addEventError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddEventError()
        }
    }

    LaunchedEffect(location) {
        if (location.isNotBlank()) {
            viewModel.fetchSuggestions(location)
            expanded = true
        } else {
            expanded = false
        }
    }

    val currentCal = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(currentCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(currentCal.get(Calendar.DAY_OF_MONTH)) }

    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    fun formatTime(h: Int, m: Int): String {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        }
        return formatter.format(c.time)
    }

    fun formatDate(y: Int, m: Int, d: Int): String {
        val c = Calendar.getInstance().apply {
            set(y, m, d)
        }
        val df = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        return df.format(c.time)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Manual Event", 
                        style = MaterialTheme.typography.titleLarge, 
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    KairoTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Event Title",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        KairoTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = "Location (Optional)",
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expanded && locationSuggestions.isNotEmpty(),
                            onDismissRequest = { expanded = false }
                        ) {
                            locationSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        location = suggestion
                                        expanded = false
                                        viewModel.clearSuggestions()
                                    }
                                )
                            }
                        }
                    }

                    KairoTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description (Optional)",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 2
                    )

                    KairoButton(
                        text = "Date: ${formatDate(selectedYear, selectedMonth, selectedDay)}",
                        onClick = {
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    selectedYear = y
                                    selectedMonth = m
                                    selectedDay = d
                                },
                                selectedYear, selectedMonth, selectedDay
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KairoButton(
                            text = "Start: ${formatTime(startHour, startMin)}",
                            onClick = {
                                TimePickerDialog(context, { _: TimePicker, h: Int, m: Int ->
                                    startHour = h
                                    startMin = m
                                }, startHour, startMin, false).show()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        KairoButton(
                            text = "End: ${formatTime(endHour, endMin)}",
                            onClick = {
                                TimePickerDialog(context, { _: TimePicker, h: Int, m: Int ->
                                    endHour = h
                                    endMin = m
                                }, endHour, endMin, false).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        KairoButton(
                            text = "Cancel",
                            onClick = onDismiss
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        KairoButton(
                            text = "Save Event",
                            onClick = {
                                val sCal = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDay, startHour, startMin, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val eCal = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDay, endHour, endMin, 0)
                                    set(Calendar.MILLISECOND, 0)
                                    if (timeInMillis < sCal.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
                                }
                                if (title.isNotBlank()) {
                                    viewModel.addManualEvent(
                                        title = title,
                                        location = location,
                                        description = description,
                                        startTime = sCal.timeInMillis,
                                        endTime = eCal.timeInMillis,
                                        onSuccess = onDismiss
                                    )
                                }
                            },
                            enabled = title.isNotBlank(),
                            isLoading = isAddingEvent,
                            loadingText = "Validating..."
                        )
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            )
        }
    }
}
