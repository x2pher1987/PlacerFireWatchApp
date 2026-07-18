package com.placer.firewatch.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.placer.firewatch.MainActivity
import com.placer.firewatch.R
import com.placer.firewatch.databinding.ActivityCreateAccountBinding
import com.placer.firewatch.user.UserRepository
import kotlinx.coroutines.launch

/** Self-registration is citizen-only — see Section 2/3 of the product spec. */
class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAccountBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreateAccount.setOnClickListener { attemptCreateAccount() }
    }

    private fun attemptCreateAccount() {
        val fullName = binding.editFullName.text?.toString()?.trim().orEmpty()
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()
        val confirmPassword = binding.editConfirmPassword.text?.toString().orEmpty()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.auth_missing_fields))
            return
        }
        if (password != confirmPassword) {
            showError(getString(R.string.auth_passwords_dont_match))
            return
        }
        if (password.length < 6) {
            showError(getString(R.string.auth_password_too_short))
            return
        }

        binding.textError.visibility = View.GONE
        binding.btnCreateAccount.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    binding.btnCreateAccount.isEnabled = true
                    showError(getString(R.string.auth_create_account_failed))
                } else {
                    finishRegistration(uid, fullName, email)
                }
            }
            .addOnFailureListener { e ->
                binding.btnCreateAccount.isEnabled = true
                showError(e.localizedMessage ?: getString(R.string.auth_create_account_failed))
            }
    }

    private fun finishRegistration(uid: String, fullName: String, email: String) {
        lifecycleScope.launch {
            try {
                userRepository.createCitizen(uid, fullName, email)
                openCitizenHome()
            } catch (e: Exception) {
                binding.btnCreateAccount.isEnabled = true
                showError(e.localizedMessage ?: getString(R.string.auth_create_account_failed))
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
}
