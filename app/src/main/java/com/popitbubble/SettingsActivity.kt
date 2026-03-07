package com.popitbubble

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.popitbubble.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
