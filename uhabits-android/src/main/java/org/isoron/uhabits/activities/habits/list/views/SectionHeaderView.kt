package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.isoron.uhabits.R
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.isoron.uhabits.utils.StyledResources

class SectionHeaderView(context: Context) : LinearLayout(context) {
    val nameView = label()
    val countView = label()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
        setPaddingRelative(dp(34), dp(12), dp(16), dp(4))
        addView(nameView, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(countView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        ViewCompat.setAccessibilityHeading(this, true)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        nameView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        countView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun bind(header: HabitCardListCache.ListItem.Header) {
        val name = if (header.sectionId == null) context.getString(R.string.section_other) else header.name
        nameView.text = name
        countView.text = context.getString(R.string.section_header_count, header.done, header.total)
        countView.visibility = if (header.total == 0) GONE else VISIBLE
        contentDescription = context.getString(R.string.section_header_description, name, header.done, header.total)
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
