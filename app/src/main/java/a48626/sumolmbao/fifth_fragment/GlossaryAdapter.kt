package a48626.sumolmbao.fifth_fragment

import a48626.sumolmbao.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

data class GlossaryItem(val term: String, val definition: String)

class GlossaryAdapter(
    private var glossaryList: List<GlossaryItem>,
    private val onItemClicked: (GlossaryItem) -> Unit
) : RecyclerView.Adapter<GlossaryAdapter.GlossaryViewHolder>(), Filterable {

    var glossaryListFiltered: List<GlossaryItem> = glossaryList

    inner class GlossaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val termTextView: TextView = itemView.findViewById(R.id.glossary_term)
        val divider: View = itemView.findViewById(R.id.divider)

        fun bind(item: GlossaryItem) {
            termTextView.text = item.term
            itemView.setOnClickListener { onItemClicked(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GlossaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_glossary, parent, false)
        return GlossaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GlossaryViewHolder, position: Int) {
        holder.bind(glossaryListFiltered[position])
        holder.divider.visibility = if (position == glossaryListFiltered.size - 1) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = glossaryListFiltered.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString()?.lowercase(Locale.ROOT) ?: ""
                glossaryListFiltered = if (charString.isEmpty()) {
                    glossaryList
                } else {
                    glossaryList.filter {
                        // Logic updated from .contains() to .startsWith()
                        it.term.lowercase(Locale.ROOT).startsWith(charString)
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = glossaryListFiltered
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                glossaryListFiltered = results?.values as? List<GlossaryItem> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}