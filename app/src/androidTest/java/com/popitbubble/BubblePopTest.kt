package com.popitbubble

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso UI test: verifies that tapping the bubble grid increments the pop counter.
 *
 * Runs on a real device or emulator (Large test tag — skipped in unit test phase).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BubblePopTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun counter_starts_at_zero() {
        onView(withId(R.id.tvCounter))
            .check(matches(withText("0 / 25")))
    }

    @Test
    fun tapping_bubble_grid_increments_counter() {
        // The default 5×5 grid fills the view; clicking the centre hits bubble [2,2].
        onView(withId(R.id.bubbleGridView)).perform(click())

        // Counter should now show 1 / 25
        onView(withId(R.id.tvCounter))
            .check(matches(withText("1 / 25")))
    }

    @Test
    fun reset_fab_restores_counter_to_zero() {
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.fabReset)).perform(click())

        onView(withId(R.id.tvCounter))
            .check(matches(withText("0 / 25")))
    }

    @Test
    fun challenge_bar_hidden_by_default() {
        onView(withId(R.id.challengeBar))
            .check(matches(not(isDisplayed())))
    }
}
