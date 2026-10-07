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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.popitbubble.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var soundManager: SoundManager

    // Challenge mode
    private var challengeMode = false
    private var challengeStarted = false
    private var finalElapsedMs = 0L
    private var celebrationAnim: AnimatorSet? = null
    private val celebrationResetRunnable = Runnable { resetGame() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        setSupportActionBar(binding.toolbar)

        Prefs.init(this)

        soundManager = SoundManager(this)
        binding.bubbleGridView.soundManager = soundManager

        // The grid is only built once the view has been measured, so the
        // counter total is set from this callback rather than in onCreate.
        binding.bubbleGridView.onGridChangedListener = { total ->
            updateCounter(binding.bubbleGridView.getPoppedCount(), total)
            updateBestTimeLabel()
        }

        // Restore saved grid size and theme
        binding.bubbleGridView.setGridSize(Prefs.gridSize, Prefs.gridSize)
        binding.bubbleGridView.theme = Prefs.colorTheme

        binding.bubbleGridView.onPopListener = { popped, total ->
            updateCounter(popped, total)
            if (challengeMode && !challengeStarted && popped == 1) {
                challengeStarted = true
                binding.chronometer.base = SystemClock.elapsedRealtime()
                binding.chronometer.start()
            }
            // Stop the clock on the last pop itself, not when the delayed
            // celebration callback fires.
            if (challengeMode && challengeStarted && popped == total) {
                binding.chronometer.stop()
                finalElapsedMs = SystemClock.elapsedRealtime() - binding.chronometer.base
            }
        }

        binding.bubbleGridView.onAllPoppedListener = {
            if (challengeMode && challengeStarted) {
                checkBestTime(finalElapsedMs)
                showAllPoppedCelebration(formatTime(finalElapsedMs))
            } else {
                showAllPoppedCelebration(null)
            }
        }

        binding.fabReset.setOnClickListener { resetGame() }

        updateBestTimeLabel()
    }

    /**
     * Pads the root by the system bar insets. With targetSdk 35, Android 15
     * draws apps edge-to-edge, so without this the toolbar sits under the
     * status bar and the FAB under the navigation bar.
     */
    private fun applySystemBarInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("challengeMode", challengeMode)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // The grid and chronometer always start fresh after recreation, so
        // only the mode is restored; a run in progress is not.
        challengeMode = savedInstanceState.getBoolean("challengeMode", false)
        binding.challengeBar.visibility = if (challengeMode) View.VISIBLE else View.GONE
    }

    private fun updateCounter(popped: Int, total: Int) {
        binding.tvCounter.text = getString(R.string.counter_format, popped, total)
    }

    private fun toggleChallengeMode() {
        challengeMode = !challengeMode
        binding.challengeBar.visibility = if (challengeMode) View.VISIBLE else View.GONE
        if (!challengeMode) binding.chronometer.stop()
        resetGame()
    }

    private fun checkBestTime(elapsedMs: Long) {
        val size = Prefs.gridSize
        val best = Prefs.bestTimeMs(size)
        if (best == 0L || elapsedMs < best) Prefs.setBestTimeMs(size, elapsedMs)
        updateBestTimeLabel()
    }

    /** Shows the best time for the current grid size; each size keeps its own record. */
    private fun updateBestTimeLabel() {
        val best = Prefs.bestTimeMs(Prefs.gridSize)
        binding.tvBestTime.text = if (best > 0L) getString(R.string.best_time_format, formatTime(best)) else ""
    }

    private fun formatTime(ms: Long): String {
        val s = ms / 1000
        val tenths = (ms % 1000) / 100
        return getString(R.string.time_seconds_format, s, tenths)
    }

    private fun showAllPoppedCelebration(timeStr: String?) {
        binding.tvAllPopped.text = if (timeStr != null) getString(R.string.all_popped_time_format, timeStr)
            else getString(R.string.all_popped)
        binding.tvAllPopped.visibility = View.VISIBLE
        binding.tvAllPopped.alpha  = 0f
        binding.tvAllPopped.scaleX = 0.5f
        binding.tvAllPopped.scaleY = 0.5f

        val fadeIn = ObjectAnimator.ofFloat(binding.tvAllPopped, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(binding.tvAllPopped, "scaleX", 0.5f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.tvAllPopped, "scaleY", 0.5f, 1.1f, 1f)
        scaleX.interpolator = BounceInterpolator()
        scaleY.interpolator = BounceInterpolator()

        celebrationAnim = AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            duration = 600
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.tvAllPopped.postDelayed(celebrationResetRunnable, 1500)
                }
            })
            start()
        }
    }

    private fun cancelCelebration() {
        celebrationAnim?.run { removeAllListeners(); cancel() }
        celebrationAnim = null
        binding.tvAllPopped.removeCallbacks(celebrationResetRunnable)
    }

    private fun resetGame() {
        cancelCelebration()
        binding.tvAllPopped.visibility = View.GONE
        binding.chronometer.stop()
        binding.chronometer.base = SystemClock.elapsedRealtime()
        challengeStarted = false
        binding.bubbleGridView.reset()
        updateCounter(0, binding.bubbleGridView.getTotalCount())
    }

    private fun setGridSize(size: Int) {
        // A new grid is a new game: also resets the challenge clock.
        resetGame()
        // Saved before the grid rebuilds so onGridChangedListener shows this size's best time.
        Prefs.gridSize = size
        binding.bubbleGridView.setGridSize(size, size)
    }

    private fun applyColorTheme(theme: Theme) {
        // Recolouring keeps popped bubbles, so the run (and clock) carry on.
        binding.bubbleGridView.theme = theme
        updateCounter(binding.bubbleGridView.getPoppedCount(), binding.bubbleGridView.getTotalCount())
        Prefs.colorTheme = theme
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
            R.id.menu_theme_rainbow -> { applyColorTheme(Theme.RAINBOW); true }
            R.id.menu_theme_pink    -> { applyColorTheme(Theme.PINK);    true }
            R.id.menu_theme_blue    -> { applyColorTheme(Theme.BLUE);    true }
            R.id.menu_theme_pastel  -> { applyColorTheme(Theme.PASTEL);  true }
            R.id.menu_theme_neon    -> { applyColorTheme(Theme.NEON);    true }
            R.id.menu_theme_candy   -> { applyColorTheme(Theme.CANDY);   true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCelebration()
        soundManager.release()
    }
}
