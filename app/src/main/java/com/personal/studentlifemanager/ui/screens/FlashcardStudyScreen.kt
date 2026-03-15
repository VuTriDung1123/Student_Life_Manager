package com.personal.studentlifemanager.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.data.model.Flashcard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(
    deckId: String,
    deckName: String,
    onBack: () -> Unit,
    flashcardViewModel: FlashcardViewModel = viewModel()
) {
    LaunchedEffect(deckId) {
        flashcardViewModel.fetchDueCards(deckId)
    }

    val dueCards = flashcardViewModel.dueCards
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    // Đã học xong toàn bộ thẻ trong mảng dueCards chưa?
    val isSessionFinished = dueCards.isNotEmpty() && currentCardIndex >= dueCards.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSessionFinished) "Kết quả ôn tập" else "Học thẻ: $deckName", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // TRƯỜNG HỢP 1: BỘ THẺ TRỐNG BÓC (Chưa tạo thẻ nào)
            if (dueCards.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bộ thẻ này chưa có từ vựng nào!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Hãy quay lại và ấn dấu + để thêm thẻ nhé.", color = Color.Gray)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onBack) { Text("Quay lại danh sách thẻ") }
                    }
                }
            }

            // TRƯỜNG HỢP 2: ĐÃ HỌC XONG PHIÊN HIỆN TẠI -> HIỆN THỐNG KÊ BIỂU ĐỒ
            else if (isSessionFinished) {
                val stats = flashcardViewModel.sessionStats
                val totalStudied = stats.values.sum()

                Column(modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉 Hoàn thành xuất sắc!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text("Bạn vừa ôn tập $totalStudied thẻ", fontSize = 16.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Vẽ biểu đồ Bar Chart đơn giản
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Phân tích độ khó", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                            StatBar("Chưa thuộc", stats[1] ?: 0, totalStudied, Color(0xFFFF5252))
                            StatBar("Khó", stats[2] ?: 0, totalStudied, Color(0xFFFF9800))
                            StatBar("Tốt", stats[3] ?: 0, totalStudied, Color(0xFF4CAF50))
                            StatBar("Dễ", stats[4] ?: 0, totalStudied, Color(0xFF2196F3))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onBack, // Bấm Hoàn tất sẽ văng ra lại danh sách thẻ
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Hoàn tất", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // TRƯỜNG HỢP 3: ĐANG HỌC LẬT THẺ
            else {
                Text(
                    text = "Thẻ ${currentCardIndex + 1} / ${dueCards.size}",
                    fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentCard = dueCards[currentCardIndex]
                val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f)

                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
                        .clickable { isFlipped = !isFlipped },
                    colors = CardDefaults.cardColors(containerColor = if (isFlipped) Color(0xFFFFF3E0) else Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        if (rotation <= 90f) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                                Text("Mặt trước", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(currentCard.frontText, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationY = 180f }) {
                                Text("Mặt sau", fontSize = 12.sp, color = Color(0xFFFF9800))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(currentCard.backText, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                if (currentCard.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("📝 ${currentCard.note}", fontSize = 14.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isFlipped) {
                    Row(modifier = Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StudyActionButton("Chưa thuộc", Color(0xFFFF5252), 1) { flashcardViewModel.rateCard(currentCard, 1); currentCardIndex++; isFlipped = false }
                        StudyActionButton("Khó", Color(0xFFFF9800), 2) { flashcardViewModel.rateCard(currentCard, 2); currentCardIndex++; isFlipped = false }
                        StudyActionButton("Tốt", Color(0xFF4CAF50), 3) { flashcardViewModel.rateCard(currentCard, 3); currentCardIndex++; isFlipped = false }
                        StudyActionButton("Dễ", Color(0xFF2196F3), 4) { flashcardViewModel.rateCard(currentCard, 4); currentCardIndex++; isFlipped = false }
                    }
                } else {
                    Button(
                        onClick = { isFlipped = true },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Hiện đáp án", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// Hàm vẽ thanh trạng thái (Thống kê kết quả)
@Composable
fun StatBar(label: String, count: Int, total: Int, color: Color) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Box(modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.LightGray.copy(alpha=0.3f))) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(color))
        }
        Text(count.toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun StudyActionButton(text: String, color: Color, rating: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(80.dp).fillMaxHeight(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = rating.toString(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = text, fontSize = 10.sp)
        }
    }
}