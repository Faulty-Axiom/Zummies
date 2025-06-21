package a48626.sumolmbao

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginBtn: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var createAccountBtnTextView: TextView
    private lateinit var loginIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize all the views
        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        loginBtn = findViewById(R.id.login_btn)
        progressBar = findViewById(R.id.progress_bar)
        createAccountBtnTextView = findViewById(R.id.create_account_text_view_btn)
        loginIcon = findViewById(R.id.login_icon)

        // Set the icon based on the current theme
        updateIconForTheme()

        // Set click listeners
        loginBtn.setOnClickListener { loginUser() }
        createAccountBtnTextView.setOnClickListener {
            startActivity(Intent(this@LoginActivity, CreateAccountActivity::class.java))
        }
    }

    private fun updateIconForTheme() {
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferences?.getString("SelectedTheme", "RedTheme")

        val drawableResId = when (currentTheme) {
            "PurpleTheme" -> R.drawable.tatakai_purple
            "JsaTheme" -> R.drawable.tatakai_jsa
            else -> R.drawable.tatakai_red // Default case
        }
        loginIcon.setImageResource(drawableResId)
    }

    private fun loginUser() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (!validateData(email, password)) {
            return
        }
        loginAccountInFirebase(email, password)
    }

    private fun loginAccountInFirebase(email: String, password: String) {
        changeInProgress(true)
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                changeInProgress(false)
                if (task.isSuccessful) {
                    if (firebaseAuth.currentUser!!.isEmailVerified) {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Utility.showToast(this, "Email not verified. Please check your inbox.")
                    }
                } else {
                    Utility.showToast(this, task.exception?.localizedMessage ?: "Login failed.")
                }
            }
    }

    private fun changeInProgress(inProgress: Boolean) {
        progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
        loginBtn.visibility = if (inProgress) View.GONE else View.VISIBLE
    }

    private fun validateData(email: String, password: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Email is invalid"
            return false
        }
        if (password.length < 6) {
            passwordEditText.error = "Password length is invalid"
            return false
        }
        return true
    }
}
