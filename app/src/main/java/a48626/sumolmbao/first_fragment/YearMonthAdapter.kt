package a48626.sumolmbao.first_fragment

import a48626.sumolmbao.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class YearMonthAdapter(
    private val items: List<String>,
    private val onItemClick: (String) -> Unit,
    private val onDismiss: () -> Unit // Added dismiss callback
) : RecyclerView.Adapter<YearMonthAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textContainer: View = itemView.findViewById(R.id.textContainer)
        val itemText: TextView = itemView.findViewById(R.id.itemText)
        private val divider: View = itemView.findViewById(R.id.divider) // Add this

        fun bind(item: String, isLast: Boolean) { // Add isLast parameter
            itemText.text = item
            itemView.setOnClickListener {
                onItemClick(item)
                onDismiss() // Call dismiss when item is clicked
            }
            divider.visibility = if (isLast) View.GONE else View.VISIBLE // Hide if last
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recycler_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val isLast = position == items.size - 1 // Check if it's the last item
        holder.bind(items[position], isLast)
    }

    override fun getItemCount(): Int = items.size
}