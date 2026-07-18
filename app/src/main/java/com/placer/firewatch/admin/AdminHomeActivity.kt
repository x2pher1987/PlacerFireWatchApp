package com.placer.firewatch.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.placer.firewatch.LandingActivity
import com.placer.firewatch.R
import com.placer.firewatch.databinding.ActivityAdminHomeBinding
import com.placer.firewatch.responder.apply.ResponderApplication
import kotlinx.coroutines.launch

/**
 * Approve/reject responder applications. Full admin capabilities beyond
 * that (manage reports, analytics, manage barangays, export reports,
 * configure app settings) are a later feature — see
 * admin_placeholder_message.
 */
class AdminHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminHomeBinding
    private val repository = AdminApplicationRepository()
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var adapter: ResponderApplicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }

    override fun onStart() {
        super.onStart()
        listenerRegistration = repository.listenForPendingApplications(
            onUpdate = { applications ->
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
}
