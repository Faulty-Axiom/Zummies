package a48626.sumolmbao

import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.fifth_fragment.FifthFragment
import a48626.sumolmbao.first_fragment.FirstFragment
import a48626.sumolmbao.fourth_fragment.FourthFragment
import a48626.sumolmbao.third_fragment.SumoApplication
import a48626.sumolmbao.third_fragment.ThirdFragment
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity()
{
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var loadingOverlay: FrameLayout

    // --- Keep instances of fragments that DON'T need to be replaced ---
    private val firstFragment = FirstFragment()
    private val secondFragment = SecondFragment()
    private val fourthFragment = FourthFragment()
    private val fifthFragment = FifthFragment()

    // --- ThirdFragment will be nullable and created on-demand ---
    private var thirdFragment: ThirdFragment? = null
    private var activeFragment: Fragment = secondFragment

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        when (sharedPreferences.getString("SelectedTheme", "RedTheme")) {
            "PurpleTheme" -> setTheme(R.style.PurpleTheme)
            "JsaTheme" -> setTheme(R.style.JsaTheme)
            else -> setTheme(R.style.RedTheme)
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        loadingOverlay = findViewById(R.id.globalLoadingOverlay)
        bottomNavigationView.visibility = View.GONE
        loadingOverlay.visibility = View.VISIBLE
        loadingOverlay.bringToFront()

        // --- Add all fragments initially and hide them ---
        supportFragmentManager.beginTransaction().apply {
            add(R.id.flFragment, fifthFragment, "5").hide(fifthFragment)
            add(R.id.flFragment, fourthFragment, "4").hide(fourthFragment)
            // Do NOT add thirdFragment here
            add(R.id.flFragment, firstFragment, "1").hide(firstFragment)
            add(R.id.flFragment, secondFragment, "2")
        }.commit()

        activeFragment = secondFragment // Set initial active fragment

        setupNavigationAfterLoad()

        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.bottom_top_colour)
        }
    }

    private fun setupNavigationAfterLoad() {
        CoroutineScope(Dispatchers.Main).launch {
            val startTime = System.currentTimeMillis()
            val MIN_LOADING_TIME = 1500L
            while (!SumoApplication.isDataLoadingComplete) {
                delay(100)
            }
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < MIN_LOADING_TIME) {
                delay(MIN_LOADING_TIME - elapsed)
            }
            runOnUiThread {
                loadingOverlay.visibility = View.GONE
                bottomNavigationView.visibility = View.VISIBLE
                bottomNavigationView.selectedItemId = R.id.favourites
                setupBottomNavListener()
            }
        }
    }

    private fun setupBottomNavListener() {
        bottomNavigationView.setOnItemSelectedListener{ item ->
            when (item.itemId) {
                R.id.dohyo -> showFragment(firstFragment)
                R.id.favourites -> showFragment(secondFragment)
                R.id.search -> {
                    // If thirdFragment is null (e.g., first time clicking), create an empty one
                    if (thirdFragment == null) {
                        thirdFragment = ThirdFragment()
                        supportFragmentManager.beginTransaction().add(R.id.flFragment, thirdFragment!!, "3").commitNow()
                    }
                    showFragment(thirdFragment!!)
                }
                R.id.oneonone -> showFragment(fourthFragment)
                R.id.settings -> showFragment(fifthFragment)
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        if (activeFragment === fragment) return

        val transaction = supportFragmentManager.beginTransaction()
        transaction.hide(activeFragment).show(fragment)
        transaction.commit()
        activeFragment = fragment
    }

    fun navigateToThirdFragment(rikishi: RikishiDetails) {
        Log.d("MainActivity", "navigateToThirdFragment called for: ${rikishi.shikonaEn}")

        // 1. Create a new ThirdFragment instance and pass the data via arguments
        val newThirdFragment = ThirdFragment().apply {
            arguments = Bundle().apply {
                putSerializable("selectedRikishi", rikishi)
            }
        }

        val transaction = supportFragmentManager.beginTransaction()

        // 2. Hide the current active fragment (SecondFragment)
        transaction.hide(activeFragment)

        // 3. If an old thirdFragment exists, remove it completely
        if (thirdFragment != null) {
            Log.d("MainActivity", "Removing old ThirdFragment instance.")
            transaction.remove(thirdFragment!!)
        }

        // 4. Add the new fragment instance to the container
        Log.d("MainActivity", "Adding new ThirdFragment instance with arguments.")
        transaction.add(R.id.flFragment, newThirdFragment, "3") // Use the same tag

        transaction.commit()

        // 5. Update our references
        thirdFragment = newThirdFragment
        activeFragment = newThirdFragment

        // 6. Update the BottomNavigationView selection
        bottomNavigationView.selectedItemId = R.id.search
    }
}