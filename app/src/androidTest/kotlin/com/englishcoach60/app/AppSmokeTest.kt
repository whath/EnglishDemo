package com.englishcoach60.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun homeRendersAndTrainingStarts() {
        rule.onNodeWithText("English Coach").assertIsDisplayed()
        rule.onNodeWithText("Look up a word").assertIsDisplayed()
        val start = rule.onAllNodes(hasText("Start Training") or hasText("Continue Training"))
        start.onFirst().assertIsDisplayed().performClick()
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodesWithText("Step 1 of 6").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Step 1 of 6").assertIsDisplayed()
        rule.onNodeWithText("Recall").assertIsDisplayed()
    }

    @Test fun difficultyThresholdCanBeRaisedFromSettings() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Today's difficulty").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Swipe").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Raise difficulty").assertExists()
        rule.onNodeWithContentDescription("Lower difficulty").assertExists()
    }
}
