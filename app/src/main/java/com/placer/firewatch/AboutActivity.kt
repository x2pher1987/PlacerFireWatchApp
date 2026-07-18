package com.placer.firewatch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.placer.firewatch.databinding.ActivityAboutBinding

/** Static content — see Section 13 of the product spec. */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
