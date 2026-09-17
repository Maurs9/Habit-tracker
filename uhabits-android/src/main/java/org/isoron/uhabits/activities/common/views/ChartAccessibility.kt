package org.isoron.uhabits.activities.common.views

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Timestamp
import java.text.DateFormat
import java.text.NumberFormat
import java.util.TimeZone

/** A paged native equivalent of the chart, rather than one uninspectable summary. */
class ChartData(
    val title: String,
    val size: () -> Int,
    val row: (Int) -> String,
    val initialPosition: () -> Int = { 0 },
    val onSelect: ((Int) -> Unit)? = null
)

fun View.setChartData(data: ChartData) {
    val controller = getTag(R.id.chart_accessibility_controller) as? ChartAccessibility
        ?: ChartAccessibility(this).also { setTag(R.id.chart_accessibility_controller, it) }
    controller.data = data
    controller.refresh()
}

fun View.refreshChartAccessibility() {
    (getTag(R.id.chart_accessibility_controller) as? ChartAccessibility)?.refresh()
}

fun View.chartSummary(): String {
    val data = (getTag(R.id.chart_accessibility_controller) as? ChartAccessibility)?.data
        ?: return context.getString(R.string.chart_no_data)
    return "${data.title}. " + if (data.size() == 0) {
        context.getString(R.string.chart_no_data)
    } else {
        data.row(data.initialPosition().coerceIn(0, data.size() - 1))
    }
}

internal class ChartAccessibility(private val view: View) {
    lateinit var data: ChartData
    var dialog: AlertDialog? = null
        private set
    private var list: ListView? = null
    private var pageStart = 0

    init {
        view.isFocusable = true
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        view.setOnClickListener { showData() }
        ViewCompat.setAccessibilityDelegate(
            view,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = android.widget.Button::class.java.name
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_CLICK,
                            host.context.getString(R.string.chart_show_data)
                        )
                    )
                }
            }
        )
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                dialog?.dismiss()
            }
        })
    }

    fun refresh() {
        val count = data.size()
        val summary = if (count == 0) {
            view.context.getString(R.string.chart_no_data)
        } else {
            data.row(data.initialPosition().coerceIn(0, count - 1))
        }
        view.contentDescription = view.context.getString(R.string.chart_description, data.title, summary)
        if (dialog != null) refreshPage()
    }

    private fun showData() {
        if (dialog != null) return
        pageStart = data.initialPosition().coerceIn(0, (data.size() - 1).coerceAtLeast(0))
        list = ListView(view.context).apply {
            id = R.id.chart_data_list
            if (data.onSelect != null) {
                setOnItemClickListener { _, _, position, _ ->
                    val index = pageStart + position
                    if (index in 0 until data.size()) data.onSelect?.invoke(index)
                }
            }
        }
        val alert = AlertDialog.Builder(view.context)
            .setTitle(data.title)
            .setView(list)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.chart_previous_page, null)
            .setPositiveButton(R.string.chart_next_page, null)
            .create()
        dialog = alert
        alert.setOnDismissListener {
            dialog = null
            list = null
            if (view.isAttachedToWindow) {
                view.requestFocus()
            }
        }
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                pageStart = (pageStart - PAGE_SIZE).coerceAtLeast(0)
                refreshPage()
            }
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                pageStart += PAGE_SIZE
                refreshPage()
            }
            refreshPage()
        }
        alert.show()
    }

    private fun refreshPage() {
        val alert = dialog ?: return
        val count = data.size()
        pageStart = pageStart.coerceIn(0, (count - 1).coerceAtLeast(0))
        val end = (pageStart + PAGE_SIZE).coerceAtMost(count)
        val rows = if (count == 0) {
            listOf(view.context.getString(R.string.chart_no_data))
        } else {
            (pageStart until end).map { index ->
                val row = data.row(index)
                if (data.onSelect != null) view.context.getString(R.string.chart_edit_entry, row) else row
            }
        }
        list?.adapter = object : ArrayAdapter<String>(view.context, android.R.layout.simple_list_item_1, rows) {
            override fun areAllItemsEnabled() = count > 0
            override fun isEnabled(position: Int) = count > 0
        }
        list?.isClickable = data.onSelect != null && count > 0
        alert.setTitle(
            if (count == 0) {
                data.title
            } else {
                view.context.getString(R.string.chart_page_title, data.title, pageStart + 1, end)
            }
        )
        alert.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = pageStart > 0
        alert.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = end < count
        list?.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }

    companion object {
        const val PAGE_SIZE = 31
    }
}

fun Context.chartDate(timestamp: Timestamp): String =
    DateFormat.getDateInstance(DateFormat.FULL, resources.configuration.locales[0]).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(timestamp.toJavaDate())

fun Context.chartNumber(value: Double): String =
    NumberFormat.getNumberInstance(resources.configuration.locales[0]).apply {
        maximumFractionDigits = 3
    }.format(value)

fun Context.chartPeriod(bucketSize: Int): String {
    val index = when (bucketSize) {
        1 -> 0
        7 -> 1
        31 -> 2
        92 -> 3
        else -> 4
    }
    return resources.getStringArray(R.array.strengthIntervalNames)[index]
}

fun Context.chartPercent(value: Double): String =
    NumberFormat.getPercentInstance(resources.configuration.locales[0]).apply {
        maximumFractionDigits = 1
    }.format(value)

fun Context.chartWidgetDescription(habitName: String, summary: String, action: Int = R.string.chart_widget_open): String =
    getString(R.string.chart_widget_description, habitName, summary, getString(action))

fun Context.chartEntry(entry: Entry, numerical: Boolean, unit: String): String {
    val state = when (entry.value) {
        Entry.UNKNOWN -> getString(R.string.chart_unknown)
        Entry.SKIP -> getString(R.string.chart_skipped)
        else -> if (numerical) {
            if (entry.value == Entry.NUMERICAL_AUTO) {
                getString(R.string.chart_completed_automatically)
            } else {
                getString(R.string.chart_value_unit, chartNumber(entry.value / 1000.0), unit).trim()
            }
        } else {
            getString(
                when (entry.value) {
                    Entry.YES_MANUAL -> R.string.chart_completed
                    Entry.YES_AUTO -> R.string.chart_completed_automatically
                    else -> R.string.chart_not_completed
                }
            )
        }
    }
    val text = getString(R.string.chart_date_value, chartDate(entry.timestamp), state)
    return if (entry.notes.isBlank()) text else getString(R.string.chart_with_notes, text, entry.notes)
}
