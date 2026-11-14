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
import androidx.annotation.VisibleForTesting
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
    // Run time banked while the app is in the background; the clock only counts while it's on screen.
    private var pausedElapsedMs = -1L
    private var isOnScreen = false
    private var celebrationAnim: AnimatorSet? = null
    private val celebrationResetRunnable = Runnable { resetGame() }
    private var optionsMenu: Menu? = null

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
            val popped = binding.bubbleGridView.getPoppedCount()
            updateCounter(popped, total)
            updateBestTimeLabel()
            // A rebuilt empty grid can't have a run in progress.
            if (popped == 0 && challengeStarted) resetClock()
            resumeClockIfNeeded()
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
                // Recorded now, not in the delayed celebration, so leaving the app can't lose it.
                checkBestTime(finalElapsedMs)
            }
        }

        binding.bubbleGridView.onAllPoppedListener = {
            if (challengeMode && challengeStarted) {
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

    /** True while a timed run is under way (started, not yet cleared). */
    private fun runInProgress(): Boolean {
        val grid = binding.bubbleGridView
        return challengeMode && challengeStarted && grid.getPoppedCount() < grid.getTotalCount()
    }

    override fun onPause() {
        super.onPause()
        isOnScreen = false
        if (runInProgress() && pausedElapsedMs < 0) {
            pausedElapsedMs = SystemClock.elapsedRealtime() - binding.chronometer.base
            binding.chronometer.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        isOnScreen = true
        resumeClockIfNeeded()
    }

    /**
     * Restarts a paused run's clock once the app is on screen and the grid is
     * built. After recreation the grid is only built at first layout, after
     * onResume, so onGridChangedListener calls this too.
     */
    private fun resumeClockIfNeeded() {
        if (pausedElapsedMs < 0 || !isOnScreen || binding.bubbleGridView.getTotalCount() == 0) return
        if (runInProgress()) {
            binding.chronometer.base = SystemClock.elapsedRealtime() - pausedElapsedMs
            binding.chronometer.start()
        }
        pausedElapsedMs = -1L
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CHALLENGE_MODE, challengeMode)
        // The grid saves its own popped bubbles; carry an unfinished run's clock with it.
        if (runInProgress()) {
            val elapsed = if (pausedElapsedMs >= 0) pausedElapsedMs
                else SystemClock.elapsedRealtime() - binding.chronometer.base
            outState.putLong(KEY_RUN_ELAPSED, elapsed)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        challengeMode = savedInstanceState.getBoolean(KEY_CHALLENGE_MODE, false)
        binding.challengeBar.visibility = if (challengeMode) View.VISIBLE else View.GONE
        val elapsed = savedInstanceState.getLong(KEY_RUN_ELAPSED, -1L)
        if (challengeMode && elapsed >= 0) {
            // onResume restarts the clock from here (if the grid came back unfinished).
            challengeStarted = true
            pausedElapsedMs = elapsed
            binding.chronometer.base = SystemClock.elapsedRealtime() - elapsed
        }
        invalidateOptionsMenu()
    }

    private fun updateCounter(popped: Int, total: Int) {
        binding.tvCounter.text = getString(R.string.counter_format, popped, total)
    }

    private fun toggleChallengeMode() {
        challengeMode = !challengeMode
        binding.challengeBar.visibility = if (challengeMode) View.VISIBLE else View.GONE
        if (!challengeMode) binding.chronometer.stop()
        invalidateOptionsMenu()
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

    private fun resetClock() {
        binding.chronometer.stop()
        binding.chronometer.base = SystemClock.elapsedRealtime()
        challengeStarted = false
        pausedElapsedMs = -1L
    }

    private fun resetGame() {
        cancelCelebration()
        binding.tvAllPopped.visibility = View.GONE
        resetClock()
        binding.bubbleGridView.reset()
        updateCounter(0, binding.bubbleGridView.getTotalCount())
    }

    private fun setGridSize(size: Int) {
        // A new grid is a new game: also resets the challenge clock.
        resetGame()
        // Saved before the grid rebuilds so onGridChangedListener shows this size's best time.
        Prefs.gridSize = size
        binding.bubbleGridView.setGridSize(size, size)
        invalidateOptionsMenu()
    }

    private fun applyColorTheme(theme: Theme) {
        // Recolouring keeps popped bubbles, so the run (and clock) carry on.
        binding.bubbleGridView.theme = theme
        updateCounter(binding.bubbleGridView.getPoppedCount(), binding.bubbleGridView.getTotalCount())
        Prefs.colorTheme = theme
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        optionsMenu = menu
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_challenge)?.isChecked = challengeMode
        // findItem also searches the Grid Size and Color Theme submenus.
        gridSizeMenuIds[Prefs.gridSize]?.let { menu.findItem(it)?.isChecked = true }
        themeMenuIds[Prefs.colorTheme]?.let { menu.findItem(it)?.isChecked = true }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset    -> { resetGame(); true }
            R.id.menu_challenge -> { toggleChallengeMode(); true }
            R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            else -> {
                gridSizeMenuIds.entries.firstOrNull { it.value == item.itemId }?.let {
                    setGridSize(it.key); return true
                }
                themeMenuIds.entries.firstOrNull { it.value == item.itemId }?.let {
                    applyColorTheme(it.key); return true
                }
                super.onOptionsItemSelected(item)
            }
        }
    }

    @VisibleForTesting
    internal fun isChallengeMenuCheckedForTest(): Boolean =
        optionsMenu?.findItem(R.id.menu_challenge)?.isChecked == true

    @VisibleForTesting
    internal fun isMenuItemCheckedForTest(id: Int): Boolean =
        optionsMenu?.findItem(id)?.isChecked == true

    override fun onDestroy() {
        super.onDestroy()
        cancelCelebration()
        soundManager.release()
    }

    private companion object {
        const val KEY_CHALLENGE_MODE = "challengeMode"
        const val KEY_RUN_ELAPSED = "runElapsedMs"

        val gridSizeMenuIds = mapOf(
            4 to R.id.menu_4x4, 5 to R.id.menu_5x5, 6 to R.id.menu_6x6, 7 to R.id.menu_7x7
        )
        val themeMenuIds = mapOf(
            Theme.RAINBOW to R.id.menu_theme_rainbow,
            Theme.PINK to R.id.menu_theme_pink,
            Theme.BLUE to R.id.menu_theme_blue,
            Theme.PASTEL to R.id.menu_theme_pastel,
            Theme.NEON to R.id.menu_theme_neon,
            Theme.CANDY to R.id.menu_theme_candy
        )
    }
}
