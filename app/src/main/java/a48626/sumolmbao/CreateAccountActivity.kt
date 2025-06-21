package a48626.sumolmbao

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class CreateAccountActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var createAccountBtn: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loginBtnTextView: TextView
    private lateinit var signUpIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        // Initialize all the views
        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text)
        createAccountBtn = findViewById(R.id.create_account_btn)
        progressBar = findViewById(R.id.progress_bar)
        loginBtnTextView = findViewById(R.id.login_text_view_btn)
        signUpIcon = findViewById(R.id.sign_up_icon)

        // Set the icon based on the current theme
        updateIconForTheme()

        // Set click listeners
        createAccountBtn.setOnClickListener { createAccount() }
        loginBtnTextView.setOnClickListener { finish() }
    }

    private fun updateIconForTheme() {
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferences?.getString("SelectedTheme", "RedTheme")

        val drawableResId = when (currentTheme) {
            "PurpleTheme" -> R.drawable.tatakai_purple
            "JsaTheme" -> R.drawable.tatakai_jsa
            else -> R.drawable.tatakai_red // Default case
        }
        signUpIcon.setImageResource(drawableResId)
    }

    private fun createAccount() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (!validateData(email, password, confirmPassword)) {
            return
        }
        createAccountInFirebase(email, password)
    }

    private fun createAccountInFirebase(email: String, password: String) {
        changeInProgress(true)
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                changeInProgress(false)
                if (task.isSuccessful) {
                    firebaseAuth.currentUser!!.sendEmailVerification()
                    Utility.showToast(this, "Account created. Please check your email to verify.")
                    firebaseAuth.signOut()
                    finish()
                } else {
                    try {
                        throw task.exception!!
                    } catch (e: FirebaseAuthWeakPasswordException) {
                        passwordEditText.error = "Password is too weak (at least 6 characters)."
                        passwordEditText.requestFocus()
                    } catch (e: FirebaseAuthUserCollisionException) {
                        emailEditText.error = "An account with this email already exists."
                        emailEditText.requestFocus()
                    } catch (e: Exception) {
                        Utility.showToast(this, e.localizedMessage ?: "Registration failed.")
                    }
                }
            }
    }

    private fun changeInProgress(inProgress: Boolean) {
        progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
        createAccountBtn.visibility = if (inProgress) View.GONE else View.VISIBLE
    }

    private fun validateData(email: String, password: String, confirmPassword: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Email is invalid"
            return false
        }
        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            return false
        }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            return false
        }
        return true
    }
}
