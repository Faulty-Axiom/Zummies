package a48626.sumolmbao.favourites

import a48626.sumolmbao.data.RikishiDetails
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavouritesAdapter(
    private var favouriteRikishiList: List<RikishiDetails>
) : RecyclerView.Adapter<FavouritesAdapter.FavouriteViewHolder>() {

    inner class FavouriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rikishiName: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        // Using a simple built-in Android layout to just display the name
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return FavouriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        val rikishi = favouriteRikishiList[position]
        holder.rikishiName.text = rikishi.shikonaEn
        holder.rikishiName.textSize = 18f
        holder.rikishiName.setTypeface(null, Typeface.BOLD)
        // You can add more details here if you change the layout later
    }

    override fun getItemCount(): Int = favouriteRikishiList.size

    fun updateData(newFavourites: List<RikishiDetails>) {
        favouriteRikishiList = newFavourites
        notifyDataSetChanged()
    }
}
