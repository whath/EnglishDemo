package com.englishcoach60.app

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.englishcoach60.app.presentation.training.RecallStep
import com.englishcoach60.app.presentation.training.TrainingUiState
import com.englishcoach60.designsystem.EnglishCoachTheme
import com.englishcoach60.domain.model.Expression
import org.junit.Rule
import org.junit.Test

class RecallSwipeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun revealedCardCanBeSwipedRightForGood() {
        rule.setContent {
            var result by remember { mutableStateOf("") }
            EnglishCoachTheme {
                if (result.isNotBlank()) Text(result)
                else RecallStep(
                    state = TrainingUiState(
                        loading = false,
                        dueExpressions = listOf(Expression("confident", "自信的", "I feel confident today.")),
                        revealRecall = true,
                    ),
                    onReveal = {},
                    onRate = { result = "Rated ${it.name}" },
                    onSkipEmpty = {},
                )
            }
        }

        rule.onNodeWithTag("recall-card").performTouchInput { swipeRight(durationMillis = 500) }
        rule.onNodeWithText("Rated GOOD").assertIsDisplayed()
    }
}
