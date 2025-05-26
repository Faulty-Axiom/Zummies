package a48626.sumolmbao.third_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.data.RikishiId
import a48626.sumolmbao.data.RikishiStats
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener

class RikishiDetailAdapter(
    private val rikishi: RikishiId,
    private val stats: RikishiStats? = null,
    private val highestRank: String? = null
) : RecyclerView.Adapter<RikishiDetailAdapter.RikishiDetailViewHolder>() {

    inner class RikishiDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val shikonaHistoryTextView: TextView = itemView.findViewById(R.id.shikonaHistoryTextView)
        val rankTextView: TextView = itemView.findViewById(R.id.rankTextView)
        val heyaTextView: TextView = itemView.findViewById(R.id.stableTextView)
        val birthDateTextView: TextView = itemView.findViewById(R.id.birthDateTextView)
        val shusshinTextView: TextView = itemView.findViewById(R.id.shusshinTextView)
        val heightTextView: TextView = itemView.findViewById(R.id.heightTextView)
        val weightTextView: TextView = itemView.findViewById(R.id.weightTextView)
        val debutTextView: TextView = itemView.findViewById(R.id.debutTextView)
        val totalWinsTextView: TextView = itemView.findViewById(R.id.totalWinsTextView)
        val statsSection: View = itemView.findViewById(R.id.statsSection)
        val highestRankTextView: TextView = itemView.findViewById(R.id.highestRankTextView)
        val rikishiImageView: ImageView = itemView.findViewById(R.id.rikishiImageView)

        // Division stats table views
        val careerBashoTextView: TextView = itemView.findViewById(R.id.careerBashoTextView)
        val makuuchiBashoTextView: TextView = itemView.findViewById(R.id.makuuchiBashoTextView)
        val juryoBashoTextView: TextView = itemView.findViewById(R.id.juryoBashoTextView)
        val makushitaBashoTextView: TextView = itemView.findViewById(R.id.makushitaBashoTextView)
        val sandanmeBashoTextView: TextView = itemView.findViewById(R.id.sandanmeBashoTextView)
        val jonidanBashoTextView: TextView = itemView.findViewById(R.id.jonidanBashoTextView)
        val jonokuchiBashoTextView: TextView = itemView.findViewById(R.id.jonokuchiBashoTextView)

        val careerYushoTextView: TextView = itemView.findViewById(R.id.careerYushoTextView)
        val makuuchiYushoTextView: TextView = itemView.findViewById(R.id.makuuchiYushoTextView)
        val juryoYushoTextView: TextView = itemView.findViewById(R.id.juryoYushoTextView)
        val makushitaYushoTextView: TextView = itemView.findViewById(R.id.makushitaYushoTextView)
        val sandanmeYushoTextView: TextView = itemView.findViewById(R.id.sandanmeYushoTextView)
        val jonidanYushoTextView: TextView = itemView.findViewById(R.id.jonidanYushoTextView)
        val jonokuchiYushoTextView: TextView = itemView.findViewById(R.id.jonokuchiYushoTextView)

        val careerWinsTextView: TextView = itemView.findViewById(R.id.careerWinsTextView)
        val makuuchiWinsTextView: TextView = itemView.findViewById(R.id.makuuchiWinsTextView)
        val juryoWinsTextView: TextView = itemView.findViewById(R.id.juryoWinsTextView)
        val makushitaWinsTextView: TextView = itemView.findViewById(R.id.makushitaWinsTextView)
        val sandanmeWinsTextView: TextView = itemView.findViewById(R.id.sandanmeWinsTextView)
        val jonidanWinsTextView: TextView = itemView.findViewById(R.id.jonidanWinsTextView)
        val jonokuchiWinsTextView: TextView = itemView.findViewById(R.id.jonokuchiWinsTextView)

        val careerLossesTextView: TextView = itemView.findViewById(R.id.careerLossesTextView)
        val makuuchiLossesTextView: TextView = itemView.findViewById(R.id.makuuchiLossesTextView)
        val juryoLossesTextView: TextView = itemView.findViewById(R.id.juryoLossesTextView)
        val makushitaLossesTextView: TextView = itemView.findViewById(R.id.makushitaLossesTextView)
        val sandanmeLossesTextView: TextView = itemView.findViewById(R.id.sandanmeLossesTextView)
        val jonidanLossesTextView: TextView = itemView.findViewById(R.id.jonidanLossesTextView)
        val jonokuchiLossesTextView: TextView = itemView.findViewById(R.id.jonokuchiLossesTextView)

        val careerTotalTextView: TextView = itemView.findViewById(R.id.careerTotalTextView)
        val makuuchiTotalTextView: TextView = itemView.findViewById(R.id.makuuchiTotalTextView)
        val juryoTotalTextView: TextView = itemView.findViewById(R.id.juryoTotalTextView)
        val makushitaTotalTextView: TextView = itemView.findViewById(R.id.makushitaTotalTextView)
        val sandanmeTotalTextView: TextView = itemView.findViewById(R.id.sandanmeTotalTextView)
        val jonidanTotalTextView: TextView = itemView.findViewById(R.id.jonidanTotalTextView)
        val jonokuchiTotalTextView: TextView = itemView.findViewById(R.id.jonokuchiTotalTextView)

        val sanshoTitleTextView: TextView = itemView.findViewById(R.id.sanshoTitleTextView)
        val sanshoContainer: LinearLayout = itemView.findViewById(R.id.sanshoContainer)
        val ginoShoTextView: TextView = itemView.findViewById(R.id.ginoShoTextView)
        val kantoShoTextView: TextView = itemView.findViewById(R.id.kantoShoTextView)
        val shukunShoTextView: TextView = itemView.findViewById(R.id.shukunShoTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RikishiDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rikishi_detail, parent, false)
        return RikishiDetailViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RikishiDetailViewHolder, position: Int) {
        fun formatDate(dateString: String?): String {
            if (dateString.isNullOrEmpty()) return "unknown"
            return dateString.substringBefore("T")
        }

        holder.rikishiImageView?.let { imageView ->
            val imageUrl = RikishiImageHelper.getImageUrl(rikishi.nskId, formatDate(rikishi.birthDate))
            Log.d("ImageDebug", "Looking up image for nskId: ${rikishi.nskId}, birthDate: ${formatDate(rikishi.birthDate)}")
            Log.d("ImageDebug", "Found URL: $imageUrl")

            if (!imageUrl.isNullOrEmpty()) {
                Log.d("ImageDebug", "Loading image from URL: $imageUrl")
                imageView.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .into(imageView)
            } else {
                Log.d("ImageDebug", "No image URL found")
                imageView.visibility = View.GONE
            }
        }

        Log.d("ShikonaDebug", "Shikonas data: ${rikishi.shikonaHistory}")

        rikishi.shikonaHistory?.let { history ->  // Changed from shikonas to shikonaHistory
            if (history.isNotEmpty()) {
                val formattedHistory = history
                    .sortedBy { it.bashoId }
                    .mapNotNull { it.shikonaEn?.takeIf { name -> name.isNotBlank() } }
                    .distinct()
                    .joinToString(" → ")

                holder.shikonaHistoryTextView.text = Html.fromHtml(
                    "<b>Shikona History:</b> $formattedHistory",
                    Html.FROM_HTML_MODE_LEGACY
                )
                holder.shikonaHistoryTextView.visibility = View.VISIBLE
            } else {
                holder.shikonaHistoryTextView.visibility = View.GONE
            }
        } ?: run {
            holder.shikonaHistoryTextView.visibility = View.GONE
        }

        highestRank?.let {
            holder.highestRankTextView.text = Html.fromHtml(
                "<b>Highest Rank:</b> $it",
                Html.FROM_HTML_MODE_LEGACY
            )
            holder.highestRankTextView.visibility = View.VISIBLE
        } ?: run {
            holder.highestRankTextView.visibility = View.GONE
        }

        if (rikishi.intai != null || rikishi.currentRank?.contains("Retired") == true) {
            val retirementDate = formatDate(rikishi.intai)
            holder.rankTextView.text = Html.fromHtml("<b>Status</b>: Retired (since $retirementDate)", Html.FROM_HTML_MODE_LEGACY)
        } else {
            holder.rankTextView.text = Html.fromHtml("<b>Rank</b>: ${rikishi.currentRank ?: "unknown"}", Html.FROM_HTML_MODE_LEGACY)
        }

        holder.heyaTextView.text = Html.fromHtml("<b>Stable:</b> ${rikishi.heya ?: "unknown"}-beya", Html.FROM_HTML_MODE_LEGACY)
        holder.birthDateTextView.text = Html.fromHtml("<b>Birthdate</b>: ${formatDate(rikishi.birthDate)}", Html.FROM_HTML_MODE_LEGACY)
        holder.shusshinTextView.text = Html.fromHtml("<b>From</b>: ${rikishi.shusshin ?: "unknown"}", Html.FROM_HTML_MODE_LEGACY)
        holder.heightTextView.text = Html.fromHtml("<b>Height</b>: ${rikishi.height?.toInt() ?: "unknown"}cm", Html.FROM_HTML_MODE_LEGACY)
        holder.weightTextView.text = Html.fromHtml("<b>Weight</b>: ${rikishi.weight?.toInt() ?: "unknown"}kg", Html.FROM_HTML_MODE_LEGACY)

        rikishi.debut?.let {
            var formattedDebut = it.substringBefore("T")
            formattedDebut = when {
                formattedDebut.length >= 6 -> "${formattedDebut.substring(0, 4)}-${formattedDebut.substring(4, 6)}"
                else -> "unknown"
            }
            holder.debutTextView.text = Html.fromHtml("<b>Debut</b>: $formattedDebut", Html.FROM_HTML_MODE_LEGACY)
        } ?: run {
            holder.debutTextView.text = Html.fromHtml("<b>Debut</b>: unknown", Html.FROM_HTML_MODE_LEGACY)
        }

        // Stats section
        stats?.let {
            holder.statsSection.visibility = View.VISIBLE
            holder.totalWinsTextView.text = Html.fromHtml("<b>Win Ratio</b>: ${calculateWinPercentage(it)}%", Html.FROM_HTML_MODE_LEGACY)

            // Division stats table
            holder.careerBashoTextView.text = it.basho.toString()
            holder.makuuchiBashoTextView.text = it.bashoByDivision["Makuuchi"]?.toString() ?: "0"
            holder.juryoBashoTextView.text = it.bashoByDivision["Juryo"]?.toString() ?: "0"
            holder.makushitaBashoTextView.text = it.bashoByDivision["Makushita"]?.toString() ?: "0"
            holder.sandanmeBashoTextView.text = it.bashoByDivision["Sandanme"]?.toString() ?: "0"
            holder.jonidanBashoTextView.text = it.bashoByDivision["Jonidan"]?.toString() ?: "0"
            holder.jonokuchiBashoTextView.text = it.bashoByDivision["Jonokuchi"]?.toString() ?: "0"


            holder.careerYushoTextView.text = it.yusho.toString()
            holder.makuuchiYushoTextView.text = it.yushoByDivision["Makuuchi"]?.toString() ?: "0"
            holder.juryoYushoTextView.text = it.yushoByDivision["Juryo"]?.toString() ?: "0"
            holder.makushitaYushoTextView.text = it.yushoByDivision["Makushita"]?.toString() ?: "0"
            holder.sandanmeYushoTextView.text = it.yushoByDivision["Sandanme"]?.toString() ?: "0"
            holder.jonidanYushoTextView.text = it.yushoByDivision["Jonidan"]?.toString() ?: "0"
            holder.jonokuchiYushoTextView.text = it.yushoByDivision["Jonokuchi"]?.toString() ?: "0"

            // Add more divisions as needed

            holder.careerWinsTextView.text = it.totalWins.toString()
            holder.makuuchiWinsTextView.text = it.winsByDivision["Makuuchi"]?.toString() ?: "0"
            holder.juryoWinsTextView.text = it.winsByDivision["Juryo"]?.toString() ?: "0"
            holder.makushitaWinsTextView.text = it.winsByDivision["Makushita"]?.toString() ?: "0"
            holder.sandanmeWinsTextView.text = it.winsByDivision["Sandanme"]?.toString() ?: "0"
            holder.jonidanWinsTextView.text = it.winsByDivision["Jonidan"]?.toString() ?: "0"
            holder.jonokuchiWinsTextView.text = it.winsByDivision["Jonokuchi"]?.toString() ?: "0"
            // Add more divisions as needed

            holder.careerLossesTextView.text = it.totalLosses.toString()
            holder.makuuchiLossesTextView.text = it.lossByDivision["Makuuchi"]?.toString() ?: "0"
            holder.juryoLossesTextView.text = it.lossByDivision["Juryo"]?.toString() ?: "0"
            holder.makushitaLossesTextView.text = it.lossByDivision["Makushita"]?.toString() ?: "0"
            holder.sandanmeLossesTextView.text = it.lossByDivision["Sandanme"]?.toString() ?: "0"
            holder.jonidanLossesTextView.text = it.lossByDivision["Jonidan"]?.toString() ?: "0"
            holder.jonokuchiLossesTextView.text = it.lossByDivision["Jonokuchi"]?.toString() ?: "0"
            // Add more divisions as needed

            // Calculate totals (wins + losses)
            holder.careerTotalTextView.text = (it.totalWins + it.totalLosses).toString()
            holder.makuuchiTotalTextView.text = ((it.winsByDivision["Makuuchi"] ?: 0) + (it.lossByDivision["Makuuchi"] ?: 0)).toString()
            holder.juryoTotalTextView.text = ((it.winsByDivision["Juryo"] ?: 0) + (it.lossByDivision["Juryo"] ?: 0)).toString()
            holder.makushitaTotalTextView.text = ((it.winsByDivision["Makushita"] ?: 0) + (it.lossByDivision["Makushita"] ?: 0)).toString()
            holder.sandanmeTotalTextView.text = ((it.winsByDivision["Sandanme"] ?: 0) + (it.lossByDivision["Sandanme"] ?: 0)).toString()
            holder.jonidanTotalTextView.text = ((it.winsByDivision["Jonidan"] ?: 0) + (it.lossByDivision["Jonidan"] ?: 0)).toString()
            holder.jonokuchiTotalTextView.text = ((it.winsByDivision["Jonokuchi"] ?: 0) + (it.lossByDivision["Jonokuchi"] ?: 0)).toString()

        } ?: run {
            holder.statsSection.visibility = View.GONE
        }
        stats?.sansho?.let { sansho ->
            if (sansho.isNotEmpty()) {
                holder.sanshoTitleTextView.visibility = View.VISIBLE
                holder.sanshoContainer.visibility = View.VISIBLE

                sansho["Gino-sho"]?.let {
                    holder.ginoShoTextView.text = "Gino-sho: $it"
                    holder.ginoShoTextView.visibility = View.VISIBLE
                } ?: run { holder.ginoShoTextView.visibility = View.GONE }

                sansho["Kanto-sho"]?.let {
                    holder.kantoShoTextView.text = "Kanto-sho: $it"
                    holder.kantoShoTextView.visibility = View.VISIBLE
                } ?: run { holder.kantoShoTextView.visibility = View.GONE }

                sansho["Shukun-sho"]?.let {
                    holder.shukunShoTextView.text = "Shukun-sho: $it"
                    holder.shukunShoTextView.visibility = View.VISIBLE
                } ?: run { holder.shukunShoTextView.visibility = View.GONE }
            } else {
                holder.sanshoTitleTextView.visibility = View.GONE
                holder.sanshoContainer.visibility = View.GONE
            }
        } ?: run {
            holder.sanshoTitleTextView.visibility = View.GONE
            holder.sanshoContainer.visibility = View.GONE
        }
    }

    private fun calculateWinPercentage(stats: RikishiStats): String {
        return if (stats.totalMatches > 0) {
            "%.1f".format(stats.totalWins.toFloat() / stats.totalMatches.toFloat() * 100)
        } else {
            "0.0"
        }
    }

    override fun getItemCount(): Int = 1
}