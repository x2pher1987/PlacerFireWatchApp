package com.placer.firewatch.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.placer.firewatch.LandingActivity
import com.placer.firewatch.R
import com.placer.firewatch.databinding.ActivityAdminHomeBinding
import com.placer.firewatch.report.export.DocxWriter
import com.placer.firewatch.report.export.ReportExportRepository
import com.placer.firewatch.report.export.XlsxWriter
import com.placer.firewatch.responder.apply.ResponderApplication
import com.placer.firewatch.settings.AppSettingsRepository
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Approve/reject responder applications. Full admin capabilities beyond
 * that (manage reports, analytics, manage barangays, export reports,
 * configure app settings) are a later feature — see
 * admin_placeholder_message.
 */
class AdminHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminHomeBinding
    private val repository = AdminApplicationRepository()
    private val settingsRepository = AppSettingsRepository()
    private val exportRepository = ReportExportRepository()
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var adapter: ResponderApplicationAdapter
    private var hasPlayedEntranceAnimation = false

    private val createXlsxLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> uri?.let { writeXlsxTo(it) } }

    private val createDocxLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri -> uri?.let { writeDocxTo(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val exportFilenameStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(java.util.Date())
        binding.btnExportXlsx.setOnClickListener {
            createXlsxLauncher.launch("PlacerFireWatch_Reports_$exportFilenameStamp.xlsx")
        }
        binding.btnExportDocx.setOnClickListener {
            createDocxLauncher.launch("PlacerFireWatch_Reports_$exportFilenameStamp.docx")
        }

        adapter = ResponderApplicationAdapter(
            onApprove = { application -> updateApplication(application.uid) { repository.approve(it) } },
            onReject = { application -> updateApplication(application.uid) { repository.reject(it) } }
        )
        binding.recyclerApplications.layoutManager = LinearLayoutManager(this)
        binding.recyclerApplications.adapter = adapter

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }

        binding.btnSaveBfpNumber.setOnClickListener { saveBfpNumber() }
        lifecycleScope.launch {
            binding.editBfpNumber.setText(settingsRepository.getBfpNumber().orEmpty())
        }
    }

    private fun saveBfpNumber() {
        val number = binding.editBfpNumber.text?.toString()?.trim().orEmpty()
        if (number.isEmpty()) return
        lifecycleScope.launch {
            val result = settingsRepository.setBfpNumber(number)
            val messageRes = if (result.isSuccess) R.string.admin_bfp_number_saved else R.string.admin_action_failed
            Toast.makeText(this@AdminHomeActivity, messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        listenerRegistration = repository.listenForPendingApplications(
            onUpdate = { applications ->
                if (!hasPlayedEntranceAnimation && applications.isNotEmpty()) {
                    binding.recyclerApplications.scheduleLayoutAnimation()
                    hasPlayedEntranceAnimation = true
                }
                adapter.submitList(applications)
                binding.textEmpty.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { e ->
                Toast.makeText(
                    this,
                    getString(R.string.admin_applications_load_failed, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun updateApplication(uid: String, action: suspend (String) -> Result<Unit>) {
        lifecycleScope.launch {
            val result = action(uid)
            if (result.isFailure) {
                Toast.makeText(this@AdminHomeActivity, R.string.admin_action_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeXlsxTo(uri: Uri) = exportTo(uri) { out, rows ->
        XlsxWriter.write(out, "Reports", ReportExportRepository.HEADERS, rows)
    }

    private fun writeDocxTo(uri: Uri) = exportTo(uri) { out, rows ->
        DocxWriter.write(out, getString(R.string.admin_home_title) + " — Reports", ReportExportRepository.HEADERS, rows)
    }

    private fun exportTo(uri: Uri, write: (java.io.OutputStream, List<List<String>>) -> Unit) {
        Toast.makeText(this, R.string.admin_export_in_progress, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = exportRepository.fetchAllAsRows()
            val rows = result.getOrNull()
            if (result.isFailure || rows == null) {
                Toast.makeText(
                    this@AdminHomeActivity,
                    getString(R.string.admin_export_failed, result.exceptionOrNull()?.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            if (rows.isEmpty()) {
                Toast.makeText(this@AdminHomeActivity, R.string.admin_export_no_reports, Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { out -> write(out, rows) }
                }
                Toast.makeText(this@AdminHomeActivity, R.string.admin_export_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AdminHomeActivity,
                    getString(R.string.admin_export_failed, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
