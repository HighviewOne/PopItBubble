package com.popitbubble

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
import kotlin.math.min
import kotlin.math.sqrt

data class Bubble(
    val row: Int,
    val col: Int,
    var cx: Float = 0f,
    var cy: Float = 0f,
    var radius: Float = 0f,
    val color: Int,
    var isPopped: Boolean = false,
    var animScale: Float = 1f,
    var celebrateAlpha: Float = 0f
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

    // Color themes
    private val themes = mapOf(
        "rainbow" to listOf(
            Color.parseColor("#FF6B9D"),  // hot pink
            Color.parseColor("#C44DFF"),  // purple
            Color.parseColor("#4D9FFF"),  // blue
            Color.parseColor("#4DFFB4"),  // teal
            Color.parseColor("#FFD14D"),  // yellow
            Color.parseColor("#FF8C4D"),  // orange
        ),
        "pink" to listOf(
            Color.parseColor("#FFB3D1"),
            Color.parseColor("#FF80B3"),
            Color.parseColor("#FF4D94"),
            Color.parseColor("#FF1A75"),
            Color.parseColor("#CC0055"),
        ),
        "blue" to listOf(
            Color.parseColor("#B3D9FF"),
            Color.parseColor("#80BFFF"),
            Color.parseColor("#4DA6FF"),
            Color.parseColor("#1A8CFF"),
            Color.parseColor("#0066CC"),
        ),
        "pastel" to listOf(
            Color.parseColor("#FFD1DC"),
            Color.parseColor("#D1F0FF"),
            Color.parseColor("#D1FFD6"),
            Color.parseColor("#FFF5D1"),
            Color.parseColor("#E8D1FF"),
        )
    )

    var currentTheme = "rainbow"
        set(value) { field = value; initBubbles(); invalidate() }

    // Paints
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(50, 0, 0, 0)
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }
    private val celebratePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Animators per bubble index
    private val animators = HashMap<Int, ValueAnimator>()

    // Callbacks
    var onAllPoppedListener: (() -> Unit)? = null
    var onPopListener: ((poppedCount: Int, total: Int) -> Unit)? = null

    // Haptic
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // Sound
    var soundManager: SoundManager? = null

    // Background gradient
    private val bgPaint = Paint()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    // ─── Public API ────────────────────────────────────────────────

    fun setGridSize(c: Int, r: Int) {
        cols = c; rows = r
        initBubbles()
    }

    fun reset() {
        animators.values.forEach { it.cancel() }
        animators.clear()
        bubbles.forEach { it.isPopped = false; it.animScale = 1f; it.celebrateAlpha = 0f }
        invalidate()
    }

    fun getPoppedCount() = bubbles.count { it.isPopped }
    fun getTotalCount() = bubbles.size

    // ─── Layout ───────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Background gradient
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

        animators.values.forEach { it.cancel() }
        animators.clear()
        bubbles.clear()

        val paddingH = width * 0.04f
        val paddingV = height * 0.04f
        val usableW = width - paddingH * 2
        val usableH = height - paddingV * 2
        val cellW = usableW / cols
        val cellH = usableH / rows
        val radius = min(cellW, cellH) * 0.42f

        val colorList = themes[currentTheme] ?: themes["rainbow"]!!
        var idx = 0

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cx = paddingH + cellW * col + cellW / 2f
                val cy = paddingV + cellH * row + cellH / 2f
                bubbles.add(
                    Bubble(
                        row = row, col = col,
                        cx = cx, cy = cy, radius = radius,
                        color = colorList[idx % colorList.size]
                    )
                )
                idx++
            }
        }
        invalidate()
    }

    // ─── Drawing ──────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw all bubbles
        for ((index, bubble) in bubbles.withIndex()) {
            drawBubble(canvas, bubble, index)
        }
    }

    private fun drawBubble(canvas: Canvas, b: Bubble, index: Int) {
        val scale = b.animScale
        val r = b.radius * scale
        val cx = b.cx
        val cy = b.cy

        canvas.save()
        canvas.scale(scale, scale, cx, cy)

        if (b.isPopped) {
            drawPoppedBubble(canvas, cx, cy, b.radius, b.color)
        } else {
            drawInflatedBubble(canvas, cx, cy, b.radius, b.color)
        }

        canvas.restore()
    }

    private fun drawInflatedBubble(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        // Drop shadow
        shadowPaint.maskFilter = BlurMaskFilter(r * 0.25f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(cx + r * 0.08f, cy + r * 0.12f, r * 0.92f, shadowPaint)

        // Main bubble body – radial gradient for 3D dome look
        val lightX = cx - r * 0.28f
        val lightY = cy - r * 0.28f
        val gradient = RadialGradient(
            lightX, lightY, r * 1.4f,
            intArrayOf(
                lighten(color, 0.70f),
                color,
                darken(color, 0.30f)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        bubblePaint.shader = gradient
        bubblePaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, r, bubblePaint)
        bubblePaint.shader = null

        // Specular highlight (top-left gloss)
        val specGrad = RadialGradient(
            cx - r * 0.28f, cy - r * 0.32f, r * 0.55f,
            intArrayOf(Color.argb(200, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        bubblePaint.shader = specGrad
        canvas.drawCircle(cx - r * 0.14f, cy - r * 0.18f, r * 0.5f, bubblePaint)
        bubblePaint.shader = null

        // Soft rim
        bubblePaint.style = Paint.Style.STROKE
        bubblePaint.strokeWidth = r * 0.06f
        bubblePaint.color = darken(color, 0.15f)
        canvas.drawCircle(cx, cy, r - r * 0.03f, bubblePaint)
        bubblePaint.style = Paint.Style.FILL
    }

    private fun drawPoppedBubble(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        // Outer silicone ring
        val ringGrad = RadialGradient(
            cx, cy, r,
            intArrayOf(
                darken(color, 0.55f),
                darken(color, 0.35f),
                darken(color, 0.20f)
            ),
            floatArrayOf(0.0f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        bubblePaint.shader = ringGrad
        bubblePaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, r, bubblePaint)
        bubblePaint.shader = null

        // Inner concave depression
        val innerGrad = RadialGradient(
            cx, cy, r * 0.7f,
            intArrayOf(darken(color, 0.70f), darken(color, 0.50f)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        bubblePaint.shader = innerGrad
        canvas.drawCircle(cx, cy, r * 0.68f, bubblePaint)
        bubblePaint.shader = null

        // Subtle inner rim highlight
        bubblePaint.style = Paint.Style.STROKE
        bubblePaint.strokeWidth = r * 0.05f
        bubblePaint.color = Color.argb(60, 255, 255, 255)
        canvas.drawCircle(cx, cy, r * 0.68f, bubblePaint)
        bubblePaint.style = Paint.Style.FILL
    }

    // ─── Touch ────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                // Support multi-touch: check each pointer
                for (pIdx in 0 until event.pointerCount) {
                    val tx = event.getX(pIdx)
                    val ty = event.getY(pIdx)
                    checkTouchAt(tx, ty)
                }
            }
        }
        return true
    }

    private fun checkTouchAt(tx: Float, ty: Float) {
        for ((index, bubble) in bubbles.withIndex()) {
            if (!bubble.isPopped) {
                val dx = tx - bubble.cx
                val dy = ty - bubble.cy
                if (dx * dx + dy * dy <= bubble.radius * bubble.radius) {
                    popBubble(index)
                }
            }
        }
    }

    // ─── Pop Logic ────────────────────────────────────────────────

    private fun popBubble(index: Int) {
        val bubble = bubbles[index]
        if (bubble.isPopped) return

        bubble.isPopped = true

        // Sound
        soundManager?.playPop()

        // Haptic feedback
        vibrate()

        // Pop animation: squeeze down then settle
        animators[index]?.cancel()
        val anim = ValueAnimator.ofFloat(1f, 0.60f, 0.88f).apply {
            duration = 220
            interpolator = OvershootInterpolator(1.8f)
            addUpdateListener {
                bubble.animScale = it.animatedValue as Float
                invalidate()
            }
        }
        animators[index] = anim
        anim.start()

        // Notify listener
        val popped = getPoppedCount()
        onPopListener?.invoke(popped, bubbles.size)

        // Check win condition
        if (popped == bubbles.size) {
            postDelayed({ onAllPoppedListener?.invoke() }, 400)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(25, 180)
                )
            } else {
                vibrator.vibrate(25)
            }
        } catch (_: Exception) { /* vibration not available */ }
    }

    // ─── Color Helpers ────────────────────────────────────────────

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun darken(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - factor)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
