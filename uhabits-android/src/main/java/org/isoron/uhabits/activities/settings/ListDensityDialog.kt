package org.isoron.uhabits.activities.settings

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.views.SectionHeaderView
import org.isoron.uhabits.core.preferences.ListDensity
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache.ListItem.Header
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres

@get:StringRes
val ListDensity.labelResId: Int
    get() = when (this) {
        ListDensity.COMPACT -> R.string.list_density_compact
        ListDensity.STANDARD -> R.string.list_density_standard
        ListDensity.SPACIOUS -> R.string.list_density_spacious
    }

private val ListDensity.radioId: Int
    get() = when (this) {
        ListDensity.COMPACT -> R.id.list_density_compact
        ListDensity.STANDARD -> R.id.list_density_standard
        ListDensity.SPACIOUS -> R.id.list_density_spacious
    }

class ListDensityDialog : DialogFragment() {
    private val preferences
        get() = (requireContext().applicationContext as HabitsApplication).component.preferences
    private var selectedDensity = ListDensity.STANDARD

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        selectedDensity = savedInstanceState?.getString("density")?.let { ListDensity.fromPersistedName(it) }
            ?: preferences.listDensity
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.HabitControlsDialogTheme)
        val content = LinearLayout(builder.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f).toInt(), 0, dp(24f).toInt(), dp(8f).toInt())
        }
        val preview = LinearLayout(builder.context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(sres.getColor(R.attr.windowBackgroundColor))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        val choices = RadioGroup(builder.context).apply {
            for (density in ListDensity.entries) {
                addView(
                    RadioButton(context).apply {
                        id = density.radioId
                        setText(density.labelResId)
                        minHeight = dp(48f).toInt()
                    },
                    RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT)
                )
            }
            check(selectedDensity.radioId)
            setOnCheckedChangeListener { _, checkedId ->
                selectedDensity = ListDensity.entries.first { it.radioId == checkedId }
                updatePreview(preview)
            }
        }
        content.addView(TextView(builder.context).apply { setText(R.string.list_density_description) })
        content.addView(choices)
        content.addView(
            TextView(builder.context).apply {
                setText(R.string.list_density_preview)
                setPadding(0, dp(8f).toInt(), 0, dp(4f).toInt())
            }
        )
        content.addView(preview)
        updatePreview(preview)
        return builder.setTitle(R.string.list_density_title)
            .setView(ScrollView(builder.context).apply { addView(content) })
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ -> preferences.listDensity = selectedDensity }
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("density", selectedDensity.name)
    }

    private fun updatePreview(preview: LinearLayout) {
        preview.removeAllViews()
        repeat(2) { index ->
            preview.addView(
                SectionHeaderView(preview.context).apply {
                    listDensity = selectedDensity
                    bind(Header(index.toLong(), getString(R.string.habit_section), 0, 1, isFirstSection = index == 0))
                }
            )
            preview.addView(
                LinearLayout(preview.context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(3f).toInt(), 0, dp(3f).toInt(), dp((selectedDensity.rowGapDp - 1).toFloat()).toInt())
                    addView(
                        TextView(context).apply {
                            setText(R.string.habit)
                            gravity = Gravity.CENTER_VERTICAL
                            minHeight = dp(selectedDensity.rowHeightDp.toFloat()).toInt()
                            setPaddingRelative(dp(31f).toInt(), 0, dp(16f).toInt(), 0)
                            setTextColor(sres.getColor(R.attr.contrast100))
                            setBackgroundColor(sres.getColor(R.attr.cardBgColor))
                        },
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    )
                    addView(
                        View(context).apply { setBackgroundColor(sres.getColor(R.attr.habitRowDividerColor)) },
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f).toInt())
                    )
                }
            )
        }
    }

    companion object {
        const val TAG = "list-density"
    }
}
