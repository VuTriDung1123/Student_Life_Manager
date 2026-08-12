package com.personal.studentlifemanager.features.flashcard

import com.personal.studentlifemanager.core.database.dao.FlashcardDao
import com.personal.studentlifemanager.core.database.entities.DeckEntity
import com.personal.studentlifemanager.core.database.entities.FlashcardActivityEntity
import com.personal.studentlifemanager.core.database.entities.FlashcardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FlashcardRepository(private val flashcardDao: FlashcardDao) {

    fun getDecks(): Flow<List<Deck>> {
        return flashcardDao.getAllDecks().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addDeck(name: String, description: String) {
        val entity = DeckEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        flashcardDao.insertDeck(entity)
    }

    suspend fun deleteDeck(deckId: String) {
        flashcardDao.deleteDeck(DeckEntity(id = deckId, name = "", description = ""))
    }

    fun getCards(deckId: String): Flow<List<Flashcard>> {
        return flashcardDao.getCardsByDeckId(deckId).map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    fun getAllCards(): Flow<List<Flashcard>> {
        return flashcardDao.getAllCards().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addCard(deckId: String, front: String, back: String, note: String) {
        val entity = FlashcardEntity(
            id = UUID.randomUUID().toString(),
            deckId = deckId,
            frontText = front,
            backText = back,
            note = note,
            createdAt = System.currentTimeMillis()
        )
        flashcardDao.insertCard(entity)
    }

    suspend fun updateCard(card: Flashcard) {
        flashcardDao.updateCard(card.toEntity())
    }

    suspend fun deleteCard(cardId: String) {
        flashcardDao.deleteCard(FlashcardEntity(id = cardId, deckId = "", frontText = "", backText = "", note = ""))
    }
    
    // --- KHU VỰC HỌC ---
    fun getDueCards(currentTime: Long): Flow<List<Flashcard>> {
        // Trả về thẻ cần học dựa trên thời gian
        return flashcardDao.getDueCards(currentTime).map { entities ->
            entities.map { it.toModel() }
        }
    }

    // --- KHU VỰC HOẠT ĐỘNG (ANALYTICS) ---
    suspend fun logDailyActivity() {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val existing = flashcardDao.getActivityByDate(dateStr)
        if (existing != null) {
            flashcardDao.insertActivity(existing.copy(count = existing.count + 1))
        } else {
            flashcardDao.insertActivity(FlashcardActivityEntity(dateStr = dateStr, count = 1))
        }
    }

    fun getAllActivities(): Flow<List<FlashcardActivityEntity>> {
        return flashcardDao.getAllActivities()
    }

    // --- MAPPERS ---
    private fun DeckEntity.toModel() = Deck(id, name, description, createdAt, newCardsPerDay, maxReviewsPerDay)
    private fun Deck.toEntity() = DeckEntity(id, name, description, createdAt, newCardsPerDay, maxReviewsPerDay)

    private fun FlashcardEntity.toModel() = Flashcard(id, deckId, frontText, backText, note, createdAt, status, repetitions, interval, easeFactor, nextReviewDate)
    private fun Flashcard.toEntity() = FlashcardEntity(id, deckId, frontText, backText, note, createdAt, status, repetitions, interval, easeFactor, nextReviewDate)
}
