package com.popitbubble

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "popitbubble_prefs"
    private const val KEY_SOUND   = "sound_enabled"
    private const val KEY_HAPTIC  = "haptic_enabled"
    private const val KEY_GRID    = "grid_size"
    private const val KEY_THEME   = "color_theme"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var soundEnabled: Boolean = true
    var hapticEnabled: Boolean = true
    var gridSize: Int = 5
    var colorTheme: String = "rainbow"

    fun load(ctx: Context) {
        val p = prefs(ctx)
        soundEnabled  = p.getBoolean(KEY_SOUND,  true)
        hapticEnabled = p.getBoolean(KEY_HAPTIC, true)
        gridSize      = p.getInt(KEY_GRID, 5)
        colorTheme    = p.getString(KEY_THEME, "rainbow") ?: "rainbow"
    }

    fun save(ctx: Context) {
        prefs(ctx).edit()
            .putBoolean(KEY_SOUND,  soundEnabled)
            .putBoolean(KEY_HAPTIC, hapticEnabled)
            .putInt(KEY_GRID, gridSize)
            .putString(KEY_THEME, colorTheme)
            .apply()
    }
}
