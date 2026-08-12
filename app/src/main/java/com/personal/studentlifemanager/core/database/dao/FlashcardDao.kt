package com.personal.studentlifemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.studentlifemanager.core.database.entities.DeckEntity
import com.personal.studentlifemanager.core.database.entities.FlashcardActivityEntity
import com.personal.studentlifemanager.core.database.entities.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM decks")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    fun getCardsByDeckId(deckId: String): Flow<List<FlashcardEntity>>
    
    @Query("SELECT * FROM flashcards")
    fun getAllCards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :currentTime")
    fun getDueCards(currentTime: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Delete
    suspend fun deleteCard(card: FlashcardEntity)

    @Query("SELECT * FROM flashcard_activity")
    fun getAllActivities(): Flow<List<FlashcardActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: FlashcardActivityEntity)
    
    @Query("SELECT * FROM flashcard_activity WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getActivityByDate(dateStr: String): FlashcardActivityEntity?
}
