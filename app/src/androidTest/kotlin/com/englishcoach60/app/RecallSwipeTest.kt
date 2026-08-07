package com.englishcoach60.app

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
