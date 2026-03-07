package com.popitbubble

import android.graphics.Color

enum class Theme(val displayName: String, val colors: List<Int>) {
    RAINBOW("rainbow", listOf(
        Color.parseColor("#FF6B9D"),
        Color.parseColor("#C44DFF"),
        Color.parseColor("#4D9FFF"),
        Color.parseColor("#4DFFB4"),
        Color.parseColor("#FFD14D"),
        Color.parseColor("#FF8C4D"),
    )),
    PINK("pink", listOf(
        Color.parseColor("#FFB3D1"),
        Color.parseColor("#FF80B3"),
        Color.parseColor("#FF4D94"),
        Color.parseColor("#FF1A75"),
        Color.parseColor("#CC0055"),
    )),
    BLUE("blue", listOf(
        Color.parseColor("#B3D9FF"),
        Color.parseColor("#80BFFF"),
        Color.parseColor("#4DA6FF"),
        Color.parseColor("#1A8CFF"),
        Color.parseColor("#0066CC"),
    )),
    PASTEL("pastel", listOf(
        Color.parseColor("#FFD1DC"),
        Color.parseColor("#D1F0FF"),
        Color.parseColor("#D1FFD6"),
        Color.parseColor("#FFF5D1"),
        Color.parseColor("#E8D1FF"),
    )),
    NEON("neon", listOf(
        Color.parseColor("#FF00FF"),
        Color.parseColor("#00FFFF"),
        Color.parseColor("#00FF41"),
        Color.parseColor("#FF6600"),
        Color.parseColor("#FFFF00"),
        Color.parseColor("#0066FF"),
    )),
    CANDY("candy", listOf(
        Color.parseColor("#FF6EB4"),
        Color.parseColor("#FF9A3C"),
        Color.parseColor("#FFE44D"),
        Color.parseColor("#5CE65C"),
        Color.parseColor("#5CC8FF"),
        Color.parseColor("#C87CFF"),
    ));

    companion object {
        fun byName(name: String): Theme = values().find { it.displayName == name } ?: RAINBOW
    }
}
