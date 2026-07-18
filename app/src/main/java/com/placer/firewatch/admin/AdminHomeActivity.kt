package com.placer.firewatch.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.placer.firewatch.LandingActivity
import com.placer.firewatch.databinding.ActivityAdminHomeBinding

/**
 * Placeholder. Full admin capabilities (approve responders/citizens,
 * reject applications, suspend users, manage reports, analytics, manage
 * barangays, export reports, configure app settings) are a later feature
 * — this exists purely so an admin-role account has somewhere to land
 * after sign-in instead of a dead end.
 */
class AdminHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }
    }
}
