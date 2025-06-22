package a48626.sumolmbao

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import a48626.sumolmbao.R // Import your R file to access custom attributes

class MaxHeightRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var maxHeight = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightRecyclerView, defStyleAttr, 0)
        try {
            maxHeight = a.getDimensionPixelSize(R.styleable.MaxHeightRecyclerView_maxHeight, (240 * context.resources.displayMetrics.density).toInt())
        } finally {
            a.recycle()
        }
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