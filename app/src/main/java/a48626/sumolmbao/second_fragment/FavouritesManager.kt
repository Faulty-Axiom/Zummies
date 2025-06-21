package a48626.sumolmbao.favourites

import android.content.Context
import android.content.SharedPreferences

object FavouritesManager {

    private const val PREFS_NAME = "SumoFavourites"
    private const val KEY_FAVOURITES = "favourite_rikishi_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFavouriteRikishiIds(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_FAVOURITES, emptySet()) ?: emptySet()
    }

    private fun saveFavourites(context: Context, favourites: Set<String>) {
        val editor = getPrefs(context).edit()
        editor.putStringSet(KEY_FAVOURITES, favourites)
        editor.apply()
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
}
