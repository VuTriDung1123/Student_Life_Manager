package com.personal.studentlifemanager.data.model

// 1. LỚP DECK (BỘ THẺ)
data class Deck(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var createdAt: Long = System.currentTimeMillis(),

    // Cài đặt mục tiêu học mỗi ngày (Cho thuật toán sau này)
    var newCardsPerDay: Int = 20,
    var maxReviewsPerDay: Int = 100
)

// 2. LỚP FLASHCARD (THẺ NHỚ) & THUẬT TOÁN SM-2
data class Flashcard(
    var id: String = "",
    var deckId: String = "",

    var frontText: String = "",
    var backText: String = "",
    var note: String = "",
    var createdAt: Long = System.currentTimeMillis(),

    // 🔥 TRÁI TIM CỦA SPACED REPETITION (SM-2)
    var status: String = "NEW",       // "NEW", "LEARNING", "REVIEW"
    var repetitions: Int = 0,         // Số lần trả lời đúng liên tiếp
    var interval: Int = 0,            // Khoảng cách ngày ôn tập tiếp theo
    var easeFactor: Float = 2.5f,     // Độ khó của thẻ (Mặc định 2.5)
    var nextReviewDate: Long = 0L     // Thời điểm cần lật lại thẻ này
)