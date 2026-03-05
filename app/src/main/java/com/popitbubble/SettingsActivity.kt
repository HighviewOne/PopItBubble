package com.popitbubble

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
        }

        Prefs.load(this)

        val switchSound  = findViewById<SwitchCompat>(R.id.switchSound)
        val switchHaptic = findViewById<SwitchCompat>(R.id.switchHaptic)

        switchSound.isChecked  = Prefs.soundEnabled
        switchHaptic.isChecked = Prefs.hapticEnabled

        switchSound.setOnCheckedChangeListener { _, checked ->
            Prefs.soundEnabled = checked
            Prefs.save(this)
        }
        switchHaptic.setOnCheckedChangeListener { _, checked ->
            Prefs.hapticEnabled = checked
            Prefs.save(this)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
