package com.popitbubble

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BubblePopTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        // Start every test from defaults (5×5 grid, no best time), whatever
        // earlier tests or manual runs saved. Clear before launching so
        // Prefs reads through to SharedPreferences, so the app sees the defaults.
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("popitbubble_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

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

    @Test
    fun challenge_menu_item_shows_checked_state() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        openActionBarOverflowOrOptionsMenu(context)
        onView(withText("⏱  Challenge Mode")).perform(click())
        scenario.onActivity { activity ->
            assertTrue(activity.isChallengeMenuCheckedForTest())
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Test
    fun sprites_are_not_clipped() {
        scenario.onActivity { activity ->
            val sprites = activity.findViewById<BubbleGridView>(R.id.bubbleGridView).spritesForTest()
            assertTrue("no sprites built", sprites.isNotEmpty())
            for (bmp in sprites) {
                val w = bmp.width
                val h = bmp.height
                // The bubble body fills the centre...
                assertEquals(255, Color.alpha(bmp.getPixel(w / 2, h / 2)))
                // ...and the shadow fades out before the sprite's edge.
                for (i in 0 until w) {
                    assertTrue(Color.alpha(bmp.getPixel(i, 0)) <= 1)
                    assertTrue(Color.alpha(bmp.getPixel(i, h - 1)) <= 1)
                }
                for (j in 0 until h) {
                    assertTrue(Color.alpha(bmp.getPixel(0, j)) <= 1)
                    assertTrue(Color.alpha(bmp.getPixel(w - 1, j)) <= 1)
                }
            }
        }
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
