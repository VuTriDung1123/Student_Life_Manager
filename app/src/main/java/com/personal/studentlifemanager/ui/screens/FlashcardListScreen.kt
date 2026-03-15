package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.data.model.Flashcard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardListScreen(
    deckId: String,
    deckName: String,
    onBack: () -> Unit,
    onNavigateToStudy: (String, String) -> Unit, // 🔥 1. THÊM THAM SỐ NÀY
    flashcardViewModel: FlashcardViewModel = viewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Flashcard?>(null) }

    LaunchedEffect(deckId) {
        flashcardViewModel.fetchCards(deckId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deckName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingCard = null; showDialog = true },
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, "Thêm thẻ") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            val cards = flashcardViewModel.cards.sortedBy {
                when(it.status) { "LEARNING" -> 0; "NEW" -> 1; "REVIEW" -> 2; else -> 3 }
            }

            // 🔥 2. NÚT HỌC THẺ TO BỰ DÀNH CHO BẠN
            if (cards.isNotEmpty()) {
                Button(
                    onClick = { onNavigateToStudy(deckId, deckName) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("🚀 HỌC BỘ THẺ NÀY", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (cards.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Bộ thẻ đang trống. Thêm từ vựng ngay nào! 🚀", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cards) { card ->
                        CardItem(
                            card = card,
                            onEdit = { editingCard = card; showDialog = true },
                            onDelete = { flashcardViewModel.deleteCard(card.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showDialog) {
        AddEditCardDialog(
            card = editingCard,
            onDismiss = { showDialog = false },
            onSave = { front, back, note ->
                if (editingCard == null) {
                    flashcardViewModel.addCard(deckId, front, back, note) { showDialog = false }
                } else {
                    val updated = editingCard!!.copy(frontText = front, backText = back, note = note)
                    flashcardViewModel.updateCard(updated) { showDialog = false }
                }
            }
        )
    }
}

@Composable
fun CardItem(card: Flashcard, onEdit: () -> Unit, onDelete: () -> Unit) {
    // 🔥 ĐỊNH NGHĨA MÀU SẮC DỰA VÀO TRẠNG THÁI THẺ
    val (statusColor, statusText) = when(card.status) {
        "LEARNING" -> Pair(Color(0xFFFF5252), "Đang học")  // Đỏ
        "NEW"      -> Pair(Color(0xFF2196F3), "Thẻ Mới")   // Xanh dương
        "REVIEW"   -> Pair(Color(0xFF4CAF50), "Đã thuộc")  // Xanh lá
        else       -> Pair(Color.Gray, "")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Vạch màu đánh dấu trạng thái nằm ở cạnh trái
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(statusColor))

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Q: ${card.frontText}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("A: ${card.backText}", fontSize = 15.sp, color = Color.DarkGray)

                        if (card.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("📝 ${card.note}", fontSize = 12.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }

                        // Hiển thị chữ trạng thái thẻ
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Trạng thái: $statusText", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }

                    Column {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Sửa", tint = Color.Gray) }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Xóa", tint = Color.Red.copy(alpha = 0.7f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCardDialog(card: Flashcard?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var front by remember { mutableStateOf(card?.frontText ?: "") }
    var back by remember { mutableStateOf(card?.backText ?: "") }
    var note by remember { mutableStateOf(card?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (card == null) "Thêm Thẻ Nhớ" else "Sửa Thẻ Nhớ", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = front, onValueChange = { front = it },
                    label = { Text("Mặt trước (Câu hỏi / Từ vựng)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = back, onValueChange = { back = it },
                    label = { Text("Mặt sau (Trả lời / Nghĩa)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú (Không bắt buộc)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (front.isNotBlank() && back.isNotBlank()) onSave(front, back, note) }) {
                Text("Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}