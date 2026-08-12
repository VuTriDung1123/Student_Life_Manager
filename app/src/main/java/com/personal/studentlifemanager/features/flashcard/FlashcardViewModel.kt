package com.personal.studentlifemanager.features.flashcard

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.studentlifemanager.core.database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FlashcardRepository(AppDatabase.getDatabase(application).flashcardDao())

    var decks by mutableStateOf<List<Deck>>(emptyList())
        private set

    var cards by mutableStateOf<List<Flashcard>>(emptyList())
        private set

    var dueCards by mutableStateOf<List<Flashcard>>(emptyList())
        private set

    var sessionStats = mutableStateMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0)
        private set

    init {
        fetchDecks()
    }

    private fun fetchDecks() {
        viewModelScope.launch {
            repository.getDecks().collectLatest { fetchedDecks ->
                decks = fetchedDecks.sortedByDescending { it.createdAt }
            }
        }
    }

    fun createDeck(name: String, description: String, onSuccess: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addDeck(name, description)
            onSuccess()
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
        }
    }

    fun fetchCards(deckId: String) {
        if (deckId.isEmpty()) return
        viewModelScope.launch {
            repository.getCards(deckId).collectLatest { fetchedCards ->
                cards = fetchedCards.sortedByDescending { it.createdAt }
            }
        }
    }

    fun addCard(deckId: String, front: String, back: String, note: String, onSuccess: () -> Unit) {
        if (front.isBlank() || back.isBlank()) return
        viewModelScope.launch {
            repository.addCard(deckId, front, back, note)
            onSuccess()
        }
    }

    fun updateCard(card: Flashcard, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateCard(card)
            onSuccess()
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    // ==========================================
    // 🔥 KHU VỰC HỌC & SPACED REPETITION (SM-2)
    // ==========================================

    fun fetchDueCards(deckId: String) {
        if (deckId.isEmpty()) return

        sessionStats[1] = 0; sessionStats[2] = 0; sessionStats[3] = 0; sessionStats[4] = 0

        viewModelScope.launch {
            // For now, load all cards of the deck and filter them manually to match the original logic
            repository.getCards(deckId).collectLatest { fetchedCards ->
                val now = System.currentTimeMillis()
                
                // Original app logic was to fetch all cards and take 200, sorting by LEARNING > NEW > REVIEW
                // We'll filter the actual due ones (nextReviewDate <= now) for REVIEW status.
                val trulyDue = fetchedCards.filter {
                    it.status == "NEW" || it.status == "LEARNING" || (it.status == "REVIEW" && it.nextReviewDate <= now)
                }

                dueCards = trulyDue
                    .sortedWith(compareBy<Flashcard> {
                        when(it.status) {
                            "LEARNING" -> 0
                            "NEW" -> 1
                            "REVIEW" -> 2
                            else -> 3
                        }
                    }.thenBy { it.nextReviewDate })
                    .take(200)
            }
        }
    }

    fun rateCard(card: Flashcard, rating: Int) {
        sessionStats[rating] = (sessionStats[rating] ?: 0) + 1

        var newEaseFactor = card.easeFactor + (0.1f - (5 - rating) * (0.08f + (5 - rating) * 0.02f))
        if (newEaseFactor < 1.3f) newEaseFactor = 1.3f

        var newInterval = 0
        var newRepetitions = 0

        when (rating) {
            1 -> {
                newInterval = 0
                newRepetitions = 0
                card.status = "LEARNING"
            }
            2, 3, 4 -> {
                newRepetitions = card.repetitions + 1

                newInterval = when (newRepetitions) {
                    1 -> 1
                    2 -> 6
                    else -> (card.interval * newEaseFactor).roundToInt()
                }
                card.status = "REVIEW"
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

        viewModelScope.launch {
            repository.updateCard(updatedCard)
            repository.logDailyActivity() // Update analytics locally
        }
    }

    // ==========================================
    // 🔥 KHU VỰC KIỂM TRA TRẮC NGHIỆM (QUIZ MODE)
    // ==========================================

    data class QuizQuestion(
        val questionText: String,
        val correctAnswer: String,
        val options: List<String>
    )

    var quizQuestions by mutableStateOf<List<QuizQuestion>>(emptyList())
        private set
    var quizCorrectCount by mutableIntStateOf(0)
        private set
    var quizWrongCount by mutableIntStateOf(0)
        private set

    fun generateQuiz() {
        if (cards.size < 4) return

        val questions = mutableListOf<QuizQuestion>()

        cards.shuffled().forEach { card ->
            val isFrontQuestion = Math.random() > 0.5
            val questionText = if (isFrontQuestion) card.frontText else card.backText
            val correctAnswer = if (isFrontQuestion) card.backText else card.frontText

            var wrongAnswers = cards.filter { it.id != card.id }
                .map { if (isFrontQuestion) it.backText else it.frontText }
                .distinct()
                .shuffled()
                .take(3)

            var padIndex = 1
            while (wrongAnswers.size < 3) {
                val padStr = "Đáp án khác $padIndex"
                if (!wrongAnswers.contains(padStr) && padStr != correctAnswer) {
                    wrongAnswers = wrongAnswers + padStr
                }
                padIndex++
            }

            val finalOptions = (wrongAnswers + correctAnswer).shuffled()
            questions.add(QuizQuestion(questionText, correctAnswer, finalOptions))
        }

        quizQuestions = questions
        quizCorrectCount = 0
        quizWrongCount = 0
    }

    fun recordQuizAnswer(isCorrect: Boolean) {
        if (isCorrect) quizCorrectCount++ else quizWrongCount++
    }

    // ==========================================
    // KHU VỰC THỐNG KÊ VÀ PHÂN TÍCH DỮ LIỆU (ANALYTICS)
    // ==========================================

    var totalCardsCount by mutableIntStateOf(0)
        private set
    var retainedCardsCount by mutableIntStateOf(0)
        private set
    var currentStreak by mutableIntStateOf(0)
        private set
    var dailyActivityMap by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    fun fetchAnalyticsData() {
        viewModelScope.launch {
            launch {
                repository.getAllCards().collectLatest { allCards ->
                    totalCardsCount = allCards.size
                    retainedCardsCount = allCards.count { it.status == "REVIEW" }
                }
            }

            launch {
                repository.getAllActivities().collectLatest { activities ->
                    val activityMap = mutableMapOf<String, Int>()
                    activities.forEach { act ->
                        activityMap[act.dateStr] = act.count
                    }
                    dailyActivityMap = activityMap
                    calculateStreak(activityMap)
                }
            }
        }
    }

    private fun calculateStreak(activityMap: Map<String, Int>) {
        var streak = 0
        val calendar = java.util.Calendar.getInstance()
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        val todayStr = format.format(calendar.time)
        val studiedToday = (activityMap[todayStr] ?: 0) > 0

        if (studiedToday) {
            streak = 1
        }

        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)

        while (true) {
            val dateStr = format.format(calendar.time)
            val count = activityMap[dateStr] ?: 0
            if (count > 0) {
                streak++
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        currentStreak = streak
    }
}
