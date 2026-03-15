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

class FlashcardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid ?: ""

    var decks by mutableStateOf<List<Deck>>(emptyList())
        private set

    // 🔥 DANH SÁCH THẺ CỦA DECK ĐANG CHỌN
    var cards by mutableStateOf<List<Flashcard>>(emptyList())
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
        // Ghi chú: Đáng lẽ phải xóa luôn Flashcard bên trong, ta sẽ viết Cloud Function hoặc xóa bằng vòng lặp sau.
    }

    // ==========================================
    // 🔥 KHU VỰC QUẢN LÝ FLASHCARD BÊN TRONG DECK
    // ==========================================

    fun fetchCards(deckId: String) {
        currentCardsListener?.remove() // Gỡ listener cũ nếu đổi Deck
        if (userId.isEmpty() || deckId.isEmpty()) return

        currentCardsListener = db.collection("users").document(userId).collection("flashcards")
            .whereEqualTo("deckId", deckId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                // Ép kiểu List và sắp xếp thủ công bằng Kotlin để tránh lỗi Index của Firestore
                val fetchedCards: List<Flashcard> = snapshot?.documents?.mapNotNull { it.toObject(Flashcard::class.java) } ?: emptyList()
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
}