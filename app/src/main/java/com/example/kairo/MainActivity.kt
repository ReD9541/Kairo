package com.example.kairo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import com.example.kairo.ui.theme.DarkBackgroundStart
import com.example.kairo.ui.theme.DarkBackgroundEnd
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kairo.ui.*
import com.example.kairo.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KairoTheme {
                val backStack = remember { mutableStateListOf<Any>(SplashRoute) }
                
                val application = applicationContext as android.app.Application
                val dashboardViewModel: com.example.kairo.viewmodel.DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.example.kairo.viewmodel.DashboardViewModel.provideFactory(application)
                )

                var showAddEventPopup by remember { mutableStateOf(false) }
                var selectedEventId by remember { mutableStateOf<String?>(null) }
                val currentRoute = backStack.lastOrNull() ?: SplashRoute

                val headerTitle = when (currentRoute) {
                    is MilestoneRoute -> "WEEKLY MILESTONES"
                    is UserRoute -> "YOUR PROFILE"
                    is SettingsRoute -> "APP SETTINGS"
                    else -> "LET'S GET YOU ON TRACK"
                }

                AnimatedStarsBackground {
                    @OptIn(ExperimentalMaterial3Api::class)
                    Scaffold(
                        topBar = {
                            if (currentRoute !is SplashRoute) {
                                TopAppBar(
                                    title = { 
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = headerTitle,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (currentRoute !is SplashRoute && currentRoute !is EventDetailRoute) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 20.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF14141E) // Dark opaque background
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CustomNavBarItem(
                                            selected = currentRoute is DashboardRoute,
                                            onClick = { 
                                                if (currentRoute !is DashboardRoute) {
                                                    backStack.clear()
                                                    backStack.add(DashboardRoute)
                                                }
                                                dashboardViewModel.loadSchedule()
                                            },
                                            selectedIcon = Icons.Filled.Home,
                                            unselectedIcon = Icons.Outlined.Home,
                                            label = "Home",
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        CustomNavBarItem(
                                            selected = currentRoute is MilestoneRoute,
                                            onClick = { 
                                                if (currentRoute !is MilestoneRoute) {
                                                    backStack.clear()
                                                    backStack.add(MilestoneRoute)
                                                }
                                            },
                                            selectedIcon = Icons.Filled.FactCheck,
                                            unselectedIcon = Icons.Outlined.FactCheck,
                                            label = "Milestones",
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        // Primary Squircle FAB
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .size(52.dp)
                                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { showAddEventPopup = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Add, "Add Event", tint = Color(0xFF06080D), modifier = Modifier.size(28.dp))
                                        }

                                        CustomNavBarItem(
                                            selected = currentRoute is UserRoute,
                                            onClick = { 
                                                if (currentRoute !is UserRoute) {
                                                    backStack.clear()
                                                    backStack.add(UserRoute)
                                                }
                                            },
                                            selectedIcon = Icons.Filled.Person,
                                            unselectedIcon = Icons.Outlined.Person,
                                            label = "Profile",
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        CustomNavBarItem(
                                            selected = currentRoute is SettingsRoute,
                                            onClick = { 
                                                if (currentRoute !is SettingsRoute) {
                                                    backStack.clear()
                                                    backStack.add(SettingsRoute)
                                                }
                                            },
                                            selectedIcon = Icons.Filled.Settings,
                                            unselectedIcon = Icons.Outlined.Settings,
                                            label = "Settings",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        },
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            AnimatedContent(
                                targetState = currentRoute,
                                transitionSpec = {
                                    slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) togetherWith
                                    slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                                },
                                label = "ScreenTransition"
                            ) { route ->
                                when (route) {
                                    is SplashRoute -> SplashScreen(
                                        onSplashFinished = {
                                            backStack.clear()
                                            backStack.add(DashboardRoute)
                                        }
                                    )
                                    is DashboardRoute -> DashboardScreen(
                                        viewModel = dashboardViewModel,
                                        onEventClick = { itemId -> selectedEventId = itemId }
                                    )
                                    is MilestoneRoute -> MilestoneScreen()
                                    is UserRoute -> UserScreen()
                                    is SettingsRoute -> SettingsScreen()
                                    else -> Text("Unknown Route")
                                }
                            }
                        }
                    }

                    if (selectedEventId != null) {
                        EventDetailPopup(
                            itemId = selectedEventId!!,
                            viewModel = dashboardViewModel,
                            onDismiss = { selectedEventId = null }
                        )
                    }

                    if (showAddEventPopup) {
                        AddEventPopup(
                            onDismiss = { showAddEventPopup = false },
                            viewModel = dashboardViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomNavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray

    Box(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            // Light flare background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            // Top solid indicator line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                    .align(Alignment.TopCenter)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}
