package a48626.sumolmbao

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // You can create a simple layout for this if you want, or just have it blank.
        // For now, it will just be a blank screen during the check.

        // Use a short delay to avoid a screen flicker
        Handler(Looper.getMainLooper()).postDelayed({
            // After the delay, start the MainActivity and finish the SplashActivity
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }, 500) // 0.5 second delay
    }
}
