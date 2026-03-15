package com.personal.studentlifemanager.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onBack: () -> Unit, // 🔥 ĐÃ SỬA THÀNH DẤU HAI CHẤM
    flashcardViewModel: FlashcardViewModel = viewModel()
) {
    // Tải dữ liệu các thẻ cần ôn tập
    LaunchedEffect(deckId) {
        flashcardViewModel.fetchDueCards(deckId)
    }

    val dueCards = flashcardViewModel.dueCards
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Học thẻ: $deckName", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hiển thị Tiến độ học
            Text(
                text = "Thẻ ${if (dueCards.isEmpty()) 0 else currentCardIndex + 1} / ${dueCards.size}",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (dueCards.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Tuyệt vời! Bạn đã hoàn thành việc ôn tập bộ thẻ này hôm nay! 🎉", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            } else if (currentCardIndex < dueCards.size) {
                val currentCard = dueCards[currentCardIndex]

                val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
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
                                Text(
                                    text = currentCard.frontText,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationY = 180f }) {
                                Text("Mặt sau", fontSize = 12.sp, color = Color(0xFFFF9800))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = currentCard.backText,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
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
                        StudyActionButton("Chưa thuộc", Color(0xFFFF5252), 1) { flashcardViewModel.rateCard(currentCard, 1); nextCard(dueCards.size, { currentCardIndex++; isFlipped = false }) }
                        StudyActionButton("Khó", Color(0xFFFF9800), 2) { flashcardViewModel.rateCard(currentCard, 2); nextCard(dueCards.size, { currentCardIndex++; isFlipped = false }) }
                        StudyActionButton("Tốt", Color(0xFF4CAF50), 3) { flashcardViewModel.rateCard(currentCard, 3); nextCard(dueCards.size, { currentCardIndex++; isFlipped = false }) }
                        StudyActionButton("Dễ", Color(0xFF2196F3), 4) { flashcardViewModel.rateCard(currentCard, 4); nextCard(dueCards.size, { currentCardIndex++; isFlipped = false }) }
                    }
                } else {
                    Button(
                        onClick = { isFlipped = true },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Hiện đáp án", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Tuyệt vời! Bạn đã hoàn thành việc ôn tập bộ thẻ này hôm nay! 🎉", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

fun nextCard(totalCards: Int, onNext: () -> Unit) {
    onNext()
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