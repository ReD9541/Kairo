package com.example.kairo.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kairo.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModel.provideFactory(application)
    )

    val wakeUpTime by viewModel.wakeUpTime.collectAsState()
    val sleepTime by viewModel.sleepTime.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val isValidating by viewModel.isValidating.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    var tempWakeUpTime by remember(wakeUpTime) { mutableStateOf(wakeUpTime) }
    var tempSleepTime by remember(sleepTime) { mutableStateOf(sleepTime) }
    var tempUserName by remember(userName) { mutableStateOf(userName) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(validationResult) {
        validationResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearValidationResult()
        }
    }

    val calculatedSleep = remember(tempWakeUpTime, tempSleepTime) {
        try {
            val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val wake = format.parse(tempWakeUpTime)
            val sleep = format.parse(tempSleepTime)
            if (wake != null && sleep != null) {
                var diff = wake.time - sleep.time
                if (diff < 0) diff += 24 * 60 * 60 * 1000
                val hours = diff / (60 * 60 * 1000)
                val mins = (diff / (60 * 1000)) % 60
                "${hours}h ${mins}m"
            } else ""
        } catch (e: Exception) {
            ""
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
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tempUserName.isNotBlank()) tempUserName.take(1).uppercase() else "U",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    KairoTextField(
                        value = tempUserName,
                        onValueChange = { 
                            tempUserName = it
                            viewModel.saveUserName(it)
                        },
                        label = "Your Name",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Routine & Location Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sleep Routine",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SleepScheduleDial(
                        wakeUpTime = tempWakeUpTime,
                        sleepTime = tempSleepTime,
                        onWakeUpChange = { tempWakeUpTime = it },
                        onSleepChange = { tempSleepTime = it }
                    )

                    Text(
                        text = "Calculated Sleep: $calculatedSleep",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            KairoButton(
                text = "Save Profile Settings",
                onClick = { 
                    viewModel.saveUserName(tempUserName)
                    viewModel.saveRoutine(tempWakeUpTime, tempSleepTime)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isValidating
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
    }
}