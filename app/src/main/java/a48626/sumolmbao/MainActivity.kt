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

    // **FIX**: Keep instances of fragments to manage them
    private val firstFragment = FirstFragment()
    private val secondFragment = SecondFragment()
    private val thirdFragment = ThirdFragment()
    private val fourthFragment = FourthFragment()
    private val fifthFragment = FifthFragment()
    private var activeFragment: Fragment = secondFragment // Default fragment

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

        // **FIX**: Add all fragments initially and hide them
        supportFragmentManager.beginTransaction().apply {
            add(R.id.flFragment, fifthFragment, "5").hide(fifthFragment)
            add(R.id.flFragment, fourthFragment, "4").hide(fourthFragment)
            add(R.id.flFragment, thirdFragment, "3").hide(thirdFragment)
            add(R.id.flFragment, firstFragment, "1").hide(firstFragment)
            // Add and show the default fragment
            add(R.id.flFragment, secondFragment, "2")
        }.commit()

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
        bottomNavigationView.setOnNavigationItemSelectedListener{ item ->
            when (item.itemId) {
                R.id.dohyo -> showFragment(firstFragment)
                R.id.favourites -> showFragment(secondFragment)
                R.id.search -> showFragment(thirdFragment)
                R.id.oneonone -> showFragment(fourthFragment)
                R.id.settings -> showFragment(fifthFragment)
            }
            true
        }
    }

    // **FIX**: New function to hide the current fragment and show the new one
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }

    // This function is now simplified, as we just need to show the fragment
    fun navigateToThirdFragment(rikishi: RikishiDetails) {
        // Pass data to the existing thirdFragment instance
        thirdFragment.arguments = Bundle().apply {
            putSerializable("selectedRikishi", rikishi)
        }
        // Show the third fragment
        showFragment(thirdFragment)
        bottomNavigationView.selectedItemId = R.id.search
    }
}