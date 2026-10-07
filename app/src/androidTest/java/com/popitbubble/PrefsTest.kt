package com.popitbubble

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrefsTest {

    private val raw = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("popitbubble_prefs", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        raw.edit().clear().commit()
        Prefs.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun best_times_are_kept_per_grid_size() {
        Prefs.setBestTimeMs(4, 3_200L)
        Prefs.setBestTimeMs(7, 18_500L)
        assertEquals(3_200L, Prefs.bestTimeMs(4))
        assertEquals(0L, Prefs.bestTimeMs(5))
        assertEquals(18_500L, Prefs.bestTimeMs(7))
    }

    @Test
    fun legacy_best_time_moves_to_the_saved_grid_size() {
        raw.edit().putInt("grid_size", 6).putLong("best_time_ms", 9_100L).commit()
        Prefs.migrateLegacyBestTime()
        assertEquals(9_100L, Prefs.bestTimeMs(6))
        assertEquals(0L, Prefs.bestTimeMs(5))
        assertFalse(raw.contains("best_time_ms"))
    }

    @Test
    fun legacy_migration_keeps_an_existing_per_size_record() {
        raw.edit().putInt("grid_size", 5).putLong("best_time_ms", 9_100L)
            .putLong("best_time_ms_5", 4_000L).commit()
        Prefs.migrateLegacyBestTime()
        assertEquals(4_000L, Prefs.bestTimeMs(5))
        assertFalse(raw.contains("best_time_ms"))
    }

    @Test
    fun unknown_theme_falls_back_to_rainbow() {
        raw.edit().putString("color_theme", "no-such-theme").commit()
        assertEquals(Theme.RAINBOW, Prefs.colorTheme)
        Prefs.colorTheme = Theme.NEON
        assertEquals("neon", raw.getString("color_theme", null))
    }
}
