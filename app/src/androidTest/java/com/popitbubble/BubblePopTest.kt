package com.popitbubble

import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
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
import org.hamcrest.Matchers.startsWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun fast_swipe_pops_every_bubble_along_the_path() {
        // One move event jumps from the first to the last bubble of row 3: the
        // three bubbles in between have no touch sample on them, but the finger crossed them.
        scenario.onActivity { activity ->
            val grid = activity.findViewById<BubbleGridView>(R.id.bubbleGridView)
            val from = grid.bubbleCenterForTest(10)  // row 3, column 1
            val to = grid.bubbleCenterForTest(14)    // row 3, column 5
            val t = SystemClock.uptimeMillis()
            for ((action, p) in listOf(
                MotionEvent.ACTION_DOWN to from, MotionEvent.ACTION_MOVE to to, MotionEvent.ACTION_UP to to
            )) {
                val e = MotionEvent.obtain(t, t, action, p.x, p.y, 0)
                grid.dispatchTouchEvent(e)
                e.recycle()
            }
        }
        onView(withId(R.id.tvCounter)).check(matches(withText("5 / 25")))
    }

    @Test
    fun flick_with_no_move_events_pops_along_the_path() {
        // A quick flick can go from DOWN straight to UP; the lift still carries the path.
        scenario.onActivity { activity ->
            val grid = activity.findViewById<BubbleGridView>(R.id.bubbleGridView)
            val from = grid.bubbleCenterForTest(10)
            val to = grid.bubbleCenterForTest(14)
            val t = SystemClock.uptimeMillis()
            for ((action, p) in listOf(MotionEvent.ACTION_DOWN to from, MotionEvent.ACTION_UP to to)) {
                val e = MotionEvent.obtain(t, t, action, p.x, p.y, 0)
                grid.dispatchTouchEvent(e)
                e.recycle()
            }
        }
        onView(withId(R.id.tvCounter)).check(matches(withText("5 / 25")))
    }

    // ── Surviving recreation (font size, language, split screen…) ────────────

    @Test
    fun popped_bubbles_survive_activity_recreation() {
        scenario.onActivity { activity ->
            val grid = activity.findViewById<BubbleGridView>(R.id.bubbleGridView)
            listOf(0, 6, 12).forEach { assertTrue(grid.accessibilityClickForTest(it)) }
        }
        scenario.recreate()
        onView(withId(R.id.tvCounter)).check(matches(withText("3 / 25")))
        scenario.onActivity { activity ->
            val grid = activity.findViewById<BubbleGridView>(R.id.bubbleGridView)
            assertEquals("Popped bubble, row 2, column 2", grid.accessibilityDescriptionForTest(6).toString())
            assertEquals("Bubble, row 2, column 3", grid.accessibilityDescriptionForTest(7).toString())
        }
    }

    @Test
    fun challenge_run_survives_recreation_and_records_a_best_time() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        openActionBarOverflowOrOptionsMenu(context)
        onView(withText("⏱  Challenge Mode")).perform(click())
        scenario.onActivity { activity ->
            val grid = activity.findViewById<BubbleGridView>(R.id.bubbleGridView)
            assertTrue(grid.accessibilityClickForTest(0))
            assertTrue(grid.accessibilityClickForTest(1))
        }
        scenario.recreate()
        onView(withId(R.id.challengeBar)).check(matches(isDisplayed()))
        onView(withId(R.id.tvCounter)).check(matches(withText("2 / 25")))
        // Finishing the restored run still counts as a timed run.
        scenario.onActivity { activity ->
            val grid = activity.findViewById<BubbleGridView>(R.id.bubbleGridView)
            (2 until 25).forEach { assertTrue(grid.accessibilityClickForTest(it)) }
        }
        onView(withId(R.id.tvBestTime)).check(matches(withText(startsWith("Best: "))))
    }

    // ── Menu checkmarks ───────────────────────────────────────────────────────

    @Test
    fun grid_size_and_theme_menus_check_the_current_choice() {
        scenario.onActivity { activity ->
            assertTrue(activity.isMenuItemCheckedForTest(R.id.menu_5x5))
            assertTrue(activity.isMenuItemCheckedForTest(R.id.menu_theme_rainbow))
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        openActionBarOverflowOrOptionsMenu(context)
        onView(withText("Grid Size")).perform(click())
        onView(withText("6 × 6  (36 bubbles)")).perform(click())
        openActionBarOverflowOrOptionsMenu(context)
        onView(withText("Color Theme")).perform(click())
        onView(withText("⚡ Neon")).perform(click())
        scenario.onActivity { activity ->
            assertTrue(activity.isMenuItemCheckedForTest(R.id.menu_6x6))
            assertFalse(activity.isMenuItemCheckedForTest(R.id.menu_5x5))
            assertTrue(activity.isMenuItemCheckedForTest(R.id.menu_theme_neon))
            assertFalse(activity.isMenuItemCheckedForTest(R.id.menu_theme_rainbow))
        }
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

    @Test
    fun best_time_label_follows_the_grid_size() {
        // Relaunch with a 4×4 record only.
        scenario.close()
        Prefs.init(InstrumentationRegistry.getInstrumentation().targetContext)
        Prefs.setBestTimeMs(4, 3_200L)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        openActionBarOverflowOrOptionsMenu(context)
        onView(withText("⏱  Challenge Mode")).perform(click())
        onView(withId(R.id.tvBestTime)).check(matches(withText("")))   // 5×5: no record

        openActionBarOverflowOrOptionsMenu(context)
        onView(withText("Grid Size")).perform(click())
        onView(withText("4 × 4  (16 bubbles)")).perform(click())
        onView(withId(R.id.tvBestTime)).check(matches(withText("Best: 3.2s")))
    }

    // ── Accessibility ─────────────────────────────────────────────────────────

    @Test
    fun bubbles_are_exposed_to_accessibility_and_poppable() {
        scenario.onActivity { activity ->
            val grid = activity.findViewById<BubbleGridView>(R.id.bubbleGridView)
            // Index 7 = row 2, column 3 on the default 5×5 grid.
            assertEquals("Bubble, row 2, column 3", grid.accessibilityDescriptionForTest(7).toString())
            assertTrue(grid.accessibilityClickForTest(7))
            assertEquals("Popped bubble, row 2, column 3", grid.accessibilityDescriptionForTest(7).toString())
            // A popped bubble no longer offers the click action.
            assertFalse(grid.accessibilityClickForTest(7))
        }
        onView(withId(R.id.tvCounter)).check(matches(withText("1 / 25")))
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
