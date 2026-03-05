package com.popitbubble

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.Chronometer
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bubbleGridView: BubbleGridView
    private lateinit var soundManager: SoundManager
    private lateinit var tvCounter: TextView
    private lateinit var tvAllPopped: TextView
    private lateinit var fabReset: FloatingActionButton

    // Challenge mode
    private lateinit var challengeBar: LinearLayout
    private lateinit var chronometer: Chronometer
    private lateinit var tvBestTime: TextView
    private var challengeMode = false
    private var challengeStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        Prefs.load(this)

        soundManager = SoundManager(this)
        bubbleGridView = findViewById(R.id.bubbleGridView)
        tvCounter     = findViewById(R.id.tvCounter)
        tvAllPopped   = findViewById(R.id.tvAllPopped)
        fabReset      = findViewById(R.id.fabReset)
        challengeBar  = findViewById(R.id.challengeBar)
        chronometer   = findViewById(R.id.chronometer)
        tvBestTime    = findViewById(R.id.tvBestTime)

        bubbleGridView.soundManager = soundManager

        bubbleGridView.onPopListener = { popped, total ->
            updateCounter(popped, total)
            if (challengeMode && !challengeStarted && popped == 1) {
                challengeStarted = true
                chronometer.base = SystemClock.elapsedRealtime()
                chronometer.start()
            }
        }

        bubbleGridView.onAllPoppedListener = {
            if (challengeMode && challengeStarted) {
                chronometer.stop()
                val elapsed = SystemClock.elapsedRealtime() - chronometer.base
                checkBestTime(elapsed)
                showAllPoppedCelebration(formatTime(elapsed))
            } else {
                showAllPoppedCelebration(null)
            }
        }

        fabReset.setOnClickListener { resetGame() }

        updateCounter(0, bubbleGridView.getTotalCount())
        updateBestTimeLabel()
    }

    // ── Counter ───────────────────────────────────────────────────────────────

    private fun updateCounter(popped: Int, total: Int) {
        tvCounter.text = "$popped / $total"
    }

    // ── Challenge mode ────────────────────────────────────────────────────────

    private fun toggleChallengeMode() {
        challengeMode = !challengeMode
        challengeBar.visibility = if (challengeMode) View.VISIBLE else View.GONE
        if (!challengeMode) chronometer.stop()
        resetGame()
    }

    private fun checkBestTime(elapsedMs: Long) {
        val best = Prefs.bestTimeMs
        if (best == 0L || elapsedMs < best) {
            Prefs.bestTimeMs = elapsedMs
            Prefs.save(this)
        }
        updateBestTimeLabel()
    }

    private fun updateBestTimeLabel() {
        val best = Prefs.bestTimeMs
        tvBestTime.text = if (best > 0L) "Best: ${formatTime(best)}" else ""
    }

    private fun formatTime(ms: Long): String {
        val s = ms / 1000
        val tenths = (ms % 1000) / 100
        return "%d.%ds".format(s, tenths)
    }

    // ── Celebration ───────────────────────────────────────────────────────────

    private fun showAllPoppedCelebration(timeStr: String?) {
        tvAllPopped.text = if (timeStr != null) "🎉 ${timeStr}! 🎉" else "🎉 All Popped! 🎉"
        tvAllPopped.visibility = View.VISIBLE
        tvAllPopped.alpha  = 0f
        tvAllPopped.scaleX = 0.5f
        tvAllPopped.scaleY = 0.5f

        val fadeIn = ObjectAnimator.ofFloat(tvAllPopped, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(tvAllPopped, "scaleX", 0.5f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(tvAllPopped, "scaleY", 0.5f, 1.1f, 1f)
        scaleX.interpolator = BounceInterpolator()
        scaleY.interpolator = BounceInterpolator()

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            duration = 600
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tvAllPopped.postDelayed({ resetGame() }, 1500)
                }
            })
            start()
        }
    }

    private fun resetGame() {
        tvAllPopped.visibility = View.GONE
        chronometer.stop()
        chronometer.base = SystemClock.elapsedRealtime()
        challengeStarted = false
        bubbleGridView.reset()
        updateCounter(0, bubbleGridView.getTotalCount())
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset    -> { resetGame(); true }
            R.id.menu_challenge -> { toggleChallengeMode(); true }
            R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.menu_4x4 -> { bubbleGridView.setGridSize(4, 4); updateCounter(0, bubbleGridView.getTotalCount()); true }
            R.id.menu_5x5 -> { bubbleGridView.setGridSize(5, 5); updateCounter(0, bubbleGridView.getTotalCount()); true }
            R.id.menu_6x6 -> { bubbleGridView.setGridSize(6, 6); updateCounter(0, bubbleGridView.getTotalCount()); true }
            R.id.menu_7x7 -> { bubbleGridView.setGridSize(7, 7); updateCounter(0, bubbleGridView.getTotalCount()); true }
            R.id.menu_theme_rainbow -> { bubbleGridView.currentTheme = "rainbow"; true }
            R.id.menu_theme_pink   -> { bubbleGridView.currentTheme = "pink";    true }
            R.id.menu_theme_blue   -> { bubbleGridView.currentTheme = "blue";    true }
            R.id.menu_theme_pastel -> { bubbleGridView.currentTheme = "pastel";  true }
            R.id.menu_theme_neon   -> { bubbleGridView.currentTheme = "neon";    true }
            R.id.menu_theme_candy  -> { bubbleGridView.currentTheme = "candy";   true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
