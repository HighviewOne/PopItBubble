package com.popitbubble

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.popitbubble.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Edge-to-edge is enforced on Android 15 at targetSdk 35; keep content clear of the bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
        }

        Prefs.load(this)

        binding.switchSound.isChecked  = Prefs.soundEnabled
        binding.switchHaptic.isChecked = Prefs.hapticEnabled

        binding.switchSound.setOnCheckedChangeListener { _, checked ->
            Prefs.soundEnabled = checked
            Prefs.save(this)
        }
        binding.switchHaptic.setOnCheckedChangeListener { _, checked ->
            Prefs.hapticEnabled = checked
            Prefs.save(this)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
