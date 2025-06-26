package a48626.sumolmbao.favourites

import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RikishiMatch
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FavouritesManager {

    private const val PREFS_NAME = "SumoFavourites"
    private const val KEY_FAVOURITES = "favourite_rikishi_ids"
    private const val KEY_SEGMENTED_TOP_DIVISION = "segmented_top_division_enabled"
    private const val KEY_SHOW_SEPARATED_DIVISION = "show_separated_division_enabled"
    private const val KEY_SELECTED_DIVISION_FILTER = "selected_division_filter"
    private const val KEY_CACHED_SORTED_FAVOURITES = "cached_sorted_favourites"
    private const val KEY_FAVOURITES_HASH = "favourites_hash"
    private const val KEY_CACHED_RIKISHI_BOUTS = "cached_rikishi_bouts"
    private const val KEY_BOUTS_VISIBILITY_STATE = "bouts_visibility_state"
    // --- NEW: Key for saving video visibility state ---
    private const val KEY_VIDEO_VISIBILITY_STATE = "video_visibility_state"
    // Added for spoiler mode
    private const val PREFS_SETTINGS = "SettingsPreferences"
    private const val SPOILER_MODE_KEY = "SpoilerMode"

    private const val TAG = "FavouritesManager" // Log Tag

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- NEW: Function to save the video visibility state ---
    fun saveVideoVisibilityState(context: Context, state: Map<Int, Boolean>) {
        val json = gson.toJson(state)
        getPrefs(context).edit().putString(KEY_VIDEO_VISIBILITY_STATE, json).apply()
    }

    // --- NEW: Function to load the video visibility state ---
    fun loadVideoVisibilityState(context: Context): MutableMap<Int, Boolean> {
        val json = getPrefs(context).getString(KEY_VIDEO_VISIBILITY_STATE, null)
        return if (json != null) {
            val type = object : TypeToken<MutableMap<Int, Boolean>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableMapOf()
        }
    }

    fun saveBoutsVisibilityState(context: Context, state: Map<Int, Boolean>) {
        val json = gson.toJson(state)
        Log.d(TAG, "Saving Bouts Visibility State: $json")
        getPrefs(context).edit().putString(KEY_BOUTS_VISIBILITY_STATE, json).apply()
    }

    fun loadBoutsVisibilityState(context: Context): MutableMap<Int, Boolean> {
        val json = getPrefs(context).getString(KEY_BOUTS_VISIBILITY_STATE, null)
        Log.d(TAG, "Loading Bouts Visibility State: $json")
        return if (json != null) {
            val type = object : TypeToken<MutableMap<Int, Boolean>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableMapOf()
        }
    }

    fun getFavouriteRikishiIds(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_FAVOURITES, emptySet()) ?: emptySet()
    }

    private fun saveFavourites(context: Context, favourites: Set<String>) {
        val editor = getPrefs(context).edit()
        editor.putStringSet(KEY_FAVOURITES, favourites)
        editor.apply()
        saveFavouritesHash(context, favourites.sorted().joinToString(","))
        editor.remove(KEY_CACHED_SORTED_FAVOURITES).apply() // Invalidate sorted favorites cache
    }

    fun isFavourite(context: Context, rikishiId: Int): Boolean {
        return getFavouriteRikishiIds(context).contains(rikishiId.toString())
    }

    fun toggleFavourite(context: Context, rikishiId: Int): Boolean {
        val favourites = getFavouriteRikishiIds(context).toMutableSet()
        val rikishiIdStr = rikishiId.toString()

        val isNowFavourite: Boolean
        if (favourites.contains(rikishiIdStr)) {
            favourites.remove(rikishiIdStr)
            isNowFavourite = false
        } else {
            favourites.add(rikishiIdStr)
            isNowFavourite = true
        }

        saveFavourites(context, favourites)
        return isNowFavourite
    }

    fun exportFavourites(context: Context): String {
        return getFavouriteRikishiIds(context).joinToString(",")
    }

    fun importFavourites(context: Context, data: String) {
        val favouriteIds = data.split(",").filter { it.isNotBlank() }.toSet()
        saveFavourites(context, favouriteIds)
    }

    fun saveSegmentedTopDivisionPreference(context: Context, isEnabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SEGMENTED_TOP_DIVISION, isEnabled).apply()
    }

    fun loadSegmentedTopDivisionPreference(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SEGMENTED_TOP_DIVISION, false)
    }

    fun saveShowSeparatedDivisionPreference(context: Context, isEnabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_SEPARATED_DIVISION, isEnabled).apply()
    }

    fun loadShowSeparatedDivisionPreference(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_SEPARATED_DIVISION, false)
    }

    fun saveSelectedDivisionFilter(context: Context, division: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_DIVISION_FILTER, division).apply()
    }

    fun loadSelectedDivisionFilter(context: Context): String {
        return getPrefs(context).getString(KEY_SELECTED_DIVISION_FILTER, "All") ?: "All"
    }

    fun saveSortedFavouritesCache(context: Context, data: List<RikishiDetails>) {
        val json = gson.toJson(data)
        getPrefs(context).edit().putString(KEY_CACHED_SORTED_FAVOURITES, json).apply()
    }

    fun loadSortedFavouritesCache(context: Context): List<RikishiDetails>? {
        val json = getPrefs(context).getString(KEY_CACHED_SORTED_FAVOURITES, null)
        return if (json != null) {
            val type = object : TypeToken<List<RikishiDetails>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

    fun saveFavouritesHash(context: Context, hash: String) {
        getPrefs(context).edit().putString(KEY_FAVOURITES_HASH, hash).apply()
    }

    fun loadFavouritesHash(context: Context): String? {
        return getPrefs(context).getString(KEY_FAVOURITES_HASH, null)
    }

    fun loadSpoilerModePreference(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(SPOILER_MODE_KEY, false)
    }
}