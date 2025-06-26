package a48626.sumolmbao

import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RikishiMatch
import a48626.sumolmbao.data.TournamentResultDisplayData
import android.util.Log
import androidx.lifecycle.ViewModel

class SecondFragmentViewModel : ViewModel() {
    init {
        // Log when a new ViewModel is created. This should only happen once.
        Log.d("ViewModelLifecycle", "New SecondFragmentViewModel instance CREATED: ${this.hashCode()}")
    }

    val cachedTournamentHistory = mutableMapOf<Int, List<TournamentResultDisplayData>>()
    val gridVisibilityState = mutableMapOf<Int, Boolean>()
    val cachedRikishiBouts = mutableMapOf<Int, List<RikishiMatch>>()
    val boutsVisibilityState = mutableMapOf<Int, Boolean>()

    // --- FIX: Add the missing property to hold the video button's state ---
    var videoVisibilityState = mutableMapOf<Int, Boolean>()

    var sortedFavouriteRikishi: List<RikishiDetails> = emptyList()
    var lastLoadedFavouritesHash: String? = null

    var lastLoadedStateSignature: String? = null

    /**
     * Toggles the visibility state of the video button for a given rikishi.
     * If it's visible, it becomes hidden, and vice-versa.
     */
    fun toggleVideoVisibility(rikishiId: Int) {
        val oldState = videoVisibilityState.getOrDefault(rikishiId, false)
        videoVisibilityState[rikishiId] = !oldState
        Log.d("ViewModelState", "Toggled video visibility for Rikishi ID $rikishiId from $oldState to ${!oldState}")
    }

    // Also add logging to the bouts toggle
    fun toggleBoutsVisibility(rikishiId: Int) {
        val oldState = boutsVisibilityState.getOrDefault(rikishiId, false)
        boutsVisibilityState[rikishiId] = !oldState
        Log.d("ViewModelState", "Toggled BOUTS visibility for Rikishi ID $rikishiId from $oldState to ${!oldState}")
    }

    // Log when the ViewModel is about to be destroyed. This should only happen when the app is killed.
    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModelLifecycle", "SecondFragmentViewModel instance CLEARED: ${this.hashCode()}")
    }
}