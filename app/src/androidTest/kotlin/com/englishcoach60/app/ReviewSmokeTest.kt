package com.englishcoach60.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.englishcoach60.app.presentation.training.ReviewStep
import com.englishcoach60.app.presentation.training.TrainingUiState
import com.englishcoach60.designsystem.EnglishCoachTheme
import com.englishcoach60.domain.model.DailyReview
import org.junit.Rule
import org.junit.Test

class ReviewSmokeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun reviewRendersRealSpeakingMetric() {
        val review = DailyReview(1, "You spoke in complete sentences.", "Keep answers short and clear.", emptyList(), emptyList(), "Recall today's expressions.")
        rule.setContent { EnglishCoachTheme { ReviewStep(TrainingUiState(loading = false, review = review), onComplete = {}) } }
        rule.onNodeWithText("You spoke under 1 min today.").assertIsDisplayed()
        rule.onNodeWithText("Finish Day").performScrollTo().assertIsDisplayed()
    }
}
