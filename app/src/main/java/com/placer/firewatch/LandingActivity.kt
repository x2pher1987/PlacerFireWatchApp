package com.placer.firewatch

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.placer.firewatch.admin.AdminHomeActivity
import com.placer.firewatch.auth.CreateAccountActivity
import com.placer.firewatch.auth.SignInActivity
import com.placer.firewatch.databinding.ActivityLandingBinding
import com.placer.firewatch.responder.DevResponderSession
import com.placer.firewatch.responder.ResponderDashboardActivity
import com.placer.firewatch.user.ResponderStatus
import com.placer.firewatch.user.UserRepository
import com.placer.firewatch.user.UserRole
import kotlinx.coroutines.launch

/**
 * App launcher and sign-in gate. Every user must Sign In or Create
 * Account before reaching any reporting/dashboard screen. Firebase Auth
 * sessions persist across app restarts by default, so an already
 * signed-in user is routed straight to their role's home screen here
 * without ever seeing the landing buttons again — only an explicit sign
 * out returns them to this screen.
 */
class LandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandingBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignIn.setOnClickListener { startActivity(Intent(this, SignInActivity::class.java)) }
        binding.btnCreateAccount.setOnClickListener { startActivity(Intent(this, CreateAccountActivity::class.java)) }
    }

    override fun onStart() {
        super.onStart()

        // ⚠ DEVELOPMENT ONLY — see DevResponderSession.
        if (BuildConfig.DEBUG && DevResponderSession.isActive(this)) {
            openResponderDashboard()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch { routeSignedInUser(uid) }
    }

    private suspend fun routeSignedInUser(uid: String) {
        val appUser = userRepository.fetchUser(uid)
        when {
            appUser?.role == UserRole.ADMIN -> openAdminHome()
            appUser?.role == UserRole.RESPONDER && appUser.responderStatus == ResponderStatus.APPROVED ->
                openResponderDashboard()
            else -> openCitizenHome()
        }
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
