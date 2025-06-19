package a48626.sumolmbao.fifth_fragment

import a48626.sumolmbao.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ThemeAdapter(
    private val themes: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val themeName: TextView = itemView.findViewById(R.id.itemText)
        val colorSwatch: View = itemView.findViewById(R.id.colorSwatch)
        val divider: View = itemView.findViewById(R.id.divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_theme_view, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]
        holder.themeName.text = theme

        val colorRes = when (theme) {
            "Taiho Theme" -> R.color.button_purple
            "Green Theme" -> R.color.button_green
            "Blue Theme" -> R.color.button_blue
            "JSA Theme" -> R.color.button_jsa
            else -> R.color.button_dark_red
        }
        holder.colorSwatch.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, colorRes))

        holder.itemView.setOnClickListener {
            onItemClick(theme)
        }

        holder.divider.visibility = if (position == themes.size - 1) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = themes.size
}