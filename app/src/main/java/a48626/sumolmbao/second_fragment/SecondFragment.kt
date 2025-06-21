package a48626.sumolmbao

import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.favourites.FavouritesAdapter
import a48626.sumolmbao.favourites.FavouritesManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class SecondFragment : Fragment() {

    private lateinit var favouritesRecyclerView: RecyclerView
    private lateinit var favouritesAdapter: FavouritesAdapter
    private var allRikishiList: List<RikishiDetails> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favouritesRecyclerView = view.findViewById(R.id.favourites_recycler_view)
        favouritesRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter with an empty list
        favouritesAdapter = FavouritesAdapter(emptyList())
        favouritesRecyclerView.adapter = favouritesAdapter
    }

    override fun onResume() {
        super.onResume()
        // Load and display favourites every time the fragment is shown
        loadAndDisplayFavourites()
    }

    private fun loadAndDisplayFavourites() {
        val favouriteIds = FavouritesManager.getFavouriteRikishiIds(requireContext())
        val rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()

        lifecycleScope.launch {
            // Fetch all rikishi entities from the database
            val allRikishiEntities = rikishiDao.getAllRikishi()
            allRikishiList = allRikishiEntities.map { it.toRikishiDetails() }

            // Filter the full list of rikishi to get only the favourited ones
            val favouriteRikishi = allRikishiList.filter { rikishi ->
                favouriteIds.contains(rikishi.id.toString())
            }

            // Update the adapter with the new list
            favouritesAdapter.updateData(favouriteRikishi)
        }
    }
}
