package com.popitbubble

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit

/**
 * App settings, read and written straight through to SharedPreferences
 * (which caches in memory), so there is no separate copy to load or save.
 * Call [init] before use; it is cheap and safe to call from every activity.
 */
object Prefs {
    private const val NAME = "popitbubble_prefs"
    private const val KEY_SOUND  = "sound_enabled"
    private const val KEY_HAPTIC = "haptic_enabled"
    private const val KEY_GRID   = "grid_size"
    private const val KEY_THEME  = "color_theme"
    private const val KEY_BEST_TIME_PREFIX = "best_time_ms_"
    /** Pre-1.4 single best time, shared by every grid size. Migrated by [init]. */
    private const val KEY_LEGACY_BEST_TIME = "best_time_ms"

    const val DEFAULT_GRID_SIZE = 5

    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        if (::prefs.isInitialized) return
        prefs = ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        migrateLegacyBestTime()
    }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit { putBoolean(KEY_SOUND, value) }

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) = prefs.edit { putBoolean(KEY_HAPTIC, value) }

    var gridSize: Int
        get() = prefs.getInt(KEY_GRID, DEFAULT_GRID_SIZE)
        set(value) = prefs.edit { putInt(KEY_GRID, value) }

    var colorTheme: Theme
        get() = Theme.byName(prefs.getString(KEY_THEME, null))
        set(value) = prefs.edit { putString(KEY_THEME, value.displayName) }

    /** Fastest Challenge Mode time for an [size]×[size] grid in ms, or 0 if none yet. */
    fun bestTimeMs(size: Int): Long = prefs.getLong(KEY_BEST_TIME_PREFIX + size, 0L)

    fun setBestTimeMs(size: Int, ms: Long) = prefs.edit { putLong(KEY_BEST_TIME_PREFIX + size, ms) }

    /**
     * Earlier versions kept one best time for all grid sizes. Attribute it to
     * the grid size saved at the time (the one most likely played) so nobody
     * loses their record, then drop the old key.
     */
    @VisibleForTesting
    internal fun migrateLegacyBestTime() {
        if (!prefs.contains(KEY_LEGACY_BEST_TIME)) return
        val legacy = prefs.getLong(KEY_LEGACY_BEST_TIME, 0L)
        val size = gridSize
        prefs.edit {
            if (legacy > 0L && bestTimeMs(size) == 0L) putLong(KEY_BEST_TIME_PREFIX + size, legacy)
            remove(KEY_LEGACY_BEST_TIME)
        }
    }
}
