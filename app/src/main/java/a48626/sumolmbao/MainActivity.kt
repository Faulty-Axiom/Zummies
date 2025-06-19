package a48626.sumolmbao

import a48626.sumolmbao.fifth_fragment.FifthFragment
import a48626.sumolmbao.first_fragment.FirstFragment
import a48626.sumolmbao.fourth_fragment.FourthFragment
import a48626.sumolmbao.third_fragment.SumoApplication
import a48626.sumolmbao.third_fragment.ThirdFragment
import android.content.Context
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?)
    {
        // Apply the saved theme
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        when (sharedPreferences.getString("SelectedTheme", "RedTheme")) {
            "PurpleTheme" -> setTheme(R.style.PurpleTheme)
            "GreenTheme" -> setTheme(R.style.GreenTheme)
            "BlueTheme" -> setTheme(R.style.BlueTheme)
            "JsaTheme" -> setTheme(R.style.JsaTheme)
            else -> setTheme(R.style.RedTheme)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main))
        { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        loadingOverlay = findViewById(R.id.globalLoadingOverlay)

        bottomNavigationView.visibility = View.GONE
        loadingOverlay.visibility = View.VISIBLE
        loadingOverlay.bringToFront()

        setupNavigationAfterLoad()

        val firstFragment = FirstFragment()
        val secondFragment = SecondFragment()
        val thirdFragment = ThirdFragment()
        val fourthFragment = FourthFragment()
        val fifthFragment = FifthFragment()

        setCurrentFragment(secondFragment)

        bottomNavigationView.setOnNavigationItemSelectedListener{
            when (it.itemId)
            {
                R.id.dohyo -> setCurrentFragment(firstFragment)
                R.id.favourites -> setCurrentFragment(secondFragment)
                R.id.search -> setCurrentFragment(thirdFragment)
                R.id.oneonone -> setCurrentFragment(fourthFragment)
                R.id.settings -> setCurrentFragment(fifthFragment)

            }
            true
        }
        // In your Activity's onCreate()
        window.apply {
            // Clear FLAG_TRANSLUCENT_STATUS flag:
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            // Add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // Set status bar color
            statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.bottom_top_colour)
        }
    }
    private fun setupNavigationAfterLoad() {
        CoroutineScope(Dispatchers.Main).launch {
            val startTime = System.currentTimeMillis()
            val MIN_LOADING_TIME = 1500L // 2 seconds minimum

            while (!SumoApplication.isDataLoadingComplete) {
                delay(100)
            }

            // Ensure loading screen shows for at least MIN_LOADING_TIME
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < MIN_LOADING_TIME) {
                delay(MIN_LOADING_TIME - elapsed)
            }

            runOnUiThread {
                loadingOverlay.visibility = View.GONE
                bottomNavigationView.visibility = View.VISIBLE
                bottomNavigationView.selectedItemId = R.id.favourites
            }
        }
    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }
}