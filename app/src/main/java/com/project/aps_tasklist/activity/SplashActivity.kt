package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.aps_tasklist.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    // Daftar gambar dan durasi tampilnya (ms)
    private val splashItems = listOf(
        SplashItem(R.drawable.ic_logo2, 3_500L),
        SplashItem(R.drawable.ic_info1, 3_500L),
        SplashItem(R.drawable.ic_info2, 3_500L)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imageView = findViewById<ImageView>(R.id.ivSplash)

        // Mulai coroutine di lifecycleScope
        lifecycleScope.launch {
            for ((drawableRes, duration) in splashItems) {
                // Set gambar
                imageView.setImageResource(drawableRes)

                // Fade-in
                imageView.alpha = 0f
                imageView.animate()
                    .alpha(1f)
                    .setDuration(500L)    // 0.5 detik fade-in
                    .start()

                // Tunggu selama durasi tampil
                delay(duration)

                // Fade-out
                imageView.animate()
                    .alpha(0f)
                    .setDuration(500L)    // 0.5 detik fade-out
                    .start()

                // Pastikan animasi selesai sebelum lanjut
                delay(500L)
            }

            // Setelah semua splash selesai, pindah ke LoginActivity
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }
    }

    // Data class pembantu
    data class SplashItem(val drawableRes: Int, val duration: Long)
}
