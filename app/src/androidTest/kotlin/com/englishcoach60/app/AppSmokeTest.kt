package com.englishcoach60.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun homeRendersAndTrainingStarts() {
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodes(hasText("English Coach")).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("English Coach").assertIsDisplayed()
        rule.onNodeWithText("Look up a word").assertIsDisplayed()
        val start = rule.onAllNodes(hasText("Start Training") or hasText("Continue Training"))
        start.onFirst().assertIsDisplayed().performClick()
        val anyTrainingStep = (1..6)
            .map { hasText("Step $it of 6") }
            .reduce { matcher, step -> matcher or step }
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodes(anyTrainingStep).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodes(anyTrainingStep).onFirst().assertIsDisplayed()
        rule.onNodeWithContentDescription("Leave training").assertExists()
    }

    @Test fun difficultyThresholdCanBeRaisedFromSettings() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Today's difficulty").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Swipe").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Raise difficulty").assertExists()
        rule.onNodeWithContentDescription("Lower difficulty").assertExists()
    }
}
