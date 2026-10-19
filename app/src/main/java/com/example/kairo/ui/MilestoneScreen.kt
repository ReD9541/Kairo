package com.example.kairo.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kairo.viewmodel.Milestone
import com.example.kairo.viewmodel.MilestoneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: MilestoneViewModel = viewModel(
        factory = MilestoneViewModel.provideFactory(application)
    )

    val milestones by viewModel.milestones.collectAsState()
    var newMilestoneTitle by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                KairoTextField(
                    value = newMilestoneTitle,
                    onValueChange = { newMilestoneTitle = it },
                    label = "Add custom milestone",
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                KairoButton(
                    text = "Add",
                    onClick = {
                        if (newMilestoneTitle.isNotBlank()) {
                            viewModel.addCustomMilestone(newMilestoneTitle)
                            newMilestoneTitle = ""
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(milestones, key = { it.id }) { milestone ->
                    val canSwipeToDelete = milestone.isCustom

                    if (canSwipeToDelete) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                    viewModel.deleteCustomMilestone(milestone.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled || dismissState.progress > 0f
                                val isActivelySwiping = isSwiping && (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart || dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                val color = if (isActivelySwiping) Color(0xFFEF4444).copy(alpha = 0.85f) else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = RoundedCornerShape(10.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    if (isActivelySwiping) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete Milestone",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        ) {
                            MilestoneCardItem(milestone = milestone, viewModel = viewModel)
                        }
                    } else {
                        MilestoneCardItem(milestone = milestone, viewModel = viewModel)
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun MilestoneCardItem(
    milestone: Milestone,
    viewModel: MilestoneViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (milestone.isCompleted) 0.5f else 1.0f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = milestone.isCompleted,
                onCheckedChange = { viewModel.toggleMilestone(milestone.id) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (milestone.dateStr.isNotBlank()) {
                    Text(
                        text = milestone.dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            IconButton(
                onClick = { viewModel.toggleMainGoal(milestone.id) },
                enabled = !milestone.isCompleted
            ) {
                Icon(
                    imageVector = if (milestone.isMainGoal && !milestone.isCompleted) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Main Goal",
                    tint = if (milestone.isMainGoal && !milestone.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}
