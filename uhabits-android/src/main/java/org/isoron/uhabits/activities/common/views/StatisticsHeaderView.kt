package org.isoron.uhabits.activities.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import org.isoron.uhabits.utils.InterfaceUtils.dpToPixels

/** Keeps the title and interval selector side by side only while both fit. */
class StatisticsHeaderView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount != 2) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val available = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
        val title = getChildAt(0)
        val selector = getChildAt(1)
        val gap = dpToPixels(context, 8f).toInt()
        val childWidth = MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST)
        val childHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        title.measure(childWidth, childHeight)
        selector.measure(childWidth, childHeight)
        val stacked = title.measuredWidth + selector.measuredWidth + gap > available
        orientation = if (stacked) VERTICAL else HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        (title.layoutParams as LayoutParams).apply {
            width = if (stacked) LayoutParams.MATCH_PARENT else 0
            weight = if (stacked) 0f else 1f
            marginEnd = if (stacked) 0 else gap
            bottomMargin = if (stacked) gap else 0
        }
        (selector.layoutParams as LayoutParams).gravity = Gravity.END
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
