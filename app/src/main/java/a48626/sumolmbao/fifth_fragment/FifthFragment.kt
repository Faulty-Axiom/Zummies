package a48626.sumolmbao.fifth_fragment

import a48626.sumolmbao.AccountDetailsActivity
import a48626.sumolmbao.LoginActivity
import a48626.sumolmbao.R
import a48626.sumolmbao.Utility
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FifthFragment : Fragment() {

    // --- Views & Adapters---
    private lateinit var themeButton: TextView
    private lateinit var accountButton: TextView
    private lateinit var themeRecyclerView: RecyclerView
    private lateinit var overlay: View
    private lateinit var glossarySearchEditText: EditText
    private lateinit var glossaryRecyclerView: RecyclerView
    private lateinit var clearSearchButton: ImageView
    private lateinit var selectedTermContainer: CardView
    private lateinit var selectedTermText: TextView
    private lateinit var selectedDefinitionText: TextView
    private lateinit var tatakaiImage: ImageView
    private lateinit var themeAdapter: ThemeAdapter
    private lateinit var glossaryAdapter: GlossaryAdapter
    private val glossaryItems = mutableListOf<GlossaryItem>()

    // --- Firebase Instances ---
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fifth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        updateViewsForTheme()
        setupThemeSelector()
        setupGlossary()

        accountButton.setOnClickListener {
            if (firebaseAuth.currentUser == null) {
                startActivity(Intent(activity, LoginActivity::class.java))
            } else {
                startActivity(Intent(activity, AccountDetailsActivity::class.java))
            }
        }
    }

    private fun initializeViews(view: View) {
        themeButton = view.findViewById(R.id.themeButton)
        accountButton = view.findViewById(R.id.account_button)
        themeRecyclerView = view.findViewById(R.id.themeRecyclerView)
        overlay = view.findViewById(R.id.overlay)
        glossarySearchEditText = view.findViewById(R.id.glossarySearchEditText)
        glossaryRecyclerView = view.findViewById(R.id.glossaryRecyclerView)
        clearSearchButton = view.findViewById(R.id.clear_search_button)
        selectedTermContainer = view.findViewById(R.id.selected_term_container)
        selectedTermText = view.findViewById(R.id.selected_term_text)
        selectedDefinitionText = view.findViewById(R.id.selected_definition_text)
        tatakaiImage = view.findViewById(R.id.tatakai_image)
    }

    private fun setupThemeSelector() {
        setupThemeRecyclerView()

        themeButton.setOnClickListener {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Utility.showToast(requireContext(), getString(R.string.login_required_toast))
                return@setOnClickListener
            }

            // --- FIREBASE PAYWALL LOGIC ---
            val userDocRef = db.collection("users").document(currentUser.uid)
            userDocRef.get().addOnSuccessListener { document ->
                val hasPaid = document.getBoolean("hasPaidForThemes") ?: false
                if (hasPaid) {
                    animateButtonClick(it)
                    if (themeRecyclerView.isVisible) hideThemeSelector() else showThemeSelector()
                } else {
                    showPurchaseDialog()
                }
            }.addOnFailureListener { e ->
                Log.w("FifthFragment", "Error getting payment status", e)
                Utility.showToast(requireContext(), "Could not check payment status.")
            }
        }

        overlay.setOnClickListener { hideThemeSelector() }
    }

    private fun showPurchaseDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_purchase_themes, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelButton = dialogView.findViewById<TextView>(R.id.cancel_button)
        val payButton = dialogView.findViewById<TextView>(R.id.pay_button)

        cancelButton.setOnClickListener { dialog.dismiss() }

        payButton.setOnClickListener {
            val currentUser = firebaseAuth.currentUser ?: return@setOnClickListener

            // --- WRITE TO FIREBASE ON PAYMENT ---
            val paymentData = hashMapOf("hasPaidForThemes" to true)
            db.collection("users").document(currentUser.uid)
                .set(paymentData, SetOptions.merge())
                .addOnSuccessListener {
                    Utility.showToast(requireContext(), getString(R.string.payment_successful_toast))
                    dialog.dismiss()
                    showThemeSelector()
                }
                .addOnFailureListener { e ->
                    Log.w("FifthFragment", "Error writing payment status", e)
                    Utility.showToast(requireContext(), "Payment failed. Please try again.")
                    dialog.dismiss()
                }
        }
        dialog.show()
    }

    // ... Other fragment functions (displayDefinition, etc.) remain here ...

    private fun displayDefinition(item: GlossaryItem) {
        selectedTermText.text = item.term
        selectedDefinitionText.text = item.definition
        selectedTermContainer.visibility = View.VISIBLE
        hideKeyboardAndGlossaryList()
    }

    private fun hideKeyboardAndGlossaryList() {
        glossarySearchEditText.clearFocus()
        glossaryRecyclerView.visibility = View.GONE
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun showThemeSelector() {
        overlay.visibility = View.VISIBLE
        themeRecyclerView.visibility = View.VISIBLE
    }

    private fun hideThemeSelector() {
        overlay.visibility = View.GONE
        themeRecyclerView.visibility = View.GONE
    }

    private fun animateButtonClick(view: View) {
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up)
        view.startAnimation(animation)
    }

    private fun loadGlossaryData() {
        if (glossaryItems.isEmpty()) {
            val rawGlossary = resources.getStringArray(R.array.glossary_terms)
            rawGlossary.forEach {
                val parts = it.split("|", limit = 2)
                if (parts.size == 2) {
                    glossaryItems.add(GlossaryItem(parts[0], parts[1]))
                }
            }
        }
    }

    private fun setupGlossary() {
        loadGlossaryData()
        glossaryAdapter = GlossaryAdapter(glossaryItems) { selectedItem ->
            displayDefinition(selectedItem)
        }
        glossaryRecyclerView.layoutManager = LinearLayoutManager(context)
        glossaryRecyclerView.adapter = glossaryAdapter
        glossarySearchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                glossaryAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        glossarySearchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                selectedTermContainer.visibility = View.GONE
                hideThemeSelector()
                glossaryRecyclerView.visibility = View.VISIBLE
                clearSearchButton.visibility = View.VISIBLE
            } else {
                clearSearchButton.visibility = View.GONE
            }
        }
        clearSearchButton.setOnClickListener {
            glossarySearchEditText.text.clear()
            hideKeyboardAndGlossaryList()
        }
    }

    private fun setupThemeRecyclerView() {
        val themes = listOf("Default Theme", "Taiho Theme", "JSA Theme")
        themeAdapter = ThemeAdapter(themes) { theme ->
            hideThemeSelector()
            val themeToSet = when (theme) {
                "Taiho Theme" -> "PurpleTheme"
                "JSA Theme" -> "JsaTheme"
                else -> "RedTheme"
            }
            saveTheme(themeToSet)
            activity?.recreate()
        }
        themeRecyclerView.layoutManager = LinearLayoutManager(context)
        themeRecyclerView.adapter = themeAdapter
    }

    private fun updateViewsForTheme() {
        val sharedPreferences = activity?.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferences?.getString("SelectedTheme", "RedTheme")
        val displayThemeName: String
        val drawableResId: Int
        when (currentTheme) {
            "PurpleTheme" -> {
                displayThemeName = "Taiho Theme"
                drawableResId = R.drawable.tatakai_purple
            }
            "JsaTheme" -> {
                displayThemeName = "JSA Theme"
                drawableResId = R.drawable.tatakai_jsa
            }
            else -> {
                displayThemeName = "Default Theme"
                drawableResId = R.drawable.tatakai_red
            }
        }
        themeButton.text = displayThemeName
        tatakaiImage.setImageResource(drawableResId)
    }

    private fun saveTheme(themeName: String) {
        val sharedPreferences = activity?.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.putString("SelectedTheme", themeName)?.apply()
    }
}
