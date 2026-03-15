package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.personal.studentlifemanager.data.model.Deck
import com.personal.studentlifemanager.data.model.Flashcard
import java.util.Calendar
import kotlin.math.roundToInt

class FlashcardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid ?: ""

    var decks by mutableStateOf<List<Deck>>(emptyList())
        private set

    var cards by mutableStateOf<List<Flashcard>>(emptyList())
        private set

    var dueCards by mutableStateOf<List<Flashcard>>(emptyList())
        private set

    private var currentCardsListener: ListenerRegistration? = null

    init {
        fetchDecks()
    }

    private fun fetchDecks() {
        if (userId.isEmpty()) return
        db.collection("users").document(userId).collection("decks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                decks = snapshot?.documents?.mapNotNull { it.toObject(Deck::class.java) } ?: emptyList()
            }
    }

    fun createDeck(name: String, description: String, onSuccess: () -> Unit) {
        if (userId.isEmpty() || name.isBlank()) return
        val ref = db.collection("users").document(userId).collection("decks").document()
        val newDeck = Deck(id = ref.id, name = name, description = description, createdAt = System.currentTimeMillis())
        ref.set(newDeck).addOnSuccessListener { onSuccess() }
    }

    fun deleteDeck(deckId: String) {
        if (userId.isEmpty()) return
        db.collection("users").document(userId).collection("decks").document(deckId).delete()
    }

    fun fetchCards(deckId: String) {
        currentCardsListener?.remove()
        if (userId.isEmpty() || deckId.isEmpty()) return

        currentCardsListener = db.collection("users").document(userId).collection("flashcards")
            .whereEqualTo("deckId", deckId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val fetchedCards = snapshot?.documents?.mapNotNull { it.toObject(Flashcard::class.java) } ?: emptyList()
                cards = fetchedCards.sortedByDescending { it.createdAt }
            }
    }

    fun addCard(deckId: String, front: String, back: String, note: String, onSuccess: () -> Unit) {
        if (userId.isEmpty() || front.isBlank() || back.isBlank()) return
        val ref = db.collection("users").document(userId).collection("flashcards").document()
        val card = Flashcard(
            id = ref.id, deckId = deckId,
            frontText = front, backText = back, note = note,
            createdAt = System.currentTimeMillis()
        )
        ref.set(card).addOnSuccessListener { onSuccess() }
    }

    fun updateCard(card: Flashcard, onSuccess: () -> Unit) {
        if (userId.isEmpty()) return
        db.collection("users").document(userId).collection("flashcards").document(card.id)
            .set(card).addOnSuccessListener { onSuccess() }
    }

    fun deleteCard(cardId: String) {
        if (userId.isEmpty()) return
        db.collection("users").document(userId).collection("flashcards").document(cardId).delete()
    }

    // ==========================================
    // 🔥 KHU VỰC HỌC & SPACED REPETITION (SM-2)
    // ==========================================

    fun fetchDueCards(deckId: String) {
        if (userId.isEmpty() || deckId.isEmpty()) return

        // Đặt mốc thời gian là 23:59:59 đêm nay
        val todayMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        db.collection("users").document(userId).collection("flashcards")
            .whereEqualTo("deckId", deckId) // Lấy hết thẻ của bộ này về
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedCards = snapshot?.documents?.mapNotNull { it.toObject(Flashcard::class.java) } ?: emptyList()

                // 🔥 THUẬT TOÁN LỌC LOCAL (Lách luật Index của Firebase)
                // Lọc ra các thẻ có ngày ôn tập <= hôm nay (Thẻ mới tạo có nextReviewDate = 0 nên sẽ luôn lọt vào đây)
                val filteredDueCards = fetchedCards.filter { it.nextReviewDate <= todayMillis }

                // Sắp xếp: Thẻ "LEARNING" học trước, rồi đến "NEW", cuối cùng là "REVIEW". Ưu tiên thẻ cũ trước.
                dueCards = filteredDueCards
                    .sortedWith(compareBy<Flashcard> { it.status }.thenBy { it.nextReviewDate })
                    .take(100) // Tối đa 100 thẻ mỗi lần học cho đỡ ngộp
            }
            .addOnFailureListener {
                it.printStackTrace() // Bắt lỗi nếu Firebase dở chứng
            }
    }

    // 🔥 ĐÃ SỬA: Dùng Chuỗi "LEARNING" và "REVIEW" thay cho Enum
    fun rateCard(card: Flashcard, rating: Int) {
        if (userId.isEmpty()) return

        var newEaseFactor = card.easeFactor + (0.1f - (5 - rating) * (0.08f + (5 - rating) * 0.02f))
        if (newEaseFactor < 1.3f) newEaseFactor = 1.3f

        var newInterval = 0
        var newRepetitions = 0

        when (rating) {
            1 -> {
                newInterval = 0
                newRepetitions = 0
                card.status = "LEARNING" // 🔥 Chữ thay vì Enum
            }
            2, 3, 4 -> {
                newRepetitions = card.repetitions + 1

                newInterval = when (newRepetitions) {
                    1 -> 1
                    2 -> 6
                    else -> (card.interval * newEaseFactor).roundToInt()
                }
                card.status = "REVIEW" // 🔥 Chữ thay vì Enum
            }
        }

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, newInterval)
        val nextReviewMillis = cal.timeInMillis

        val updatedCard = card.copy(
            easeFactor = newEaseFactor,
            interval = newInterval,
            repetitions = newRepetitions,
            nextReviewDate = nextReviewMillis,
            status = card.status
        )

        db.collection("users").document(userId).collection("flashcards").document(card.id).set(updatedCard)
    }
}