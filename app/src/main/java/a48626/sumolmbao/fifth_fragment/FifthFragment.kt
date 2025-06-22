package a48626.sumolmbao.fifth_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.favourites.FavouritesManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FifthFragment : Fragment() {

    // --- Views ---
    private lateinit var themeButton: TextView
    private lateinit var themeRecyclerView: RecyclerView
    private lateinit var overlay: View
    private lateinit var glossarySearchEditText: EditText
    private lateinit var glossaryRecyclerView: RecyclerView
    private lateinit var clearSearchButton: ImageView
    private lateinit var selectedTermContainer: CardView
    private lateinit var selectedTermText: TextView
    private lateinit var selectedDefinitionText: TextView
    private lateinit var tatakaiImage: ImageView
    private lateinit var exportTextView: TextView
    private lateinit var importTextView: TextView
    private lateinit var exportCard: CardView
    private lateinit var importCard: CardView


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
        clearSearchButton = view.findViewById(R.id.clear_search_button)
        selectedTermContainer = view.findViewById(R.id.selected_term_container)
        selectedTermText = view.findViewById(R.id.selected_term_text)
        selectedDefinitionText = view.findViewById(R.id.selected_definition_text)
        tatakaiImage = view.findViewById(R.id.tatakai_image)
        exportTextView = view.findViewById(R.id.export_textview_button)
        importTextView = view.findViewById(R.id.import_textview_button)
        exportCard = view.findViewById(R.id.export_card)
        importCard = view.findViewById(R.id.import_card)
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
            val exportDataTextView = exportCard.findViewById<EditText>(R.id.export_data_textview)
            exportDataTextView.setText(favouritesManager.exportFavourites(requireContext()))
        }

        importTextView.setOnClickListener {
            importCard.visibility = View.VISIBLE
        }

        val closeExportButton = exportCard.findViewById<Button>(R.id.close_export_button)
        closeExportButton.setOnClickListener {
            exportCard.visibility = View.GONE
        }

        val closeImportButton = importCard.findViewById<Button>(R.id.close_import_button)
        closeImportButton.setOnClickListener {
            importCard.visibility = View.GONE
        }

        val saveImportButton = importCard.findViewById<Button>(R.id.save_import_button)
        saveImportButton.setOnClickListener {
            val importDataEditText = importCard.findViewById<EditText>(R.id.import_data_edittext)
            val data = importDataEditText.text.toString()
            if (data.isNotBlank()) {
                favouritesManager.importFavourites(requireContext(), data)
                Toast.makeText(requireContext(), "Favourites imported successfully!", Toast.LENGTH_SHORT).show()
                importCard.visibility = View.GONE
            } else {
                Toast.makeText(requireContext(), "Please paste your data", Toast.LENGTH_SHORT).show()
            }
        }
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