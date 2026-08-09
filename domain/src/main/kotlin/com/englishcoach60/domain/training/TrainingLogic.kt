package com.englishcoach60.domain.training

import com.englishcoach60.domain.model.*
import kotlin.math.max
import kotlin.math.min

object TrainingPlan {
    private val topics = listOf(
        "Introduce Yourself and Your Academic Background", "Campus, Hometown and City Life", "Study and Work Routines", "Scheduling and Priorities", "Preferences and Reasons",
        "Relationships and Collaboration", "Food, Health and Culture", "Consumer Choices and Comparisons", "Explaining Places and Directions", "University Foundation Review",
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
        in 1..10 -> "University Foundation"
        in 11..20 -> "Practical Communication"
        in 21..30 -> "Social and Academic English"
        in 31..40 -> "Android Work English I"
        in 41..50 -> "Android Work English II"
        else -> "Independent Conversation"
    }
    fun listeningWordRange(day: Int) = when (day) {
        in 1..10 -> 110..140
        in 11..20 -> 125..160
        in 21..30 -> 140..180
        in 31..40 -> 155..195
        in 41..50 -> 170..215
        else -> 185..235
    }
    fun listeningWordRange(day: Int, difficulty: Int): IntRange {
        val base = listeningWordRange(day)
        val offset = when (difficulty.coerceIn(1, 4)) {
            1 -> 0
            2 -> 20
            3 -> 40
            else -> 60
        }
        return (base.first + offset)..(base.last + offset)
    }
    fun speakingTargetMinutes(day: Int) = when (day) { in 1..10 -> 8; in 11..30 -> 12; else -> 15 }
    fun ttsRate(day: Int) = when (day) {
        in 1..10 -> .95f
        in 11..20 -> 1f
        in 21..30 -> 1.03f
        in 31..40 -> 1.06f
        in 41..50 -> 1.08f
        else -> 1.1f
    }
    fun ttsRate(day: Int, difficulty: Int): Float {
        val adjustment = when (difficulty.coerceIn(1, 4)) {
            1 -> 0f
            2 -> .05f
            3 -> .1f
            else -> .15f
        }
        return (ttsRate(day) + adjustment).coerceIn(.95f, 1.2f)
    }

    fun repeatSentences(listeningText: String): List<String> = listeningText
        .split(Regex("(?<=[.!?])\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
}

data class DifficultyProfile(
    val level: Int,
    val name: String,
    val summary: String,
    val promptGuidance: String,
)

object DifficultyProfiles {
    private val profiles = listOf(
        DifficultyProfile(1, "University Foundation", "B1 core English with complete explanations and practical vocabulary", "Use CEFR B1 university foundation English. Require complete sentences, reasons, comparisons, and reusable academic or everyday vocabulary. Keep support clear without simplifying below B1."),
        DifficultyProfile(2, "University Plus", "B1+ extended responses with denser listening and less scaffolding", "Use CEFR B1+ English. Include connected ideas, paraphrasing, common collocations, and follow-up questions that require explanation rather than one-line answers."),
        DifficultyProfile(3, "Advanced Communication", "B2 natural pace, nuanced opinions, and workplace-ready language", "Use CEFR B2 English at a natural pace. Include nuanced opinions, inference, idiomatic but broadly useful phrasing, and realistic academic or workplace interaction with limited hints."),
        DifficultyProfile(4, "Professional Challenge", "B2+/C1 precision, complex scenarios, and demanding follow-ups", "Use upper-B2 to C1 English. Require precise argument, synthesis, tactful disagreement, richer collocations, and complex professional or social follow-ups without learner scaffolding."),
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
