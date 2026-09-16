package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.ListDensity
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.dp

class SectionHeaderView(context: Context) : LinearLayout(context) {
    val nameView = label()
    val countView = label()
    private var isFirstSection = true
    private val dividerPaint = Paint().apply {
        color = StyledResources(context).getColor(R.attr.contrast20)
    }
    private val dividerHeight = dp(1f)
    private val dividerTop = dp(8f)
    var listDensity = ListDensity.STANDARD
        set(value) {
            if (field == value) return
            field = value
            updatePadding()
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setWillNotDraw(false)
        updatePadding()
        nameView.apply {
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(StyledResources(context).getColor(R.attr.contrast80))
        }
        addView(nameView, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(countView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        ViewCompat.setAccessibilityHeading(this, true)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        nameView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        countView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun bind(header: HabitCardListCache.ListItem.Header) {
        isFirstSection = header.isFirstSection
        updatePadding()
        invalidate()
        val name = if (header.sectionId == null) context.getString(R.string.section_other) else header.name
        nameView.text = name
        countView.text = context.getString(R.string.section_header_count, header.done, header.total)
        countView.visibility = if (header.total == 0) GONE else VISIBLE
        contentDescription = context.getString(R.string.section_header_description, name, header.done, header.total)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isFirstSection) {
            canvas.drawRect(
                paddingLeft.toFloat(),
                dividerTop,
                (width - paddingRight).toFloat(),
                dividerTop + dividerHeight,
                dividerPaint
            )
        }
    }

    private fun updatePadding() {
        setPaddingRelative(
            dp(34f).toInt(),
            dp((if (isFirstSection) listDensity.firstSectionTopDp else listDensity.sectionTopDp).toFloat()).toInt(),
            dp(16f).toInt(),
            dp(listDensity.sectionBottomDp.toFloat()).toInt()
        )
    }

    private fun label() = TextView(context).apply {
        val styled = StyledResources(context)
        isAllCaps = true
        letterSpacing = 0.08f
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.smallerTextSize))
        setTextColor(styled.getColor(R.attr.contrast60))
    }
}

class SectionHeaderViewHolder(view: SectionHeaderView) : RecyclerView.ViewHolder(view)
