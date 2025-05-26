package a48626.sumolmbao.third_fragment

import a48626.sumolmbao.R
import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream

object RikishiImageHelper {
    private val imageMap = mutableMapOf<Int, String>()  // nskId -> URL (from rikishi_images.csv)
    private val dateImageMap = mutableMapOf<String, String>()  // full date -> URL (from rikishi_images.csv)
    private val goatsDateMap = mutableMapOf<String, String>()  // full date -> URL (from goats.csv)

    fun initialize(context: Context) {
        try {
            // Load regular rikishi images
            context.resources.openRawResource(R.raw.rikishi_images).use { inputStream ->
                inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(',')
                        if (parts.size >= 3) {
                            val nskId = parts[0].trim().toIntOrNull() ?: 0
                            val date = parts[1].trim()
                            val imageUrl = parts[2].trim()

                            if (nskId != 0) {
                                imageMap[nskId] = imageUrl
                            }
                            dateImageMap[date] = imageUrl
                        }
                    }
                }
            }

            // Load goats images
            context.resources.openRawResource(R.raw.goats).use { inputStream ->
                inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(',')
                        if (parts.size >= 3) {
                            val date = parts[1].trim()  // Ignore the ID column
                            val imageUrl = parts[2].trim()
                            goatsDateMap[date] = imageUrl
                        }
                    }
                }
            }

            Log.d("CSVLoad", "Loaded ${imageMap.size} nskId entries, ${dateImageMap.size} date entries, and ${goatsDateMap.size} goats entries")
        } catch (e: Exception) {
            Log.e("CSVLoad", "Error loading CSV", e)
        }
    }

    fun getImageUrl(nskId: Int, birthDate: String?): String? {
        // Case 1: Has valid nskId - only check nskId map (from main CSV)
        if (nskId != 0) {
            return imageMap[nskId]?.also { url ->
                Log.d("ImageLookup", "Found by nskId: $nskId -> $url")
            }
        }

        // Case 2: No nskId - check date in both CSVs (main first, then goats)
        val dateKey = birthDate?.substringBefore('T') ?: return null

        // First try main CSV
        dateImageMap[dateKey]?.let { url ->
            Log.d("ImageLookup", "Found by date in main CSV: $dateKey -> $url")
            return url
        }

        // Then try goats CSV
        goatsDateMap[dateKey]?.let { url ->
            Log.d("ImageLookup", "Found by date in goats CSV: $dateKey -> $url")
            return url
        }

        Log.d("ImageLookup", "No image found for nskId: $nskId or date: $dateKey")
        return null
    }
}