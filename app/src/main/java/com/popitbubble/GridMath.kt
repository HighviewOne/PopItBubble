package com.popitbubble

/**
 * Pure, Android-free grid calculation utilities.
 * Kept separate so they can be unit-tested on the JVM without Robolectric.
 */
object GridMath {

    /** Returns the bubble radius for the given cell dimensions and padding factor. */
    fun bubbleRadius(cellWidth: Float, cellHeight: Float, fillFactor: Float = 0.42f): Float =
        minOf(cellWidth, cellHeight) * fillFactor

    /** Returns the (cx, cy) centre of a cell at [row, col]. */
    fun bubbleCenter(
        col: Int, row: Int,
        cellWidth: Float, cellHeight: Float,
        paddingH: Float, paddingV: Float
    ): Pair<Float, Float> {
        val cx = paddingH + cellWidth * col + cellWidth / 2f
        val cy = paddingV + cellHeight * row + cellHeight / 2f
        return cx to cy
    }

    /**
     * True if the touch point (tx, ty) falls within [radius] of bubble centre (bx, by).
     * Uses squared distance to avoid sqrt.
     */
    fun isTouching(tx: Float, ty: Float, bx: Float, by: Float, radius: Float): Boolean {
        val dx = tx - bx
        val dy = ty - by
        return dx * dx + dy * dy <= radius * radius
    }

    /** Lighten a packed RGB colour by [factor] (0..1). */
    fun lighten(r: Int, g: Int, b: Int, factor: Float): Triple<Int, Int, Int> {
        fun chan(v: Int) = (v + ((255 - v) * factor)).toInt().coerceIn(0, 255)
        return Triple(chan(r), chan(g), chan(b))
    }

    /** Darken a packed RGB colour by [factor] (0..1). */
    fun darken(r: Int, g: Int, b: Int, factor: Float): Triple<Int, Int, Int> {
        fun chan(v: Int) = (v * (1f - factor)).toInt().coerceIn(0, 255)
        return Triple(chan(r), chan(g), chan(b))
    }
}
