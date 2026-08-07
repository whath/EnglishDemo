package com.englishcoach60.domain.training

import com.englishcoach60.domain.model.*
import kotlin.math.max
import kotlin.math.min

object TrainingPlan {
    private val topics = listOf(
        "Introduce Yourself", "Name, Hometown and Basic Information", "My Daily Routine", "Time and Schedule", "Likes and Dislikes",
        "Family and Friends", "Food and Drinks", "Shopping and Prices", "Places and Asking Directions", "Foundation Review",
        "Restaurant", "Coffee Shop", "Taxi / Ride", "Hotel Check-in", "Airport Check-in", "Security and Boarding", "Travel Problems",
        "Ask for Help", "Doctor / Pharmacy Basic Communication", "Travel Simulation", "Small Talk", "Weekend", "Hobbies", "Invitation",
        "Phone / Video Call", "Future Plans", "Past Experiences", "Explain a Problem", "Give an Opinion", "Social Conversation Review",
        "Introduce My Job", "My Responsibilities", "Introduce an Android Project", "Daily Stand-up", "Ask for Clarification", "Explain a Bug",
        "Discuss UI / UX", "API / Network Problem", "Progress and Deadline", "Work Conversation Review", "Job Interview", "Explain My Tech Stack",
        "Architecture Decision", "Code Review", "Remote Work", "Politely Disagree", "Present an Idea", "Discuss Priorities", "Production Problem",
        "Developer Simulation", "Recent News", "Artificial Intelligence", "Apps and Technology", "Travel Plans", "Money and Everyday Finance",
        "Future Goals", "Free Conversation", "Multi-scene Role Play", "Travel + Work Simulation", "Final Assessment",
    )

    fun topic(day: Int) = topics[(day.coerceIn(1, 60) - 1)]
    fun phase(day: Int) = when (day) {
        in 1..10 -> "Foundation"
        in 11..20 -> "Survival English"
        in 21..30 -> "Social English"
        in 31..40 -> "Android Work English I"
        in 41..50 -> "Android Work English II"
        else -> "Independent Conversation"
    }
    fun listeningWordRange(day: Int) = when (day) {
        in 1..10 -> 70..90
        in 11..20 -> 80..110
        in 21..30 -> 90..120
        in 31..40 -> 100..130
        in 41..50 -> 110..145
        else -> 120..160
    }
    fun listeningWordRange(day: Int, difficulty: Int): IntRange {
        val base = listeningWordRange(day)
        val offset = when (difficulty.coerceIn(1, 4)) { 1 -> -10; 3 -> 10; 4 -> 20; else -> 0 }
        return (base.first + offset).coerceAtLeast(55)..(base.last + offset)
    }
    fun speakingTargetMinutes(day: Int) = when (day) { in 1..10 -> 8; in 11..30 -> 12; else -> 15 }
    fun ttsRate(day: Int) = when (day) { in 1..10 -> .85f; in 11..20 -> .9f; in 21..30 -> .95f; else -> 1f }
    fun ttsRate(day: Int, difficulty: Int): Float {
        val adjustment = when (difficulty.coerceIn(1, 4)) { 1 -> -.07f; 3 -> .05f; 4 -> .1f; else -> 0f }
        return (ttsRate(day) + adjustment).coerceIn(.78f, 1.1f)
    }
}

data class DifficultyProfile(
    val level: Int,
    val name: String,
    val summary: String,
    val promptGuidance: String,
)

object DifficultyProfiles {
    private val profiles = listOf(
        DifficultyProfile(1, "Gentle", "Slower audio, shorter phrases, more support", "Use very short common sentences, slower pacing, explicit context, and generous scaffolding."),
        DifficultyProfile(2, "Balanced", "Everyday pace with clear, reusable language", "Use common spoken English at a clear everyday pace with moderate scaffolding."),
        DifficultyProfile(3, "Stretch", "Longer replies, less scaffolding, faster audio", "Use slightly longer natural turns, less scaffolding, and occasional unfamiliar but practical phrasing."),
        DifficultyProfile(4, "Challenge", "Natural pace, richer language, sharper follow-ups", "Use natural conversational pace, richer phrasing, fewer hints, and more demanding follow-up questions."),
    )

    fun get(level: Int): DifficultyProfile = profiles[level.coerceIn(1, 4) - 1]
    fun all(): List<DifficultyProfile> = profiles
}

data class ReviewSchedule(val intervalDays: Int, val mastery: Int)

object ReviewScheduler {
    fun schedule(previousInterval: Int, mastery: Int, rating: ReviewRating): ReviewSchedule = when (rating) {
        ReviewRating.AGAIN -> ReviewSchedule(1, max(0, mastery - 1))
        ReviewRating.HARD -> ReviewSchedule(max(2, previousInterval), mastery)
        ReviewRating.GOOD -> ReviewSchedule(if (previousInterval <= 0) 3 else min(previousInterval * 2, 30), min(5, mastery + 1))
    }
}

object DifficultyPolicy {
    fun next(current: Int, recent: List<TrainingMetrics>): Int {
        if (recent.isEmpty()) return current.coerceIn(1, 4)
        val accuracy = recent.map { it.listeningAccuracy }.average()
        val corrections = recent.map { it.importantCorrectionRate }.average()
        if (recent.size >= 3 && accuracy >= .75 && corrections <= .30 && recent.all { it.userTurnCount > 0 }) return min(4, current + 1)
        if (recent.size >= 2 && (accuracy < .50 || corrections > .50)) return max(1, current - 1)
        return current.coerceIn(1, 4)
    }
}

object SessionProgressPolicy {
    fun dayAfterReview(currentDay: Int, status: TrainingStatus): Int =
        if (status == TrainingStatus.COMPLETED) min(60, currentDay + 1) else currentDay
}

object TrainingMetricsCalculator {
    fun calculate(
        listeningCorrect: Int,
        listeningTotal: Int,
        speakingMillis: Long,
        userTurns: List<ConversationTurn>,
        responseDelays: List<Long>,
        targetExpressionsUsed: Int,
        retellingTranscript: String,
    ): TrainingMetrics {
        val wordCounts = userTurns.map { it.text.trim().split(Regex("\\s+")).count { word -> word.isNotBlank() } }
        val sorted = responseDelays.sorted()
        val median = if (sorted.isEmpty()) 0 else sorted[sorted.size / 2]
        return TrainingMetrics(
            listeningCorrect = listeningCorrect,
            listeningTotal = listeningTotal,
            speakingMillis = speakingMillis,
            responseDelayMedianMs = median,
            averageWordsPerTurn = wordCounts.averageOrZero(),
            importantCorrections = userTurns.count { it.correction?.type == CorrectionType.IMPORTANT },
            userTurnCount = userTurns.size,
            targetExpressionsUsed = targetExpressionsUsed,
            retellingWordCount = retellingTranscript.trim().split(Regex("\\s+")).count { it.isNotBlank() },
        )
    }

    private fun List<Int>.averageOrZero() = if (isEmpty()) 0.0 else average()
}
