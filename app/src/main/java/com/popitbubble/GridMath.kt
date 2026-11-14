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

    /**
     * True if the finger path from (x0, y0) to (x1, y1) passes within [radius]
     * of bubble centre (bx, by). A fast swipe moves far between touch samples,
     * so checking only the sample points would skip bubbles in between.
     */
    fun segmentTouches(
        x0: Float, y0: Float, x1: Float, y1: Float,
        bx: Float, by: Float, radius: Float
    ): Boolean {
        val dx = x1 - x0
        val dy = y1 - y0
        val lenSq = dx * dx + dy * dy
        // Closest point on the segment to the centre, clamped to the endpoints.
        val t = if (lenSq == 0f) 0f else (((bx - x0) * dx + (by - y0) * dy) / lenSq).coerceIn(0f, 1f)
        return isTouching(x0 + t * dx, y0 + t * dy, bx, by, radius)
    }

    /**
     * Half the side length, in whole pixels, of a square sprite that fits a
     * bubble of [radius] including its drop shadow. The shadow circle is
     * offset by (0.08r, 0.12r) with radius 0.92r and a 0.25r blur; Skia maps a
     * blur radius to sigma ≈ 0.577·radius + 0.5, and beyond ~2σ past the edge
     * the 50-alpha shadow is invisible. That is about 1.35r + 1px, so 1.5r plus
     * two pixels leaves headroom.
     */
    fun spriteHalfSize(radius: Float): Int = kotlin.math.ceil(radius * 1.5f).toInt() + 2

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
