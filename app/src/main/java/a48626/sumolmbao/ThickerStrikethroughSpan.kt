package a48626.sumolmbao

import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.text.style.ReplacementSpan
import android.text.style.StrikethroughSpan

class ThickerStrikethroughSpan : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val originalStyle = paint.style
        val originalStrokeWidth = paint.strokeWidth
        val originalColor = paint.color

        // Draw normal text first
        canvas.drawText(text, start, end, x, y.toFloat(), paint)

        // Draw thick strikethrough
        val textWidth = paint.measureText(text, start, end)
        val strikeY = (y - paint.ascent() * 0.5f)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawLine(x, strikeY, x + textWidth, strikeY, paint)

        // Restore paint
        paint.style = originalStyle
        paint.strokeWidth = originalStrokeWidth
        paint.color = originalColor
    }
}

