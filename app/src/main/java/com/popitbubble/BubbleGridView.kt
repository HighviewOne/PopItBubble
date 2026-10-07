package com.popitbubble

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt

/** One cell of the grid; mutable state, so a plain class rather than a data class. */
class Bubble(
    val row: Int,
    val col: Int,
    var cx: Float = 0f,
    var cy: Float = 0f,
    var radius: Float = 0f,
    var color: Int,
    var isPopped: Boolean = false,
    var animScale: Float = 1f
)

class BubbleGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Grid dimensions
    private var cols = 5
    private var rows = 5

    // Bubble data
    private val bubbles = mutableListOf<Bubble>()

    // Theme management
    var theme: Theme = Theme.RAINBOW
        set(value) {
            field = value
            recolorBubbles()
        }

    // Paints
    // Fills use shaders and must stay fully opaque: a shader fill is multiplied
    // by the paint's alpha, so strokes get their own paint.
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(50, 0, 0, 0)
    }
    private val spritePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    // Shader Caches
    private val inflatedShaders = mutableMapOf<Int, Shader>()
    private val poppedShaders = mutableMapOf<Int, Shader>()
    private val innerPoppedShaders = mutableMapOf<Int, Shader>()
    private var specShader: Shader? = null

    // Pre-rendered bubbles, one per colour and state. Rendering them once in
    // software keeps BlurMaskFilter working while onDraw stays on the GPU.
    private val inflatedSprites = mutableMapOf<Int, Bitmap>()
    private val poppedSprites = mutableMapOf<Int, Bitmap>()
    private var spriteHalf = 0

    // Animators per bubble index
    private val animators = HashMap<Int, ValueAnimator>()

    // Callbacks
    var onAllPoppedListener: (() -> Unit)? = null
    var onPopListener: ((poppedCount: Int, total: Int) -> Unit)? = null
    /** Fired whenever the grid is (re)built, e.g. after first layout or a size change. */
    var onGridChangedListener: ((total: Int) -> Unit)? = null

    private val allPoppedRunnable = Runnable { onAllPoppedListener?.invoke() }

    // Haptic
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    // Sound
    var soundManager: SoundManager? = null

    // Background gradient
    private val bgPaint = Paint()

    // ─── Public API ────────────────────────────────────────────────

    fun setGridSize(c: Int, r: Int) {
        cols = c; rows = r
        initBubbles()
    }

    fun reset() {
        removeCallbacks(allPoppedRunnable)
        animators.values.forEach { it.cancel() }
        animators.clear()
        bubbles.forEach { it.isPopped = false; it.animScale = 1f }
        invalidate()
    }

    fun getPoppedCount() = bubbles.count { it.isPopped }
    fun getTotalCount() = bubbles.size

    // ─── Layout ───────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.parseColor("#1A1A2E"),
            Color.parseColor("#16213E"),
            Shader.TileMode.CLAMP
        )

        initBubbles()
    }

    private fun initBubbles() {
        if (width == 0 || height == 0) return

        removeCallbacks(allPoppedRunnable)
        animators.values.forEach { it.cancel() }
        animators.clear()
        bubbles.clear()
        clearShaderCaches()

        val paddingH = width * 0.04f
        val paddingV = height * 0.04f
        val cellW = (width - paddingH * 2) / cols
        val cellH = (height - paddingV * 2) / rows
        val radius = GridMath.bubbleRadius(cellW, cellH)

        val colorList = theme.colors
        var idx = 0

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val (cx, cy) = GridMath.bubbleCenter(col, row, cellW, cellH, paddingH, paddingV)
                val color = colorList[idx % colorList.size]
                bubbles.add(
                    Bubble(
                        row = row, col = col,
                        cx = cx, cy = cy, radius = radius,
                        color = color
                    )
                )
                idx++
            }
        }

        prepareRendering(radius)
        invalidate()
        onGridChangedListener?.invoke(bubbles.size)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(allPoppedRunnable)
        animators.values.forEach { it.cancel() }
        animators.clear()
        super.onDetachedFromWindow()
    }

    /** Applies the current theme's colours in place, keeping popped state and animations. */
    private fun recolorBubbles() {
        if (bubbles.isEmpty()) return  // not laid out yet; initBubbles() will colour them
        val colorList = theme.colors
        bubbles.forEachIndexed { i, b -> b.color = colorList[i % colorList.size] }
        clearShaderCaches()
        prepareRendering(bubbles[0].radius)
        invalidate()
    }

    private fun prepareRendering(r: Float) {
        prepareShaders(r)
        buildSprites(r)
    }

    private fun buildSprites(r: Float) {
        inflatedSprites.clear()
        poppedSprites.clear()
        spriteHalf = GridMath.spriteHalfSize(r)
        val size = spriteHalf * 2
        theme.colors.distinct().forEach { color ->
            inflatedSprites[color] = renderSprite(size) { drawInflatedBubble(it, r, color) }
            poppedSprites[color] = renderSprite(size) { drawPoppedBubble(it, r, color) }
        }
    }

    private inline fun renderSprite(size: Int, draw: (Canvas) -> Unit): Bitmap {
        val bmp = createBitmap(size, size)
        val c = Canvas(bmp)
        c.translate(size / 2f, size / 2f)
        draw(c)
        return bmp
    }

    @VisibleForTesting
    internal fun spritesForTest(): List<Bitmap> = inflatedSprites.values + poppedSprites.values

    private fun clearShaderCaches() {
        inflatedShaders.clear()
        poppedShaders.clear()
        innerPoppedShaders.clear()
        specShader = null
    }

    private fun prepareShaders(r: Float) {
        val colorList = theme.colors
        
        // Specular highlight is the same for all bubbles
        specShader = RadialGradient(
            -r * 0.28f, -r * 0.32f, r * 0.55f,
            intArrayOf(Color.argb(200, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        colorList.distinct().forEach { color ->
            // Inflated Shader
            val (lr, lg, lb) = GridMath.lighten(Color.red(color), Color.green(color), Color.blue(color), 0.70f)
            val (dr, dg, db) = GridMath.darken(Color.red(color), Color.green(color), Color.blue(color), 0.30f)
            
            inflatedShaders[color] = RadialGradient(
                -r * 0.28f, -r * 0.28f, r * 1.4f,
                intArrayOf(Color.rgb(lr, lg, lb), color, Color.rgb(dr, dg, db)),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )

            // Popped Shaders
            val (d1r, d1g, d1b) = GridMath.darken(Color.red(color), Color.green(color), Color.blue(color), 0.55f)
            val (d2r, d2g, d2b) = GridMath.darken(Color.red(color), Color.green(color), Color.blue(color), 0.35f)
            val (d3r, d3g, d3b) = GridMath.darken(Color.red(color), Color.green(color), Color.blue(color), 0.20f)
            
            poppedShaders[color] = RadialGradient(
                0f, 0f, r,
                intArrayOf(Color.rgb(d1r, d1g, d1b), Color.rgb(d2r, d2g, d2b), Color.rgb(d3r, d3g, d3b)),
                floatArrayOf(0.0f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )

            val (d4r, d4g, d4b) = GridMath.darken(Color.red(color), Color.green(color), Color.blue(color), 0.70f)
            val (d5r, d5g, d5b) = GridMath.darken(Color.red(color), Color.green(color), Color.blue(color), 0.50f)

            innerPoppedShaders[color] = RadialGradient(
                0f, 0f, r * 0.7f,
                intArrayOf(Color.rgb(d4r, d4g, d4b), Color.rgb(d5r, d5g, d5b)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    // ─── Drawing ──────────────────────────────────────────────────
    // drawInflatedBubble / drawPoppedBubble render into the sprite bitmaps
    // (centred at 0,0); onDraw only blits those sprites.

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        for (bubble in bubbles) {
            drawBubble(canvas, bubble)
        }
    }

    private fun drawBubble(canvas: Canvas, b: Bubble) {
        val sprite = (if (b.isPopped) poppedSprites else inflatedSprites)[b.color] ?: return
        canvas.save()
        // Whole-pixel centre so unscaled sprites are drawn 1:1, without filtering blur.
        canvas.translate(b.cx.roundToInt().toFloat(), b.cy.roundToInt().toFloat())
        canvas.scale(b.animScale, b.animScale)
        canvas.drawBitmap(sprite, -spriteHalf.toFloat(), -spriteHalf.toFloat(), spritePaint)
        canvas.restore()
    }

    private fun drawInflatedBubble(canvas: Canvas, r: Float, color: Int) {
        // Drop shadow
        shadowPaint.maskFilter = BlurMaskFilter(r * 0.25f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(r * 0.08f, r * 0.12f, r * 0.92f, shadowPaint)

        // Main body
        bubblePaint.shader = inflatedShaders[color]
        canvas.drawCircle(0f, 0f, r, bubblePaint)

        // Specular
        bubblePaint.shader = specShader
        canvas.drawCircle(-r * 0.14f, -r * 0.18f, r * 0.5f, bubblePaint)
        bubblePaint.shader = null

        // Soft rim
        strokePaint.strokeWidth = r * 0.06f
        val (dr, dg, db) = GridMath.darken(Color.red(color), Color.green(color), Color.blue(color), 0.15f)
        strokePaint.color = Color.rgb(dr, dg, db)
        canvas.drawCircle(0f, 0f, r - r * 0.03f, strokePaint)
    }

    private fun drawPoppedBubble(canvas: Canvas, r: Float, color: Int) {
        // Outer ring
        bubblePaint.shader = poppedShaders[color]
        canvas.drawCircle(0f, 0f, r, bubblePaint)

        // Inner concave
        bubblePaint.shader = innerPoppedShaders[color]
        canvas.drawCircle(0f, 0f, r * 0.68f, bubblePaint)
        bubblePaint.shader = null

        // Subtle highlight
        strokePaint.strokeWidth = r * 0.05f
        strokePaint.color = Color.argb(60, 255, 255, 255)
        canvas.drawCircle(0f, 0f, r * 0.68f, strokePaint)
    }

    // ─── Touch ────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE -> {
                for (pIdx in 0 until event.pointerCount) {
                    checkTouchAt(event.getX(pIdx), event.getY(pIdx))
                }
            }
        }
        return true
    }

    private fun checkTouchAt(tx: Float, ty: Float) {
        for ((index, bubble) in bubbles.withIndex()) {
            if (!bubble.isPopped && GridMath.isTouching(tx, ty, bubble.cx, bubble.cy, bubble.radius)) {
                popBubble(index)
            }
        }
    }

    // ─── Pop Logic ────────────────────────────────────────────────

    private fun popBubble(index: Int) {
        val bubble = bubbles[index]
        if (bubble.isPopped) return
        bubble.isPopped = true

        if (Prefs.soundEnabled) soundManager?.playPop()
        if (Prefs.hapticEnabled) vibrate()

        animators[index]?.cancel()
        val anim = ValueAnimator.ofFloat(1f, 0.60f, 0.88f).apply {
            duration = 220
            interpolator = OvershootInterpolator(1.8f)
            addUpdateListener {
                bubble.animScale = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animators[index] === animation) animators.remove(index)
                }
            })
        }
        animators[index] = anim
        anim.start()

        onPopListener?.invoke(getPoppedCount(), bubbles.size)
        if (getPoppedCount() == bubbles.size) {
            postDelayed(allPoppedRunnable, 400)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        try {
            val vibrator = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, 180))
            } else {
                vibrator.vibrate(25)
            }
        } catch (_: Exception) { }
    }
}
