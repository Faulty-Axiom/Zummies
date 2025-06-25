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

    var sortedFavouriteRikishi: List<RikishiDetails> = emptyList()
    var lastLoadedFavouritesHash: String? = null

    // Log when the ViewModel is about to be destroyed. This should only happen when the app is killed.
    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModelLifecycle", "SecondFragmentViewModel instance CLEARED: ${this.hashCode()}")
    }
}