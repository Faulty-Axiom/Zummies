// app/src/main/java/a48626/sumolmbao/third_fragment/SumoApplication.kt
package a48626.sumolmbao.third_fragment

import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.RikishiDao
import a48626.sumolmbao.database.toEntity
import a48626.sumolmbao.retrofit.RetrofitInstance
import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// This class will be the first one to be initialized when the app is loading

class SumoApplication : Application() {
    companion object {
        var isDataLoadingComplete = false
            private set
        var isAppFirstLaunch = true
            private set
        fun onDataLoaded() {
            isDataLoadingComplete = true
            isAppFirstLaunch = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        RikishiImageHelper.initialize(this)
        initRikishiData()

    }

    private fun initRikishiData() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = RikishiDatabase.getDatabase(this@SumoApplication)
            val rikishiDao = database.rikishiDao()

            val prefs = getSharedPreferences("cache_prefs", MODE_PRIVATE)
            val lastFetched = prefs.getLong("rikishi_last_fetch", 0L)
            val now = System.currentTimeMillis()
            val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L

            val isExpired = now - lastFetched > oneWeekMillis
            val count = rikishiDao.getCount()

            // Log the cache expiration status
            Log.d("CacheStatus", "isExpired: $isExpired")

            if (count == 0 || isExpired) {
                fetchAndStoreRikishi(rikishiDao)
                prefs.edit().putLong("rikishi_last_fetch", now).apply()
                Log.d("DataInit", "Fetched fresh data and updated timestamp")
            } else {
                // Calculate and log the time remaining until cache refresh
                val timeRemainingMillis = lastFetched + oneWeekMillis - now
                val hoursRemaining = timeRemainingMillis / (60 * 60 * 1000)
                val minutesRemaining = (timeRemainingMillis % (60 * 60 * 1000)) / (60 * 1000)
                Log.d("DataInit", "Using cached data ($count records). Cache will refresh in approximately ${hoursRemaining} hours and ${minutesRemaining} minutes.")
            }

            onDataLoaded()
        }
    }

    private suspend fun fetchAndStoreRikishi(rikishiDao: RikishiDao) {
        try {
            val allRikishi = fetchAllPages()
            Log.d("DataInit", "Fetched ${allRikishi.size} records from API")

            withContext(Dispatchers.IO) {
                rikishiDao.clear()
                val entities = allRikishi.map { it.toEntity(System.currentTimeMillis()) }
                Log.d("DataInit", "Converted to ${entities.size} entities")

                rikishiDao.insertAllInBatches(entities)
                val newCount = rikishiDao.getCount()
                Log.d("DataInit", "Successfully stored $newCount records")
            }
        } catch (e: Exception) {
            Log.e("DataInit", "Error loading data", e)
        }
    }

    private fun fetchAllPages(): List<RikishiDetails> {
        val allRikishi = mutableListOf<RikishiDetails>()
        var currentPage = 0
        val pageSize = 1000

        do {
            Log.d("API Debug", "Fetching page $currentPage")
            val response = try {
                val call = RetrofitInstance.api.getRikishis(
                    limit = pageSize,
                    skip = currentPage * pageSize,
                    intai = true
                )
                call.execute()
            } catch (e: Exception) {
                Log.e("API Debug", "Network error", e)
                break
            }

            Log.d("API Debug", "Response code: ${response.code()}")
            Log.d("API Debug", "Response body: ${response.body()}")

            val rikishiList = response.body()?.records ?: emptyList()
            if (rikishiList.isNotEmpty()) {
                allRikishi.addAll(rikishiList)
                currentPage++
                Log.d("API Debug", "Fetched ${rikishiList.size} records")
            } else {
                Log.d("API Debug", "Empty page received")
            }
        } while (rikishiList.size == pageSize)

        return allRikishi
    }
}