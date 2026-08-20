package com.selflimit.instagramtimer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.selflimit.instagramtimer.R
import com.selflimit.instagramtimer.data.TimeWindow
import com.selflimit.instagramtimer.util.TimeSlots

/**
 * Vertical day agenda: 00:00 at the top, 24:00 at the bottom. Each configured time
 * window is drawn as a block positioned/sized by its time range, colored by whether
 * its cap still has time left, with a line marking the current time.
 */
class DayScheduleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Entry(val window: TimeWindow, val usedMinutes: Int)

    private var entries: List<Entry> = emptyList()
    private var currentMinute: Int = 0

    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private val backgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.day_background)
    }
    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.day_grid_line)
        strokeWidth = dp(1f)
    }
    private val availablePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.status_granted)
        alpha = WINDOW_BLOCK_ALPHA
    }
    private val usedUpPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.status_not_granted)
        alpha = WINDOW_BLOCK_ALPHA
    }
    private val nowPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.now_indicator)
        strokeWidth = dp(2f)
    }
    private val labelPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.day_grid_label)
        textSize = dp(11f)
        isAntiAlias = true
    }
    private val windowLabelPaint = Paint().apply {
        color = Color.WHITE
        textSize = dp(12f)
        isAntiAlias = true
    }
    private val windowLabelPaintRight = Paint(windowLabelPaint).apply {
        textAlign = Paint.Align.RIGHT
    }

    fun setData(entries: List<Entry>, currentMinute: Int) {
        this.entries = entries
        this.currentMinute = currentMinute
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val left = dp(40f)
        val right = w - dp(8f)
        val topPad = dp(14f)
        val bottomPad = dp(14f)
        val dayHeight = h - topPad - bottomPad
        fun y(minuteOfDay: Int) = topPad + dayHeight * (minuteOfDay / 1440f)

        canvas.drawRect(left, topPad, right, h - bottomPad, backgroundPaint)

        for (hour in 0..24 step 3) {
            val lineY = y(hour * 60)
            canvas.drawLine(left, lineY, right, lineY, gridPaint)
            canvas.drawText(String.format("%02d:00", hour), 0f, lineY + dp(4f), labelPaint)
        }

        val inset = dp(4f)
        for (entry in entries) {
            val window = entry.window
            val top = y(window.startMinute)
            val bottom = y(window.endMinute)
            val remaining = window.capMinutes - entry.usedMinutes
            val paint = if (remaining <= 0) usedUpPaint else availablePaint
            val rect = RectF(left + inset, top + dp(2f), right - inset, bottom - dp(2f))
            canvas.drawRect(rect, paint)

            if (rect.height() >= dp(18f)) {
                val timeLabel = "${TimeSlots.label(window.startMinute)}-${TimeSlots.label(window.endMinute)}"
                val remainingLabel = "${remaining.coerceAtLeast(0)}/${window.capMinutes} min used"
                val textY = rect.top + dp(15f)
                canvas.drawText(timeLabel, rect.left + dp(6f), textY, windowLabelPaint)
                canvas.drawText(remainingLabel, rect.right - dp(6f), textY, windowLabelPaintRight)
            }
        }

        val nowY = y(currentMinute)
        canvas.drawLine(left, nowY, right, nowY, nowPaint)
    }

    companion object {
        private const val WINDOW_BLOCK_ALPHA = 153 // ~0.6 opacity, so grid lines remain visible through blocks
    }
}
