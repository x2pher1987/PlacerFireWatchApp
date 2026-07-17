package com.placer.firewatch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.placer.firewatch.alert.AlertSender
import com.placer.firewatch.databinding.ActivitySettingsBinding
import com.placer.firewatch.location.LocationProvider
import com.placer.firewatch.util.Prefs
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editBfpNumbers.setText(Prefs.getBfpNumbers(this).joinToString(", "))
        binding.editLocationLabel.setText(Prefs.getLocationLabel(this))
        binding.seekSensitivity.progress = Prefs.getSensitivity(this)

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnTestAlert.setOnClickListener { sendTestAlert() }
    }

    private fun saveSettings() {
        Prefs.setBfpNumbers(this, binding.editBfpNumbers.text.toString())
        Prefs.setLocationLabel(this, binding.editLocationLabel.text.toString())
        Prefs.setSensitivity(this, binding.seekSensitivity.progress)
        finish()
    }

    private fun sendTestAlert() {
        lifecycleScope.launch {
            LocationProvider(this@SettingsActivity).getLastKnownLocation()
            val sender = AlertSender(this@SettingsActivity)
            val numbers = binding.editBfpNumbers.text.toString()
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val message = "PLACER FIREWATCH TEST ALERT — please ignore. This confirms the " +
                "app can reach this number. Location: " +
                binding.editLocationLabel.text.toString().ifBlank { Prefs.getLocationLabel(this@SettingsActivity) }
            sender.sendSms(numbers, message)
        }
    }
}
