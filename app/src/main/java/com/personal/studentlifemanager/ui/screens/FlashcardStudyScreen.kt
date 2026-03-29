package com.personal.studentlifemanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(
    deckId: String,
    deckName: String,
    onBack: () -> Unit,
    flashcardViewModel: FlashcardViewModel = viewModel()
) {
    // Tải dữ liệu bộ thẻ khi mở màn hình
    LaunchedEffect(deckId) {
        flashcardViewModel.fetchDueCards(deckId)
    }

    val dueCards = flashcardViewModel.dueCards
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    // Kiểm tra trạng thái hoàn thành phiên học
    val isSessionFinished = dueCards.isNotEmpty() && currentCardIndex >= dueCards.size

    // Quản lý trạng thái kéo thả theo trục X
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSessionFinished) "Kết quả ôn tập" else "Học thẻ: $deckName", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Trở về") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Xử lý khi bộ thẻ trống
            if (dueCards.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bộ thẻ này chưa có từ vựng nào.", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onBack) { Text("Quay lại danh sách thẻ") }
                    }
                }
            }
            // Xử lý khi đã học xong toàn bộ thẻ
            else if (isSessionFinished) {
                val stats = flashcardViewModel.sessionStats
                val totalStudied = stats.values.sum()

                Column(modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hoàn thành bài học", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text("Số lượng thẻ đã ôn tập: $totalStudied", fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(32.dp))

                    // Vẽ biểu đồ phân tích độ khó
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
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Hoàn tất", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Trạng thái đang hiển thị và học thẻ
            else {
                Text(
                    text = "Thẻ ${currentCardIndex + 1} / ${dueCards.size}",
                    fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentCard = dueCards[currentCardIndex]
                val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f)

                // Tính toán độ trong suốt và màu sắc của lớp phủ khi kéo
                val overlayAlpha = (abs(offsetX.value) / 500f).coerceIn(0f, 0.4f)
                val overlayColor = if (offsetX.value > 0) Color(0xFF4CAF50) else Color(0xFFFF5252)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        // Đặt pointerInput trước graphicsLayer để tránh lật ngược trục X khi thẻ lật mặt sau
                        .pointerInput(isFlipped) {
                            if (!isFlipped) return@pointerInput

                            detectDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (offsetX.value > 250f) {
                                            // Chuyển thẻ sang trạng thái "Tốt" khi kéo đủ xa sang phải
                                            offsetX.animateTo(1000f, tween(300))
                                            flashcardViewModel.rateCard(currentCard, 3)
                                            currentCardIndex++
                                            isFlipped = false
                                            offsetX.snapTo(0f)
                                        } else if (offsetX.value < -250f) {
                                            // Chuyển thẻ sang trạng thái "Chưa thuộc" khi kéo đủ xa sang trái
                                            offsetX.animateTo(-1000f, tween(300))
                                            flashcardViewModel.rateCard(currentCard, 1)
                                            currentCardIndex++
                                            isFlipped = false
                                            offsetX.snapTo(0f)
                                        } else {
                                            // Đưa thẻ về vị trí cũ nếu khoảng cách kéo không đủ
                                            offsetX.animateTo(0f, spring())
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                    }
                                }
                            )
                        }
                        // Chỉnh sửa hiệu ứng nghiêng: Điểm neo ở đáy thẻ, đầu thẻ di chuyển
                        .graphicsLayer {
                            rotationZ = offsetX.value / 25f // Hiệu ứng nghiêng 3D kiểu Tinder
                            cameraDistance = 12f * density // Khoảng cách camera để tạo hiệu ứng 3D
                            // Đặt điểm neo ở đáy giữa thẻ (TransformOrigin(x=0.5f, y=1f))
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        // Hiệu ứng lật thẻFront/Back tách biệt với hiệu ứng nghiêng khi kéo
                        .graphicsLayer {
                            rotationY = rotation // Hiệu ứng lật thẻ Front/Back
                            cameraDistance = 12f * density // Khoảng cách camera để tạo hiệu ứng 3D lật thẻ
                            // Điểm neo mặc định ở trung tâm thẻ (TransformOrigin(x=0.5f, y=0.5f))
                        }
                        .clickable { isFlipped = !isFlipped },
                    colors = CardDefaults.cardColors(containerColor = if (isFlipped) Color(0xFFFFF3E0) else Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
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
                                        Text("Ghi chú: ${currentCard.note}", fontSize = 14.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                }
                            }
                        }

                        // Lớp phủ màu báo hiệu kết quả thao tác kéo
                        if (isFlipped && overlayAlpha > 0f) {
                            Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.background(overlayColor.copy(alpha = overlayAlpha)))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Các nút điều khiển thủ công và chỉ dẫn
                if (isFlipped) {
                    Text(
                        text = "👈 Vuốt trái (Chưa thuộc)   |   Vuốt phải (Tốt) 👉",
                        fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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

// Xây dựng thanh trạng thái thống kê kết quả
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

// Xây dựng các nút chọn cấp độ ưu tiên ôn tập
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