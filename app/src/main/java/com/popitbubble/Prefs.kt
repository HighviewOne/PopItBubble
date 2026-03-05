package com.popitbubble

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "popitbubble_prefs"
    private const val KEY_SOUND     = "sound_enabled"
    private const val KEY_HAPTIC    = "haptic_enabled"
    private const val KEY_GRID      = "grid_size"
    private const val KEY_THEME     = "color_theme"
    private const val KEY_BEST_TIME = "best_time_ms"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var soundEnabled: Boolean = true
    var hapticEnabled: Boolean = true
    var gridSize: Int = 5
    var colorTheme: String = "rainbow"
    var bestTimeMs: Long = 0L

    fun load(ctx: Context) {
        val p = prefs(ctx)
        soundEnabled  = p.getBoolean(KEY_SOUND,  true)
        hapticEnabled = p.getBoolean(KEY_HAPTIC, true)
        gridSize      = p.getInt(KEY_GRID, 5)
        colorTheme    = p.getString(KEY_THEME, "rainbow") ?: "rainbow"
        bestTimeMs    = p.getLong(KEY_BEST_TIME, 0L)
    }

    fun save(ctx: Context) {
        prefs(ctx).edit()
            .putBoolean(KEY_SOUND,  soundEnabled)
            .putBoolean(KEY_HAPTIC, hapticEnabled)
            .putInt(KEY_GRID, gridSize)
            .putString(KEY_THEME, colorTheme)
            .putLong(KEY_BEST_TIME, bestTimeMs)
            .apply()
    }
}
