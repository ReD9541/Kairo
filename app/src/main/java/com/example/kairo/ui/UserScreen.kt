package com.example.kairo.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val isValidating by viewModel.isValidating.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    var tempWakeUpTime by remember(wakeUpTime) { mutableStateOf(wakeUpTime) }
    var tempSleepTime by remember(sleepTime) { mutableStateOf(sleepTime) }
    var tempUserName by remember(userName) { mutableStateOf(userName) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(validationResult) {
        validationResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearValidationResult()
        }
    }

    LaunchedEffect(authError) {
        authError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAuthError()
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

            // Supabase Account Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (currentUserEmail != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = "Cloud Status",
                            tint = if (currentUserEmail != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (currentUserEmail != null) "Supabase Cloud Account" else "Cloud Sync & Backup",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (currentUserEmail != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Logged in as",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = currentUserEmail!!,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Logged In",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncNow() },
                                modifier = Modifier.weight(1f),
                                enabled = !isAuthLoading
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now")
                            }

                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                modifier = Modifier.weight(1f),
                                enabled = !isAuthLoading
                            ) {
                                Text("Sign Out")
                            }
                        }
                    } else {
                        Text(
                            text = "Log in or create an account to save your settings to Supabase and keep your instance synced everywhere.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isAuthLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.signIn(emailInput, passwordInput) },
                                modifier = Modifier.weight(1f),
                                enabled = !isAuthLoading
                            ) {
                                Text("Sign In")
                            }

                            OutlinedButton(
                                onClick = { viewModel.signUp(emailInput, passwordInput) },
                                modifier = Modifier.weight(1f),
                                enabled = !isAuthLoading
                            ) {
                                Text("Sign Up")
                            }
                        }
                    }
                }
            }

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
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
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
