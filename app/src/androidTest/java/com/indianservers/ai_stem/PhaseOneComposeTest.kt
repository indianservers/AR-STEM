package com.indianservers.ai_stem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
        composeRule.onNodeWithText("Math Fortress AR").assertIsDisplayed()
        composeRule.onNodeWithText("Equation Escape AR").assertIsDisplayed()
        composeRule.onNodeWithText("Geometry Architect AR").assertIsDisplayed()
        composeRule.onNodeWithText("Fraction Factory AR").assertIsDisplayed()
        composeRule.onNodeWithText("Coordinate Conquest AR").assertIsDisplayed()
        composeRule.onNodeWithText("Math Expedition AR").assertIsDisplayed()
        composeRule.onNodeWithTag("open-game-ar-math-arena").performClick()
        composeRule.onNodeWithTag("ar-math-arena-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("ar-math-arena-card").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("games-library-screen").assertIsDisplayed()
    }

    @Test
    fun gamesDetailsAndHowToPlayRoutesWorkForAvailableAndComingSoonGames() {
        composeRule.onNodeWithText("Start Exploring").performClick()
        composeRule.onNodeWithTag("games-entry-card").performClick()
        composeRule.onNodeWithTag("howto-game-ar-math-arena").performClick()
        composeRule.onNodeWithTag("game-howto-ar-math-arena").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("view-game-equation_escape_ar").performClick()
        composeRule.onNodeWithTag("game-details-equation_escape_ar").assertIsDisplayed()
        composeRule.onNodeWithText("Available").assertIsDisplayed()
        composeRule.onNodeWithTag("details-howto-equation_escape_ar").performClick()
        composeRule.onNodeWithTag("game-howto-equation_escape_ar").assertIsDisplayed()
        composeRule.onNodeWithTag("howto-play-equation_escape_ar").assertIsDisplayed()
        composeRule.onNodeWithTag("howto-details-equation_escape_ar").performClick()
        composeRule.onNodeWithTag("game-details-equation_escape_ar").assertIsDisplayed()
        composeRule.onNodeWithTag("details-play-equation_escape_ar").performClick()
        composeRule.onNodeWithText("Equation Escape AR").assertIsDisplayed()
        composeRule.onNodeWithTag("equation_escape_active_lock").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("game-details-equation_escape_ar").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("view-game-math_expedition_ar").performClick()
        composeRule.onNodeWithTag("game-details-math_expedition_ar").assertIsDisplayed()
        composeRule.onNodeWithTag("details-play-math_expedition_ar").performClick()
        composeRule.onNodeWithTag("math_expedition_route").assertIsDisplayed()
    }

    @Test
    fun mathFortressLandingHowToUsesReusableHowToPlayPage() {
        composeRule.onNodeWithText("Start Exploring").performClick()
        composeRule.onNodeWithTag("games-entry-card").performClick()
        composeRule.onNodeWithTag("open-game-ar-math-arena").performClick()
        composeRule.onNodeWithText("How to Play").performClick()
        composeRule.onNodeWithTag("game-howto-ar-math-arena").assertIsDisplayed()
        composeRule.onNodeWithText("Estimated reading time: 4 min").assertIsDisplayed()
        composeRule.onNodeWithTag("howto-play-ar-math-arena").assertIsDisplayed()
    }
}
