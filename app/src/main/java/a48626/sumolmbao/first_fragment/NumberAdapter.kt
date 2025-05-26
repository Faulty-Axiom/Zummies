package a48626.sumolmbao.first_fragment

import a48626.sumolmbao.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NumberAdapter(private val pages: List<List<Int>>) : RecyclerView.Adapter<NumberAdapter.NumberViewHolder>() {

    private var selectedNumber: Int? = null
    var onNumberSelected: ((Int?) -> Unit)? = null
    var onPageChange: ((Int) -> Unit)? = null  // For arrow navigation
    private val viewHolders = mutableMapOf<Int, NumberViewHolder>()

    inner class NumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberContainer: LinearLayout = itemView.findViewById(R.id.numberContainer)
        val leftArrow: TextView = itemView.findViewById(R.id.leftArrowIndicator)
        val rightArrow: TextView = itemView.findViewById(R.id.rightArrowIndicator)

        init {
            leftArrow.setOnClickListener {
                onPageChange?.invoke(adapterPosition - 1)
            }
            rightArrow.setOnClickListener {
                onPageChange?.invoke(adapterPosition + 1)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_number_page, parent, false)
        return NumberViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        val numbers = pages[position]
        viewHolders[position] = holder

        // Set arrow visibility
        holder.leftArrow.visibility = View.GONE
        holder.rightArrow.visibility = View.GONE
        // Clear previous views
        holder.numberContainer.removeAllViews()

        // Add number views
        numbers.forEach { number ->
            val numberView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_number, holder.numberContainer, false) as TextView

            numberView.text = number.toString()
            numberView.setBackgroundResource(
                if (number == selectedNumber) R.drawable.number_selected_background
                else R.drawable.number_unselected_background
            )

            numberView.setOnClickListener {
                selectedNumber = if (selectedNumber == number) null else number
                onNumberSelected?.invoke(selectedNumber)
                notifyDataSetChanged()
            }

            holder.numberContainer.addView(numberView)
        }
    }

    fun getViewHolderAt(position: Int): NumberViewHolder? {
        return viewHolders[position]
    }

    override fun getItemCount(): Int = pages.size

    // Helper function to update selection
    fun setSelectedDay(day: Int?) {
        selectedNumber = day
        notifyDataSetChanged()
    }
}