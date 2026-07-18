package com.placer.firewatch.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.placer.firewatch.BuildConfig
import com.placer.firewatch.MainActivity
import com.placer.firewatch.R
import com.placer.firewatch.admin.AdminHomeActivity
import com.placer.firewatch.databinding.ActivitySignInBinding
import com.placer.firewatch.responder.DevResponderSession
import com.placer.firewatch.responder.ResponderDashboardActivity
import com.placer.firewatch.user.ResponderStatus
import com.placer.firewatch.user.UserRepository
import com.placer.firewatch.user.UserRole
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignIn.setOnClickListener { attemptSignIn() }

        // ⚠ DEVELOPMENT ONLY — see DevResponderSession. Never shown in a
        // release build: BuildConfig.DEBUG is false there.
        if (BuildConfig.DEBUG) {
            binding.textDevOnlyLabel.visibility = View.VISIBLE
            binding.btnDevLogin.visibility = View.VISIBLE
            binding.btnDevLogin.setOnClickListener {
                DevResponderSession.start(this)
                openResponderDashboard()
            }
        }
    }

    private fun attemptSignIn() {
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.auth_missing_fields))
            return
        }

        binding.textError.visibility = View.GONE
        binding.btnSignIn.isEnabled = false
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    binding.btnSignIn.isEnabled = true
                    showError(getString(R.string.auth_sign_in_failed))
                } else {
                    routeAfterSignIn(uid)
                }
            }
            .addOnFailureListener { e ->
                binding.btnSignIn.isEnabled = true
                showError(e.localizedMessage ?: getString(R.string.auth_sign_in_failed))
            }
    }

    private fun routeAfterSignIn(uid: String) {
        lifecycleScope.launch {
            val appUser = userRepository.fetchUser(uid)
            when {
                appUser?.role == UserRole.ADMIN -> openAdminHome()
                appUser?.role == UserRole.RESPONDER && appUser.responderStatus == ResponderStatus.APPROVED ->
                    openResponderDashboard()
                else -> openCitizenHome()
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun openCitizenHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openResponderDashboard() {
        startActivity(Intent(this, ResponderDashboardActivity::class.java))
        finish()
    }

    private fun openAdminHome() {
        startActivity(Intent(this, AdminHomeActivity::class.java))
        finish()
    }
}
