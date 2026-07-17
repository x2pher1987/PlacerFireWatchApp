package com.placer.firewatch.responder

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.placer.firewatch.R
import com.placer.firewatch.databinding.ActivityResponderLoginBinding

class ResponderLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResponderLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResponderLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    override fun onStart() {
        super.onStart()
        // Anonymous sessions (from the one-tap reporter flow) don't count as
        // a responder being logged in — only a real email/password account does.
        val currentUser = auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            openDashboard()
        }
    }

    private fun attemptLogin() {
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.responder_login_missing_fields))
            return
        }

        binding.textError.visibility = View.GONE
        binding.btnLogin.isEnabled = false
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { openDashboard() }
            .addOnFailureListener { e ->
                binding.btnLogin.isEnabled = true
                showError(e.localizedMessage ?: getString(R.string.responder_login_failed))
            }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun openDashboard() {
        startActivity(Intent(this, ResponderDashboardActivity::class.java))
        finish()
    }
}
