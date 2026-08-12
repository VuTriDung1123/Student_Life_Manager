package com.personal.studentlifemanager.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val newCardsPerDay: Int = 20,
    val maxReviewsPerDay: Int = 100
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("deckId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["deckId"])]
)
data class FlashcardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val frontText: String,
    val backText: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "NEW", // "NEW", "LEARNING", "REVIEW"
    val repetitions: Int = 0,
    val interval: Int = 0,
    val easeFactor: Float = 2.5f,
    val nextReviewDate: Long = 0L
)

@Entity(tableName = "flashcard_activity")
data class FlashcardActivityEntity(
    @PrimaryKey val dateStr: String, // format: "yyyy-MM-dd"
    val count: Int
)
