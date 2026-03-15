package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.data.model.Deck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardDecksScreen(
    onBack: () -> Unit,
    onNavigateToDeck: (String, String) -> Unit,
    flashcardViewModel: FlashcardViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Bộ Thẻ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Tạo bộ thẻ mới")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            val decks = flashcardViewModel.decks

            if (decks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có bộ thẻ nào. Bấm dấu + để tạo nhé!", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(decks) { deck ->
                        DeckItem(
                            deck = deck,
                            onNavigateToDeck = onNavigateToDeck, // 🔥 TRUYỀN HÀM NÀY XUỐNG
                            onDelete = { flashcardViewModel.deleteDeck(deck.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CreateDeckDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, desc ->
                flashcardViewModel.createDeck(name, desc) { showAddDialog = false }
            }
        )
    }
}

@Composable
fun DeckItem(
    deck: Deck,
    onNavigateToDeck: (String, String) -> Unit,
    onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigateToDeck(deck.id, deck.name) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // Màu xanh nhạt
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFF2196F3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(deck.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D47A1))
                if (deck.description.isNotBlank()) {
                    Text(deck.description, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn", tint = Color.Gray)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Xóa bộ thẻ", color = Color.Red) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateDeckDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Bộ Thẻ Mới", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên bộ thẻ (VD: Tiếng Anh Giao Tiếp)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text("Mô tả (Tùy chọn)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, desc) }) { Text("Tạo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}