package com.indianservers.ai_stem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PhaseOneComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun welcomeSubjectAndMathematicsFlowIsVisible() {
        composeRule.onNodeWithText("AI STEM").assertIsDisplayed()
        composeRule.onNodeWithText("Start Exploring").performClick()
        composeRule.onNodeWithText("Mathematics").assertIsDisplayed()
        composeRule.onNodeWithText("Physics").assertIsDisplayed()
        composeRule.onNodeWithTag("games-entry-card").assertIsDisplayed()
        composeRule.onAllNodesWithText("Coming in a future phase")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Mathematics").performClick()
        composeRule.onNodeWithText("AR Mathematics Playground").assertIsDisplayed()
        composeRule.onNodeWithText("Open AR Playground").assertIsDisplayed()
    }

    @Test
    fun disabledSubjectShowsFuturePhaseMessage() {
        composeRule.onNodeWithText("Start Exploring").performClick()
        composeRule.onNodeWithText("Physics").performClick()
        composeRule.onNodeWithText("This STEM subject will be added in a future phase.").assertIsDisplayed()
    }

    @Test
    fun gamesEntryOpensLibraryArenaAndBackNavigationWorks() {
        composeRule.onNodeWithText("Start Exploring").performClick()
        composeRule.onNodeWithTag("games-entry-card").performClick()
        composeRule.onNodeWithTag("games-library-screen").assertIsDisplayed()
        composeRule.onNodeWithText("AR Math Arena").assertIsDisplayed()
        composeRule.onNodeWithTag("open-game-ar-math-arena").performClick()
        composeRule.onNodeWithTag("ar-math-arena-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("ar-math-arena-card").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("games-library-screen").assertIsDisplayed()
    }
}
