package com.popitbubble

import org.junit.Assert.*
import org.junit.Test

class GridMathTest {

    // ── bubbleRadius ───────────────────────────────────────────────────────────

    @Test
    fun `bubbleRadius uses smaller cell dimension`() {
        // Wide cell: height is the constraint
        val r = GridMath.bubbleRadius(cellWidth = 80f, cellHeight = 60f)
        assertEquals(60f * 0.42f, r, 0.001f)
    }

    @Test
    fun `bubbleRadius uses custom fillFactor`() {
        val r = GridMath.bubbleRadius(100f, 100f, fillFactor = 0.5f)
        assertEquals(50f, r, 0.001f)
    }

    // ── bubbleCenter ──────────────────────────────────────────────────────────

    @Test
    fun `bubbleCenter origin cell is offset by half cell plus padding`() {
        val (cx, cy) = GridMath.bubbleCenter(
            col = 0, row = 0,
            cellWidth = 60f, cellHeight = 60f,
            paddingH = 10f, paddingV = 10f
        )
        assertEquals(10f + 30f, cx, 0.001f)
        assertEquals(10f + 30f, cy, 0.001f)
    }

    @Test
    fun `bubbleCenter advances correctly across columns`() {
        val (cx1, _) = GridMath.bubbleCenter(0, 0, 60f, 60f, 0f, 0f)
        val (cx2, _) = GridMath.bubbleCenter(1, 0, 60f, 60f, 0f, 0f)
        assertEquals(60f, cx2 - cx1, 0.001f)
    }

    // ── isTouching ────────────────────────────────────────────────────────────

    @Test
    fun `isTouching returns true when point is at centre`() {
        assertTrue(GridMath.isTouching(50f, 50f, 50f, 50f, 20f))
    }

    @Test
    fun `isTouching returns true when point is exactly on edge`() {
        assertTrue(GridMath.isTouching(70f, 50f, 50f, 50f, 20f))
    }

    @Test
    fun `isTouching returns false when point is outside radius`() {
        assertFalse(GridMath.isTouching(71f, 50f, 50f, 50f, 20f))
    }

    @Test
    fun `isTouching works diagonally`() {
        // Point 14.1 units away diagonally from (50,50) — just inside r=15
        assertTrue(GridMath.isTouching(60f, 60f, 50f, 50f, 15f))
        // Point 14.1 units away — just outside r=14
        assertFalse(GridMath.isTouching(60f, 60f, 50f, 50f, 14f))
    }

    // ── colour helpers ────────────────────────────────────────────────────────

    @Test
    fun `lighten with factor 0 returns original colour`() {
        val (r, g, b) = GridMath.lighten(100, 150, 200, 0f)
        assertEquals(100, r); assertEquals(150, g); assertEquals(200, b)
    }

    @Test
    fun `lighten with factor 1 returns white`() {
        val (r, g, b) = GridMath.lighten(50, 100, 200, 1f)
        assertEquals(255, r); assertEquals(255, g); assertEquals(255, b)
    }

    @Test
    fun `darken with factor 0 returns original colour`() {
        val (r, g, b) = GridMath.darken(100, 150, 200, 0f)
        assertEquals(100, r); assertEquals(150, g); assertEquals(200, b)
    }

    @Test
    fun `darken with factor 1 returns black`() {
        val (r, g, b) = GridMath.darken(100, 150, 200, 1f)
        assertEquals(0, r); assertEquals(0, g); assertEquals(0, b)
    }

    @Test
    fun `colour channels are clamped to 0-255`() {
        val (r, g, b) = GridMath.lighten(255, 255, 255, 0.5f)
        assertTrue(r in 0..255 && g in 0..255 && b in 0..255)
    }

    // ── Bubble state ──────────────────────────────────────────────────────────

    @Test
    fun `Bubble defaults to not popped with scale 1`() {
        val b = Bubble(row = 0, col = 0, color = 0xFF6B9D.toInt())
        assertFalse(b.isPopped)
        assertEquals(1f, b.animScale, 0.001f)
    }

    @Test
    fun `Bubble can be marked as popped`() {
        val b = Bubble(row = 0, col = 0, color = 0xFF6B9D.toInt())
        b.isPopped = true
        assertTrue(b.isPopped)
    }
}
