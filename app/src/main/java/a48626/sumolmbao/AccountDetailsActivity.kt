package a48626.sumolmbao

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class AccountDetailsActivity : AppCompatActivity() {

    private lateinit var userEmailTextView: TextView
    private lateinit var logoutBtn: TextView
    private lateinit var accountIcon: ImageView

    // Views for the new password change form
    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmNewPasswordEditText: EditText
    private lateinit var confirmChangesBtn: TextView

    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_details)

        userEmailTextView = findViewById(R.id.user_email_textview)
        logoutBtn = findViewById(R.id.logout_btn)
        accountIcon = findViewById(R.id.account_icon)

        // Initialize new views
        oldPasswordEditText = findViewById(R.id.old_password_edit_text)
        newPasswordEditText = findViewById(R.id.new_password_edit_text)
        confirmNewPasswordEditText = findViewById(R.id.confirm_new_password_edit_text)
        confirmChangesBtn = findViewById(R.id.confirm_changes_btn)

        updateIconForTheme()

        val currentUser = firebaseAuth.currentUser
        userEmailTextView.text = currentUser?.email ?: "No email found"

        // --- Logout Logic ---
        logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            Utility.showToast(this, "You have successfully logged out.")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // --- Password Change Logic ---
        confirmChangesBtn.setOnClickListener {
            handleChangePassword()
        }
    }

    private fun handleChangePassword() {
        val oldPassword = oldPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()
        val confirmPassword = confirmNewPasswordEditText.text.toString()

        // --- Validation ---
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Utility.showToast(this, "Please fill all password fields.")
            return
        }
        if (newPassword.length < 6) {
            newPasswordEditText.error = "New password must be at least 6 characters."
            return
        }
        if (newPassword != confirmPassword) {
            confirmNewPasswordEditText.error = "Passwords do not match."
            return
        }
        // --- End Validation ---

        val user = firebaseAuth.currentUser ?: return

        // Create credential with old password to re-authenticate
        val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)

        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                // Re-authentication successful, now update the password
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Utility.showToast(this, "Password updated successfully.")
                        // Clear fields after success
                        oldPasswordEditText.text.clear()
                        newPasswordEditText.text.clear()
                        confirmNewPasswordEditText.text.clear()
                    } else {
                        Utility.showToast(this, "Error: ${updateTask.exception?.localizedMessage}")
                    }
                }
            } else {
                // Re-authentication failed
                oldPasswordEditText.error = "Incorrect old password."
                Utility.showToast(this, "Error: ${reauthTask.exception?.localizedMessage}")
            }
        }
    }

    private fun updateIconForTheme() {
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferences.getString("SelectedTheme", "RedTheme")
        val drawableResId = when (currentTheme) {
            "PurpleTheme" -> R.drawable.tatakai_purple
            "JsaTheme" -> R.drawable.tatakai_jsa
            else -> R.drawable.tatakai_red
        }
        accountIcon.setImageResource(drawableResId)
    }
}
