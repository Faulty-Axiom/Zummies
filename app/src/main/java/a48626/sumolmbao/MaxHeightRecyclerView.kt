package a48626.sumolmbao

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MaxHeightRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var maxHeight = 0

    init {
        // Convert 240dp to pixels
        maxHeight = (240 * context.resources.displayMetrics.density).toInt()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        var newHeightSpec = heightSpec

        // First measure with the original height spec to get child measurements
        super.onMeasure(widthSpec, heightSpec)

        if (measuredHeight > maxHeight) {
            // If content is taller than max height, use max height with scroll
            newHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        } else {
            // Otherwise, use the measured height (wrap content behavior)
            newHeightSpec = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthSpec, newHeightSpec)
    }
}