package com.placer.firewatch.responder.apply

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.placer.firewatch.R
import com.placer.firewatch.barangay.Barangays
import com.placer.firewatch.databinding.ActivityResponderApplicationBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

/** Citizen self-service application to become a responder — see Section 3 of the product spec. */
class ResponderApplicationActivity : AppCompatActivity() {

    companion object {
        private val BLOOD_TYPES = listOf("Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    }

    private lateinit var binding: ActivityResponderApplicationBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private var governmentIdUri: Uri? = null
    private var barangayCertificationUri: Uri? = null
    private var selfieUri: Uri? = null
    private var pendingSelfieUri: Uri? = null

    private val pickGovernmentId = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            governmentIdUri = uri
            binding.imageGovernmentId.apply { setImageURI(uri); visibility = View.VISIBLE }
        }
    }

    private val pickBarangayCertification = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            barangayCertificationUri = uri
            binding.imageBarangayCertification.apply { setImageURI(uri); visibility = View.VISIBLE }
        }
    }

    private val takeSelfie = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingSelfieUri
        if (success && uri != null) {
            selfieUri = uri
            binding.imageSelfie.apply { setImageURI(uri); visibility = View.VISIBLE }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResponderApplicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editEmail.setText(FirebaseAuth.getInstance().currentUser?.email.orEmpty())

        binding.spinnerBarangay.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Barangays.ALL)
        binding.spinnerBloodType.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, BLOOD_TYPES)

        binding.editBirthdate.setOnClickListener { showDatePicker() }
        binding.btnGovernmentId.setOnClickListener { pickGovernmentId.launch("image/*") }
        binding.btnBarangayCertification.setOnClickListener { pickBarangayCertification.launch("image/*") }
        binding.btnSelfie.setOnClickListener {
            val uri = createSelfieUri()
            pendingSelfieUri = uri
            takeSelfie.launch(uri)
        }
        binding.btnSubmitApplication.setOnClickListener { attemptSubmit() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                binding.editBirthdate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR) - 25,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun createSelfieUri(): Uri {
        val dir = File(cacheDir, "responder_application_photos").apply { mkdirs() }
        val file = File(dir, "selfie_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun attemptSubmit() {
        val fullName = binding.editFullName.text?.toString()?.trim().orEmpty()
        val birthdate = binding.editBirthdate.text?.toString()?.trim().orEmpty()
        val address = binding.editAddress.text?.toString()?.trim().orEmpty()
        val barangay = binding.spinnerBarangay.selectedItem?.toString().orEmpty()
        val contactNumber = binding.editContactNumber.text?.toString()?.trim().orEmpty()
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val emergencyContact = binding.editEmergencyContact.text?.toString()?.trim().orEmpty()
        val bloodType = binding.spinnerBloodType.selectedItem?.toString().orEmpty()
        val occupation = binding.editOccupation.text?.toString()?.trim().orEmpty()
        val reason = binding.editReason.text?.toString()?.trim().orEmpty()

        val govId = governmentIdUri
        val cert = barangayCertificationUri
        val selfie = selfieUri

        if (fullName.isEmpty() || birthdate.isEmpty() || address.isEmpty() || contactNumber.isEmpty() ||
            emergencyContact.isEmpty() || occupation.isEmpty() || reason.isEmpty()
        ) {
            showError(getString(R.string.auth_missing_fields))
            return
        }
        if (govId == null || cert == null || selfie == null) {
            showError(getString(R.string.apply_missing_documents))
            return
        }

        binding.textError.visibility = View.GONE
        binding.btnSubmitApplication.isEnabled = false
        Toast.makeText(this, R.string.apply_submitting, Toast.LENGTH_SHORT).show()

        val draft = ResponderApplicationDraft(
            fullName = fullName,
            birthdate = birthdate,
            address = address,
            barangay = barangay,
            contactNumber = contactNumber,
            email = email,
            governmentIdUri = govId,
            barangayCertificationUri = cert,
            selfieUri = selfie,
            emergencyContact = emergencyContact,
            bloodType = bloodType,
            occupation = occupation,
            reason = reason
        )

        lifecycleScope.launch {
            val result = ResponderApplicationRepository().submit(draft)
            binding.btnSubmitApplication.isEnabled = true
            if (result.isSuccess) {
                Toast.makeText(this@ResponderApplicationActivity, R.string.apply_submitted_success, Toast.LENGTH_LONG).show()
                finish()
            } else {
                showError(getString(R.string.apply_submitted_failure))
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }
}
