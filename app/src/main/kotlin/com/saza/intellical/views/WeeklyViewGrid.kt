package com.saza.intellical.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.saza.intellical.R
import com.saza.intellical.extensions.config
import com.saza.intellical.extensions.getWeeklyViewItemHeight

class WeeklyViewGrid(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private val ROWS_CNT = 24
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var daysCount = context.config.weeklyViewDays

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        paint.color = context.resources.getColor(R.color.divider_grey)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rowHeight = context.getWeeklyViewItemHeight()
        for (i in 0 until ROWS_CNT) {
            val y = rowHeight * i.toFloat() - i / 2
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }

        val rowWidth = width / daysCount.toFloat()
        for (i in 0 until daysCount) {
            val x = rowWidth * i.toFloat()
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
        }
    }
}
