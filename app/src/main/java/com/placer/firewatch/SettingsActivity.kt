package com.placer.firewatch

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.placer.firewatch.alert.AlertSender
import com.placer.firewatch.barangay.Barangays
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

        val barangayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Barangays.ALL)
        binding.spinnerBarangay.adapter = barangayAdapter
        val currentBarangay = Prefs.getLocationLabel(this)
        val currentIndex = Barangays.ALL.indexOf(currentBarangay).takeIf { it >= 0 } ?: 0
        binding.spinnerBarangay.setSelection(currentIndex)

        binding.seekSensitivity.progress = Prefs.getSensitivity(this)

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnTestAlert.setOnClickListener { sendTestAlert() }
    }

    private fun selectedBarangay(): String = binding.spinnerBarangay.selectedItem.toString()

    private fun saveSettings() {
        Prefs.setBfpNumbers(this, binding.editBfpNumbers.text.toString())
        Prefs.setLocationLabel(this, selectedBarangay())
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
                "app can reach this number. Location: " + selectedBarangay()
            sender.sendSms(numbers, message)
        }
    }
}
