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
import androidx.appcompat.app.AppCompatActivity
import com.popitbubble.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var soundManager: SoundManager

    // Challenge mode
    private var challengeMode = false
    private var challengeStarted = false
    private var celebrationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        Prefs.load(this)

        soundManager = SoundManager(this)
        binding.bubbleGridView.soundManager = soundManager

        // Restore saved grid size and theme
        binding.bubbleGridView.setGridSize(Prefs.gridSize, Prefs.gridSize)
        binding.bubbleGridView.currentTheme = Prefs.colorTheme

        binding.bubbleGridView.onPopListener = { popped, total ->
            updateCounter(popped, total)
            if (challengeMode && !challengeStarted && popped == 1) {
                challengeStarted = true
                binding.chronometer.base = SystemClock.elapsedRealtime()
                binding.chronometer.start()
            }
        }

        binding.bubbleGridView.onAllPoppedListener = {
            if (challengeMode && challengeStarted) {
                binding.chronometer.stop()
                val elapsed = SystemClock.elapsedRealtime() - binding.chronometer.base
                checkBestTime(elapsed)
                showAllPoppedCelebration(formatTime(elapsed))
            } else {
                showAllPoppedCelebration(null)
            }
        }

        binding.fabReset.setOnClickListener { resetGame() }

        updateCounter(0, binding.bubbleGridView.getTotalCount())
        updateBestTimeLabel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("challengeMode", challengeMode)
        outState.putBoolean("challengeStarted", challengeStarted)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        challengeMode = savedInstanceState.getBoolean("challengeMode", false)
        challengeStarted = savedInstanceState.getBoolean("challengeStarted", false)
        binding.challengeBar.visibility = if (challengeMode) View.VISIBLE else View.GONE
    }

    private fun updateCounter(popped: Int, total: Int) {
        binding.tvCounter.text = "$popped / $total"
    }

    private fun toggleChallengeMode() {
        challengeMode = !challengeMode
        binding.challengeBar.visibility = if (challengeMode) View.VISIBLE else View.GONE
        if (!challengeMode) binding.chronometer.stop()
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
        binding.tvBestTime.text = if (best > 0L) "Best: ${formatTime(best)}" else ""
    }

    private fun formatTime(ms: Long): String {
        val s = ms / 1000
        val tenths = (ms % 1000) / 100
        return "%d.%ds".format(s, tenths)
    }

    private fun showAllPoppedCelebration(timeStr: String?) {
        binding.tvAllPopped.text = if (timeStr != null) "🎉 ${timeStr}! 🎉" else "🎉 All Popped! 🎉"
        binding.tvAllPopped.visibility = View.VISIBLE
        binding.tvAllPopped.alpha  = 0f
        binding.tvAllPopped.scaleX = 0.5f
        binding.tvAllPopped.scaleY = 0.5f

        val fadeIn = ObjectAnimator.ofFloat(binding.tvAllPopped, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(binding.tvAllPopped, "scaleX", 0.5f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.tvAllPopped, "scaleY", 0.5f, 1.1f, 1f)
        scaleX.interpolator = BounceInterpolator()
        scaleY.interpolator = BounceInterpolator()

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            duration = 600
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    celebrationRunnable = Runnable { resetGame() }
                    binding.tvAllPopped.postDelayed(celebrationRunnable!!, 1500)
                }
            })
            start()
        }
    }

    private fun resetGame() {
        binding.tvAllPopped.visibility = View.GONE
        binding.chronometer.stop()
        binding.chronometer.base = SystemClock.elapsedRealtime()
        challengeStarted = false
        binding.bubbleGridView.reset()
        updateCounter(0, binding.bubbleGridView.getTotalCount())
    }

    private fun setGridSize(size: Int) {
        binding.bubbleGridView.setGridSize(size, size)
        updateCounter(0, binding.bubbleGridView.getTotalCount())
        Prefs.gridSize = size
        Prefs.save(this)
    }

    private fun setTheme(name: String) {
        binding.bubbleGridView.currentTheme = name
        Prefs.colorTheme = name
        Prefs.save(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset    -> { resetGame(); true }
            R.id.menu_challenge -> { toggleChallengeMode(); true }
            R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.menu_4x4 -> { setGridSize(4); true }
            R.id.menu_5x5 -> { setGridSize(5); true }
            R.id.menu_6x6 -> { setGridSize(6); true }
            R.id.menu_7x7 -> { setGridSize(7); true }
            R.id.menu_theme_rainbow -> { setTheme("rainbow"); true }
            R.id.menu_theme_pink   -> { setTheme("pink");    true }
            R.id.menu_theme_blue   -> { setTheme("blue");    true }
            R.id.menu_theme_pastel -> { setTheme("pastel");  true }
            R.id.menu_theme_neon   -> { setTheme("neon");    true }
            R.id.menu_theme_candy  -> { setTheme("candy");   true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        celebrationRunnable?.let { binding.tvAllPopped.removeCallbacks(it) }
        soundManager.release()
    }
}
