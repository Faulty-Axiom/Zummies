package a48626.sumolmbao.second_fragment

import a48626.sumolmbao.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout // Import LinearLayout for divider
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DivisionFilterAdapter(
    var divisions: List<String>,
    private val onDivisionSelected: (String) -> Unit
) : RecyclerView.Adapter<DivisionFilterAdapter.DivisionViewHolder>() {

    private var selectedDivision: String = "All"

    inner class DivisionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val divisionName: TextView = itemView.findViewById(R.id.itemText)
        val divider: LinearLayout = itemView.findViewById(R.id.divider) // Reference the divider LinearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DivisionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recycler_view, parent, false) // CHANGED: Use item_recycler_view
        return DivisionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DivisionViewHolder, position: Int) {
        val division = divisions[position]
        holder.divisionName.text = division

        val isSelected = (division == selectedDivision)
        holder.divisionName.setTextColor(
            if (isSelected) holder.itemView.context.getColor(R.color.white)
            else holder.itemView.context.getColor(R.color.selectedGrey)
        )
        // Ensure colorSwatch is not visible if item_theme_view was reused previously
        holder.itemView.findViewById<View>(R.id.colorSwatch)?.visibility = View.GONE

        // Hide divider for the last item
        holder.divider.visibility = if (position == divisions.size - 1) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onDivisionSelected(division)
            updateSelectedDivision(division)
        }
    }

    override fun getItemCount(): Int = divisions.size

    fun updateData(newDivisions: List<String>) {
        divisions = newDivisions
        notifyDataSetChanged()
    }

    fun updateSelectedDivision(newSelection: String) {
        if (selectedDivision != newSelection) {
            val oldPosition = divisions.indexOf(selectedDivision)
            val newPosition = divisions.indexOf(newSelection)
            selectedDivision = newSelection
            if (oldPosition != -1) notifyItemChanged(oldPosition)
            if (newPosition != -1) notifyItemChanged(newPosition)
        }
    }
}