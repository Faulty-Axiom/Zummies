package a48626.sumolmbao.third_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.data.RikishiDetails
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RikishiAdapter(
    private var rikishiList: List<RikishiDetails>,
    private val recyclerView: RecyclerView,
    private val onItemClick: ((RikishiDetails) -> Unit)? = null

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var maxItemCountForFullHeight = 20
    var shouldExpandToContent = false

    companion object {
        private const val TYPE_RIKISHI = 0
        private const val TYPE_DIVIDER = 1
    }

    // ViewHolder no longer references a favourite button
    inner class RikishiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.rikishiName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val itemPosition = position / 2
                    if (itemPosition < rikishiList.size) {
                        onItemClick?.invoke(rikishiList[itemPosition])
                        recyclerView.visibility = View.GONE
                    }
                }
            }
        }
    }

    inner class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return if (position % 2 == 0) TYPE_RIKISHI else TYPE_DIVIDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RIKISHI -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_rikishi, parent, false)
                RikishiViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_divider, parent, false)
                DividerViewHolder(view)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RikishiViewHolder -> {
                val itemPosition = position / 2
                if (itemPosition < rikishiList.size) {
                    val rikishi = rikishiList[itemPosition]
                    val cleanName = rikishi.shikonaEn?.replace("#", "") ?: "Unknown"
                    val displayRank = rikishi.currentRank ?: "Retired"
                    holder.nameTextView.text = "$cleanName - $displayRank"
                    // All favourite button logic has been removed from here
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (rikishiList.isEmpty()) 0 else (rikishiList.size * 2) - 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<RikishiDetails>, shouldExpand: Boolean = false) {
        rikishiList = newList
        shouldExpandToContent = shouldExpand
        notifyDataSetChanged()
    }

    fun shouldShowFullHeight(): Boolean {
        return !shouldExpandToContent && rikishiList.size >= maxItemCountForFullHeight
    }
}