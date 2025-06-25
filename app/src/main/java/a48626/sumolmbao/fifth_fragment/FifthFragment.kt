package a48626.sumolmbao.fifth_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.favourites.FavouritesManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button // Import Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FifthFragment : Fragment() {

    // --- Views ---
    private lateinit var themeButton: TextView
    private lateinit var themeRecyclerView: RecyclerView
    private lateinit var overlay: View
    private lateinit var glossarySearchEditText: EditText
    private lateinit var glossaryRecyclerView: RecyclerView
    private lateinit var clearSearchButton: ImageView // Corrected name: was clear_search_button
    private lateinit var selectedTermContainer: CardView // Corrected name: was selected_term_container
    private lateinit var selectedTermText: TextView // Corrected name: was selected_term_text
    private lateinit var selectedDefinitionText: TextView // Corrected name: was selected_definition_text
    private lateinit var tatakaiImage: ImageView // Corrected name: was tatakai_image
    private lateinit var exportTextView: TextView // Corrected name: was export_textview_button
    private lateinit var importTextView: TextView // Corrected name: was import_textview_button
    private lateinit var exportCard: CardView // Corrected name: was export_card
    private lateinit var importCard: CardView // Corrected name: was import_card

    // Corrected variable names and types for popup buttons
    private lateinit var exportDataTextView: EditText // Corrected: This is the EditText in popup_export
    private lateinit var closeExportButton: Button // Corrected: Was closeExportPopup (ImageView)
    private lateinit var importDataEditText: EditText // Corrected: This is the EditText in popup_import
    private lateinit var closeImportButton: Button // Corrected: Was closeImportPopup (ImageView)
    private lateinit var saveImportButton: Button // Corrected: Was confirmImportButton (Button)


    // --- Adapters and Data ---
    private lateinit var themeAdapter: ThemeAdapter
    private lateinit var glossaryAdapter: GlossaryAdapter
    private val glossaryItems = mutableListOf<GlossaryItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fifth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        updateViewsForTheme() // Set initial theme-based UI
        setupThemeSelector()
        setupGlossary()
        setupImportExport(view)
    }

    private fun initializeViews(view: View) {
        themeButton = view.findViewById(R.id.themeButton)
        themeRecyclerView = view.findViewById(R.id.themeRecyclerView)
        overlay = view.findViewById(R.id.overlay)
        glossarySearchEditText = view.findViewById(R.id.glossarySearchEditText)
        glossaryRecyclerView = view.findViewById(R.id.glossaryRecyclerView)
        clearSearchButton = view.findViewById(R.id.clear_search_button) // Corrected ID
        selectedTermContainer = view.findViewById(R.id.selected_term_container) // Corrected ID
        selectedTermText = view.findViewById(R.id.selected_term_text) // Corrected ID
        selectedDefinitionText = view.findViewById(R.id.selected_definition_text) // Corrected ID
        tatakaiImage = view.findViewById(R.id.tatakai_image) // Corrected ID
        exportTextView = view.findViewById(R.id.export_textview_button) // Corrected ID
        importTextView = view.findViewById(R.id.import_textview_button) // Corrected ID
        exportCard = view.findViewById(R.id.export_card) // Corrected ID
        importCard = view.findViewById(R.id.import_card) // Corrected ID

        // Initialize popup-specific views found within their respective CardViews
        // These need to be initialized here because they are part of the main layout,
        // but their specific includes have the IDs.
        exportDataTextView = exportCard.findViewById(R.id.export_data_textview) // Corrected ID
        closeExportButton = exportCard.findViewById(R.id.close_export_button) // Corrected ID and Type
        importDataEditText = importCard.findViewById(R.id.import_data_edittext) // Corrected ID
        closeImportButton = importCard.findViewById(R.id.close_import_button) // Corrected ID and Type
        saveImportButton = importCard.findViewById(R.id.save_import_button) // Corrected ID and Type
    }

    private fun setupThemeSelector() {
        setupThemeRecyclerView()

        themeButton.setOnClickListener {
            animateButtonClick(it)
            if (themeRecyclerView.isVisible) {
                hideThemeSelector()
            } else {
                hideKeyboardAndGlossaryList()
                showThemeSelector()
            }
        }

        overlay.setOnClickListener {
            hideThemeSelector()
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

    private fun setupImportExport(view: View) {
        val favouritesManager = FavouritesManager

        exportTextView.setOnClickListener {
            exportCard.visibility = View.VISIBLE
            // exportDataTextView is already initialized in initializeViews
            exportDataTextView.setText(favouritesManager.exportFavourites(requireContext()))
        }

        importTextView.setOnClickListener {
            importCard.visibility = View.VISIBLE
        }

        closeExportButton.setOnClickListener { // Corrected variable name
            exportCard.visibility = View.GONE
        }

        closeImportButton.setOnClickListener { // Corrected variable name
            importCard.visibility = View.GONE
        }

        saveImportButton.setOnClickListener { // Corrected variable name
            // importDataEditText is already initialized in initializeViews
            val data = importDataEditText.text.toString()
            if (data.isNotBlank()) {
                favouritesManager.importFavourites(requireContext(), data)
                Toast.makeText(requireContext(), "Favourites imported successfully!", Toast.LENGTH_SHORT).show()
                importCard.visibility = View.GONE
            } else {
                Toast.makeText(requireContext(), "Please paste your data", Toast.LENGTH_SHORT).show()
            }
        }

        // NEW: Add the "See segmented top division" switch
        val settingsContainer = view.findViewById<LinearLayout>(R.id.settingsContainer)

        val segmentedTopDivisionSwitch = Switch(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, resources.getDimensionPixelSize(R.dimen.default_margin), 0, 0)
            }
            text = getString(R.string.setting_segmented_top_division)
            isChecked = FavouritesManager.loadSegmentedTopDivisionPreference(requireContext())
            setOnCheckedChangeListener { _, isChecked ->
                FavouritesManager.saveSegmentedTopDivisionPreference(requireContext(), isChecked)
                Toast.makeText(context, "Preference saved. Re-open Favourites tab to see changes.", Toast.LENGTH_SHORT).show()
            }
        }
        settingsContainer?.addView(segmentedTopDivisionSwitch)
    }

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
            "JSA Theme" -> {
                displayThemeName = "JSA Theme"
                drawableResId = R.drawable.tatakai_jsa
            }
            else -> { // Default for RedTheme, GreenTheme, BlueTheme
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