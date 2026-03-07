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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset    -> { resetGame(); true }
            R.id.menu_challenge -> { toggleChallengeMode(); true }
            R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.menu_4x4 -> { binding.bubbleGridView.setGridSize(4, 4); updateCounter(0, binding.bubbleGridView.getTotalCount()); true }
            R.id.menu_5x5 -> { binding.bubbleGridView.setGridSize(5, 5); updateCounter(0, binding.bubbleGridView.getTotalCount()); true }
            R.id.menu_6x6 -> { binding.bubbleGridView.setGridSize(6, 6); updateCounter(0, binding.bubbleGridView.getTotalCount()); true }
            R.id.menu_7x7 -> { binding.bubbleGridView.setGridSize(7, 7); updateCounter(0, binding.bubbleGridView.getTotalCount()); true }
            R.id.menu_theme_rainbow -> { binding.bubbleGridView.currentTheme = "rainbow"; true }
            R.id.menu_theme_pink   -> { binding.bubbleGridView.currentTheme = "pink";    true }
            R.id.menu_theme_blue   -> { binding.bubbleGridView.currentTheme = "blue";    true }
            R.id.menu_theme_pastel -> { binding.bubbleGridView.currentTheme = "pastel";  true }
            R.id.menu_theme_neon   -> { binding.bubbleGridView.currentTheme = "neon";    true }
            R.id.menu_theme_candy  -> { binding.bubbleGridView.currentTheme = "candy";   true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        celebrationRunnable?.let { binding.tvAllPopped.removeCallbacks(it) }
        soundManager.release()
    }
}
