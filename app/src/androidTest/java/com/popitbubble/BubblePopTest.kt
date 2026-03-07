package com.popitbubble

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BubblePopTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // ── Baseline state ────────────────────────────────────────────────────────

    @Test
    fun counter_starts_at_zero() {
        onView(withId(R.id.tvCounter))
            .check(matches(withText("0 / 25")))
    }

    @Test
    fun best_time_label_empty_initially() {
        onView(withId(R.id.tvBestTime))
            .check(matches(withText("")))
    }

    @Test
    fun challenge_bar_hidden_by_default() {
        onView(withId(R.id.challengeBar))
            .check(matches(not(isDisplayed())))
    }

    // ── Pop behaviour ─────────────────────────────────────────────────────────

    @Test
    fun tapping_bubble_grid_increments_counter() {
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.tvCounter))
            .check(matches(withText("1 / 25")))
    }

    @Test
    fun repeated_taps_at_same_spot_count_only_once() {
        // All three taps land on the same centre bubble [2,2].
        // Only the first tap should register; the bubble is already popped for taps 2 and 3.
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.tvCounter))
            .check(matches(withText("1 / 25")))
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    fun reset_fab_restores_counter_to_zero() {
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.fabReset)).perform(click())
        onView(withId(R.id.tvCounter))
            .check(matches(withText("0 / 25")))
    }

    // ── Challenge Mode ────────────────────────────────────────────────────────

    @Test
    fun challenge_bar_visible_after_toggle() {
        openActionBarOverflowOrOptionsMenu(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        onView(withText("⏱  Challenge Mode")).perform(click())
        onView(withId(R.id.challengeBar)).check(matches(isDisplayed()))
    }

    // ── Theme switching ───────────────────────────────────────────────────────

    @Test
    fun switching_to_neon_theme_does_not_crash() {
        openActionBarOverflowOrOptionsMenu(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        onView(withText("Color Theme")).perform(click())
        onView(withText("⚡ Neon")).perform(click())
        // Grid must still be interactive after the theme change
        onView(withId(R.id.bubbleGridView)).perform(click())
        onView(withId(R.id.tvCounter))
            .check(matches(withText("1 / 25")))
    }
}
