package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardQuizScreen(
    deckId: String,
    deckName: String,
    onBack: () -> Unit,
    flashcardViewModel: FlashcardViewModel = viewModel()
) {
    // 🔥 FIX 1: Tải dữ liệu bộ thẻ từ Firebase ngay khi vừa vào màn hình
    LaunchedEffect(deckId) {
        flashcardViewModel.fetchCards(deckId)
    }

    // 🔥 FIX 2: Lắng nghe, hễ tải xong thẻ về (có ít nhất 4 thẻ) thì bắt đầu xào bài!
    LaunchedEffect(flashcardViewModel.cards) {
        if (flashcardViewModel.cards.size >= 4 && flashcardViewModel.quizQuestions.isEmpty()) {
            flashcardViewModel.generateQuiz()
        }
    }

    val questions = flashcardViewModel.quizQuestions
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }

    val isFinished = questions.isNotEmpty() && currentIndex >= questions.size

    LaunchedEffect(selectedAnswer) {
        if (selectedAnswer != null) {
            delay(1200)
            currentIndex++
            selectedAnswer = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFinished) "Kết quả Kiểm tra" else "Kiểm tra: $deckName", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (questions.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // Cảnh báo thêm cho chắc nếu dữ liệu lỗi
                    if (flashcardViewModel.cards.isNotEmpty() && flashcardViewModel.cards.size < 4) {
                        Text("Cần ít nhất 4 thẻ để tạo bài kiểm tra!", color = Color.Red)
                    } else {
                        Text("Đang tải dữ liệu và xào bài...", color = Color.Gray)
                    }
                }
            }
            else if (isFinished) {
                val correct = flashcardViewModel.quizCorrectCount
                val wrong = flashcardViewModel.quizWrongCount
                val total = correct + wrong
                val score = if (total > 0) (correct.toFloat() / total * 100).toInt() else 0

                Column(modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯 Hoàn thành bài thi!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF9C27B0))
                    Text("Điểm của bạn: $score / 100", fontSize = 18.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Phân tích kết quả", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                            StatBar("Chính xác", correct, total, Color(0xFF4CAF50))
                            StatBar("Sai", wrong, total, Color(0xFFFF5252))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Quay lại danh sách thẻ", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
            }
            else {
                val currentQ = questions[currentIndex]

                val progressValue = if (questions.isNotEmpty()) (currentIndex.toFloat() / questions.size) else 0f
                LinearProgressIndicator(
                    progress = progressValue,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF9C27B0), trackColor = Color(0xFFF3E5F5)
                )

                Text("Câu ${currentIndex + 1} / ${questions.size}", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(currentQ.questionText, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val alphabet = listOf("A", "B", "C", "D")
                    currentQ.options.forEachIndexed { index, option ->

                        val isSelected = selectedAnswer == option
                        val isCorrect = option == currentQ.correctAnswer

                        val containerColor = when {
                            selectedAnswer == null -> Color.White
                            isCorrect -> Color(0xFFE8F5E9)
                            isSelected && !isCorrect -> Color(0xFFFFEBEE)
                            else -> Color.White
                        }
                        val borderColor = when {
                            selectedAnswer == null -> Color.LightGray
                            isCorrect -> Color(0xFF4CAF50)
                            isSelected && !isCorrect -> Color(0xFFFF5252)
                            else -> Color.LightGray
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable {
                                    if (selectedAnswer == null) {
                                        selectedAnswer = option
                                        flashcardViewModel.recordQuizAnswer(isCorrect)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = containerColor,
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${alphabet[index]}.", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(30.dp))
                                Text(option, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}