package com.personal.studentlifemanager.features.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.features.dashboard.UserProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    onBack: () -> Unit,
    viewModel: HabitViewModel = viewModel(),
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.habits) { habit ->
                    HabitCard(
                        habit = habit,
                        logs = viewModel.getLogsForHabit(habit.id),
                        onCheckIn = {
                            viewModel.checkIn(habit)
                            userProfileViewModel.addExp(10)
                        },
                        onDelete = { viewModel.deleteHabit(habit.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddHabitDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, freq ->
                    viewModel.addHabit(title, freq) {
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun HabitCard(habit: Habit, logs: List<HabitLog>, onCheckIn: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = habit.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "🔥 Streak: ${habit.currentStreak} | Best: ${habit.bestStreak}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val isCheckedInToday = logs.any { it.dateMs == today && it.status == "COMPLETED" }

                IconButton(
                    onClick = onCheckIn,
                    enabled = !isCheckedInToday
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Check In",
                        tint = if (isCheckedInToday) Color(0xFF4CAF50) else Color.LightGray,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HabitHeatmap(logs)
        }
    }
}

@Composable
fun HabitHeatmap(logs: List<HabitLog>) {
    // A simple heatmap implementation for the last 7 days
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Go back 6 days to show 7 days total including today
        cal.add(Calendar.DAY_OF_YEAR, -6)

        val format = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 0..6) {
            val dateMs = cal.timeInMillis
            val label = format.format(cal.time)
            val isCompleted = logs.any { it.dateMs == dateMs && it.status == "COMPLETED" }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = if (isCompleted) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = label, fontSize = 12.sp, color = Color.Gray)
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Habit") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Habit Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onAdd(title, "DAILY") }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
