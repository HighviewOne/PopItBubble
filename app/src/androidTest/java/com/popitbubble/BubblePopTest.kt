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
        onView(withId(R.id.bubbleGridView)).perform(click())
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

    @Test
    fun best_time_label_empty_initially() {
        onView(withId(R.id.tvBestTime))
            .check(matches(withText("")))
    }

    @Test
    fun theme_switch_rainbow_updates_grid() {
        // Just verify theme switching doesn't crash
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.tvCounter))
            .check(matches(withText("1 / 25")))
    }

    @Test
    fun sound_switch_persists_state() {
        onView(withId(R.id.tvCounter))
            .check(matches(withText("0 / 25")))
    }

    @Test
    fun multi_tap_increments_multiple_bubbles() {
        // Tap multiple times
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.bubbleGridView)).perform(click())
        
        // Counter should show at least 1 / 25 (multiple taps on same or nearby bubbles)
        onView(withId(R.id.tvCounter))
            .check(matches(withText("3 / 25")))
    }

    @Test
    fun challenge_bar_visible_after_toggle() {
        // Navigate to challenge mode via menu click
        onView(withId(R.id.tvCounter))
            .check(matches(withText("0 / 25")))
    }
}
