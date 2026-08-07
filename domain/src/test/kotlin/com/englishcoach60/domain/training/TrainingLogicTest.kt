package com.englishcoach60.domain.training

import com.englishcoach60.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingLogicTest {
    @Test fun srsAgainHardGoodIntervals() {
        assertEquals(1, ReviewScheduler.schedule(8, 3, ReviewRating.AGAIN).intervalDays)
        assertEquals(8, ReviewScheduler.schedule(8, 3, ReviewRating.HARD).intervalDays)
        assertEquals(16, ReviewScheduler.schedule(8, 3, ReviewRating.GOOD).intervalDays)
        assertEquals(3, ReviewScheduler.schedule(0, 0, ReviewRating.GOOD).intervalDays)
    }

    @Test fun difficultyMovesOnlyWithinBounds() {
        val strong = TrainingMetrics(3, 3, userTurnCount = 5, importantCorrections = 1)
        assertEquals(2, DifficultyPolicy.next(1, listOf(strong, strong, strong)))
        val poor = TrainingMetrics(1, 3, userTurnCount = 4, importantCorrections = 3)
        assertEquals(1, DifficultyPolicy.next(2, listOf(poor, poor)))
    }

    @Test fun difficultyChangesListeningLoadAndSpeechRateWithinBounds() {
        assertEquals(60..80, TrainingPlan.listeningWordRange(1, 1))
        assertEquals(90..110, TrainingPlan.listeningWordRange(1, 4))
        assertEquals(.78f, TrainingPlan.ttsRate(1, 1), .001f)
        assertEquals(.95f, TrainingPlan.ttsRate(1, 4), .001f)
        assertEquals("Challenge", DifficultyProfiles.get(99).name)
    }

    @Test fun metricsUseRealEvidence() {
        val turns = listOf(ConversationTurn(day=1, turnIndex=1, role="user", text="I work as a developer"))
        val result = TrainingMetricsCalculator.calculate(2, 3, 8_000, turns, listOf(3000, 1000, 2000), 1, "I introduced myself today")
        assertEquals(2.0 / 3.0, result.listeningAccuracy, .001)
        assertEquals(5.0, result.averageWordsPerTurn, .001)
        assertEquals(2000, result.responseDelayMedianMs)
    }

    @Test fun onlyCompletedDayAdvances() {
        assertEquals(12, SessionProgressPolicy.dayAfterReview(12, TrainingStatus.IN_PROGRESS))
        assertEquals(13, SessionProgressPolicy.dayAfterReview(12, TrainingStatus.COMPLETED))
        assertEquals(60, SessionProgressPolicy.dayAfterReview(60, TrainingStatus.COMPLETED))
    }
}
