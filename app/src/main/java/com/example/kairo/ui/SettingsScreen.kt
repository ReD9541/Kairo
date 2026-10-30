package com.example.kairo.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kairo.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(application)
    )

    val loggedInUserEmail by viewModel.loggedInUserEmail.collectAsState()

    val workUrl by viewModel.workCalendarUrl.collectAsState()
    val collegeUrl by viewModel.collegeCalendarUrl.collectAsState()
    val personalUrl by viewModel.personalCalendarUrl.collectAsState()
    val homeLocation by viewModel.homeLocation.collectAsState()
    val workLocation by viewModel.workLocation.collectAsState()
    val collegeLocation by viewModel.collegeLocation.collectAsState()
    val transportMode by viewModel.transportMode.collectAsState()
    val pomodoroWork by viewModel.pomodoroWorkDuration.collectAsState()
    val pomodoroBreak by viewModel.pomodoroBreakDuration.collectAsState()

    val morningMins by viewModel.morningRoutineMins.collectAsState()
    val decompressMins by viewModel.postCommuteDecompressMins.collectAsState()
    val studyPrepMins by viewModel.studyPrepBufferMins.collectAsState()
    val lunchMins by viewModel.lunchDurationMins.collectAsState()
    val dinnerMins by viewModel.dinnerPrepAndEatMins.collectAsState()
    val minStudyMins by viewModel.minStudySlotMins.collectAsState()

    val isValidatingWork by viewModel.isValidatingWork.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    
    val homeSuggestions by viewModel.homeLocationSuggestions.collectAsState()
    val workSuggestions by viewModel.workLocationSuggestions.collectAsState()
    val collegeSuggestions by viewModel.collegeLocationSuggestions.collectAsState()

    var tempWorkUrl by remember(workUrl) { mutableStateOf(workUrl) }
    var tempCollegeUrl by remember(collegeUrl) { mutableStateOf(collegeUrl) }
    var tempPersonalUrl by remember(personalUrl) { mutableStateOf(personalUrl) }
    var tempHomeLocation by remember(homeLocation) { mutableStateOf(homeLocation) }
    var tempWorkLocation by remember(workLocation) { mutableStateOf(workLocation) }
    var tempCollegeLocation by remember(collegeLocation) { mutableStateOf(collegeLocation) }
    var tempTransportMode by remember(transportMode) { mutableStateOf(transportMode) }
    var tempPomodoroWork by remember(pomodoroWork) { mutableStateOf(pomodoroWork) }
    var tempPomodoroBreak by remember(pomodoroBreak) { mutableStateOf(pomodoroBreak) }

    var tempMorning by remember(morningMins) { mutableStateOf(morningMins) }
    var tempDecompress by remember(decompressMins) { mutableStateOf(decompressMins) }
    var tempStudyPrep by remember(studyPrepMins) { mutableStateOf(studyPrepMins) }
    var tempLunch by remember(lunchMins) { mutableStateOf(lunchMins) }
    var tempDinner by remember(dinnerMins) { mutableStateOf(dinnerMins) }
    var tempMinStudy by remember(minStudyMins) { mutableStateOf(minStudyMins) }

    var expandedHome by remember { mutableStateOf(false) }
    var expandedWork by remember { mutableStateOf(false) }
    var expandedCollege by remember { mutableStateOf(false) }
    var expandedTransport by remember { mutableStateOf(false) }
    val transportOptions = listOf("All Public Transport", "Train Only", "Bus Only")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(validationResult) {
        validationResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearValidationResult()
        }
    }

    LaunchedEffect(tempWorkLocation) {
        if (tempWorkLocation.isNotBlank() && tempWorkLocation != workLocation) {
            viewModel.fetchWorkSuggestions(tempWorkLocation)
            expandedWork = true
        } else {
            expandedWork = false
        }
    }

    LaunchedEffect(tempHomeLocation) {
        if (tempHomeLocation.isNotBlank() && tempHomeLocation != homeLocation) {
            viewModel.fetchHomeSuggestions(tempHomeLocation)
            expandedHome = true
        } else {
            expandedHome = false
        }
    }

    LaunchedEffect(tempCollegeLocation) {
        if (tempCollegeLocation.isNotBlank() && tempCollegeLocation != collegeLocation) {
            viewModel.fetchCollegeSuggestions(tempCollegeLocation)
            expandedCollege = true
        } else {
            expandedCollege = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Cloud Sync Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (loggedInUserEmail != null) "Cloud Backup Active" else "Local Storage Only",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (loggedInUserEmail != null) "Synced with Supabase ($loggedInUserEmail)" else "Sign in on Profile tab to enable cross-device sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 1: Calendars
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(35.dp)
                ) {
                    Text("Calendars", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    KairoTextField(
                        value = tempWorkUrl,
                        onValueChange = { tempWorkUrl = it },
                        label = "Work Calendar URL (iCal)",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    KairoTextField(
                        value = tempCollegeUrl,
                        onValueChange = { tempCollegeUrl = it },
                        label = "College Calendar URL (iCal)",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    KairoTextField(
                        value = tempPersonalUrl,
                        onValueChange = { tempPersonalUrl = it },
                        label = "Personal Calendar URL (iCal)",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Section 2: Addresses
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Text("Addresses", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    ExposedDropdownMenuBox(
                        expanded = expandedHome,
                        onExpandedChange = { expandedHome = it }
                    ) {
                        KairoTextField(
                            value = tempHomeLocation,
                            onValueChange = { tempHomeLocation = it },
                            label = "Home Location",
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedHome && homeSuggestions.isNotEmpty(),
                            onDismissRequest = { expandedHome = false }
                        ) {
                            homeSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        tempHomeLocation = suggestion
                                        expandedHome = false
                                        viewModel.clearHomeSuggestions()
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedWork,
                        onExpandedChange = { expandedWork = it }
                    ) {
                        KairoTextField(
                            value = tempWorkLocation,
                            onValueChange = { tempWorkLocation = it },
                            label = "Work Location",
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWork && workSuggestions.isNotEmpty(),
                            onDismissRequest = { expandedWork = false }
                        ) {
                            workSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        tempWorkLocation = suggestion
                                        expandedWork = false
                                        viewModel.clearWorkSuggestions()
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedCollege,
                        onExpandedChange = { expandedCollege = it }
                    ) {
                        KairoTextField(
                            value = tempCollegeLocation,
                            onValueChange = { tempCollegeLocation = it },
                            label = "College Location",
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCollege && collegeSuggestions.isNotEmpty(),
                            onDismissRequest = { expandedCollege = false }
                        ) {
                            collegeSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        tempCollegeLocation = suggestion
                                        expandedCollege = false
                                        viewModel.clearCollegeSuggestions()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Time & Transport
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Text("Time & Transport", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedTransport,
                        onExpandedChange = { expandedTransport = it }
                    ) {
                        KairoTextField(
                            value = tempTransportMode,
                            onValueChange = { },
                            readOnly = true,
                            label = "Preferred Transport Mode",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTransport) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTransport,
                            onDismissRequest = { expandedTransport = false }
                        ) {
                            transportOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        tempTransportMode = selectionOption
                                        expandedTransport = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        KairoTextField(
                            value = tempPomodoroWork,
                            onValueChange = { tempPomodoroWork = it },
                            label = "Focus (min)",
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        KairoTextField(
                            value = tempPomodoroBreak,
                            onValueChange = { tempPomodoroBreak = it },
                            label = "Break (min)",
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // Section 4: Human Routine & Buffers
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Text("Human Routine & Buffers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        KairoTextField(
                            value = tempMorning,
                            onValueChange = { tempMorning = it },
                            label = "Morning Routine (min)",
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        KairoTextField(
                            value = tempDecompress,
                            onValueChange = { tempDecompress = it },
                            label = "Wind-Down / Unwind (min)",
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        KairoTextField(
                            value = tempLunch,
                            onValueChange = { tempLunch = it },
                            label = "Lunch Time (min)",
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        KairoTextField(
                            value = tempDinner,
                            onValueChange = { tempDinner = it },
                            label = "Dinner Prep & Eat (min)",
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        KairoTextField(
                            value = tempStudyPrep,
                            onValueChange = { tempStudyPrep = it },
                            label = "Study Setup Prep (min)",
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        KairoTextField(
                            value = tempMinStudy,
                            onValueChange = { tempMinStudy = it },
                            label = "Min Study Slot (min)",
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            KairoButton(
                text = "Save All Settings",
                onClick = { 
                    viewModel.saveAllSettings(
                        workUrl = tempWorkUrl,
                        collegeUrl = tempCollegeUrl,
                        personalUrl = tempPersonalUrl,
                        homeLoc = tempHomeLocation,
                        workLoc = tempWorkLocation,
                        collegeLoc = tempCollegeLocation,
                        mode = tempTransportMode,
                        pomodoroWork = tempPomodoroWork,
                        pomodoroBreak = tempPomodoroBreak,
                        morning = tempMorning,
                        decompress = tempDecompress,
                        studyPrep = tempStudyPrep,
                        lunch = tempLunch,
                        dinner = tempDinner,
                        minStudy = tempMinStudy
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isValidatingWork,
                isLoading = isValidatingWork,
                loadingText = "Validating & Saving..."
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
    }
}
