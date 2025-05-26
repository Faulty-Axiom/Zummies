package a48626.sumolmbao.first_fragment

import a48626.sumolmbao.R
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DivisionAdapter(
    private val divisions: List<String>,
    private val onDivisionSelected: (String) -> Unit,
    private val onDismiss: () -> Unit
) : RecyclerView.Adapter<DivisionAdapter.DivisionViewHolder>() {

    inner class DivisionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textContainer: View = itemView.findViewById(R.id.textContainer)
        val divisionText: TextView = itemView.findViewById(R.id.itemText)

        @SuppressLint("SetTextI18n")
        fun bind(division: String, position: Int) {
            // Show full label (e.g., "1st Division: Makuuchi")
            val displayText = if (division != "Division") {
                val ordinalPrefix = when (position) {
                    1 -> "1st"
                    2 -> "2nd"
                    3 -> "3rd"
                    4 -> "4th"
                    5 -> "5th"
                    6 -> "6th"
                    else -> "${position + 1}th"
                }
                "$ordinalPrefix Division: $division"
            } else {
                division
            }

            divisionText.text = displayText

            // Set up click listener to pass raw division name and dismiss
            textContainer.setOnClickListener {
                onDivisionSelected(division) // Only raw name is passed back
                onDismiss()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DivisionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recycler_view, parent, false)
        return DivisionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DivisionViewHolder, position: Int) {
        holder.bind(divisions[position], position)
    }

    override fun getItemCount(): Int = divisions.size
}
