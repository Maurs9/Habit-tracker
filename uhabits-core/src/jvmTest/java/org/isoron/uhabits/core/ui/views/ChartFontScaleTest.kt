package org.isoron.uhabits.core.ui.views

import org.isoron.platform.gui.Canvas
import org.isoron.platform.gui.Color
import org.isoron.platform.gui.Font
import org.isoron.platform.gui.Image
import org.isoron.platform.gui.TextAlign
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.models.PaletteColor
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChartFontScaleTest {
    @Test
    fun historyWidensCellsForScaledNumbersAndKeepsDateHitTestingAligned() {
        val today = LocalDate(2020, 6, 20)
        var clicked: LocalDate? = null
        val chart = HistoryChart(
            JavaLocalDateFormatter(Locale.US), DayOfWeek.SUNDAY, PaletteColor(0),
            List(200) { HistoryChart.Square.ON }, HistoryChart.Square.OFF,
            emptyList(), LightTheme(), today,
            object : OnDateClickedListener {
                override fun onDateShortPress(date: LocalDate) { clicked = date }
            }
        )
        val normal = RecordingCanvas(400.0, 200.0, 1.0)
        chart.draw(normal)
        val originalColumnWidth = chart.dataColumnWidth
        assertEquals(26.0, originalColumnWidth)
        val enlarged = RecordingCanvas(400.0, 200.0, 2.0)
        chart.draw(enlarged)
        assertTrue(chart.dataColumnWidth > originalColumnWidth)
        assertTrue(enlarged.labels.all { it.fontSize == 24.0 })
        enlarged.assertLabelsFit()
        val dates = enlarged.labels.filter { it.text.length <= 2 && it.text.toIntOrNull() != null }
        val first = dates.first()
        val nextColumn = dates.first { it.y == first.y && it.x > first.x }
        chart.onClick(first.x, first.y)
        val firstDate = assertNotNull(clicked)
        assertEquals(first.text.toInt(), firstDate.day)
        chart.onClick(nextColumn.x, nextColumn.y)
        assertEquals(7, firstDate.distanceTo(assertNotNull(clicked)))
    }

    @Test
    fun barReservesScaledValueWidthsAndFooterSpace() {
        val today = LocalDate(2020, 6, 20)
        val chart = BarChart(LightTheme(), JavaLocalDateFormatter(Locale.US)).apply {
            series = mutableListOf((1..30).map { it.toDouble() })
            colors = mutableListOf(Color.BLUE)
            axis = (0 until 30).map { today.minus(it) }
        }
        chart.draw(RecordingCanvas(400.0, 220.0, 1.0))
        assertEquals(18.0, chart.dataColumnWidth)
        val enlarged = RecordingCanvas(400.0, 220.0, 2.0)
        chart.draw(enlarged)
        assertTrue(chart.dataColumnWidth > 18.0)
        assertTrue(enlarged.labels.isNotEmpty())
        assertTrue(enlarged.labels.all { it.fontSize == 20.0 })
        enlarged.assertLabelsFit()
        assertTrue(enlarged.labels.any { it.text == "2020" })
    }

    private class RecordingCanvas(
        private val width: Double,
        private val height: Double,
        private val scale: Double
    ) : Canvas {
        data class Label(
            val text: String,
            val x: Double,
            val y: Double,
            val width: Double,
            val fontSize: Double,
            val align: TextAlign
        )
        val labels = mutableListOf<Label>()
        private var size = 0.0
        private var align = TextAlign.CENTER
        override fun getWidth() = width
        override fun getHeight() = height
        override fun getScaledFontSize(size: Double) = size * scale
        override fun setFontSize(size: Double) { this.size = getScaledFontSize(size) }
        override fun measureText(text: String) = text.length * size * 0.55
        override fun setTextAlign(align: TextAlign) { this.align = align }
        override fun drawText(text: String, x: Double, y: Double) {
            if (text.isNotEmpty()) labels.add(Label(text, x, y, measureText(text), size, align))
        }
        fun assertLabelsFit() {
            for (label in labels) {
                val left = when (label.align) {
                    TextAlign.LEFT -> label.x
                    TextAlign.CENTER -> label.x - label.width / 2
                    TextAlign.RIGHT -> label.x - label.width
                }
                assertTrue(left >= 0 && left + label.width <= width, label.toString())
                assertTrue(label.y - label.fontSize / 2 >= 0 && label.y + label.fontSize / 2 <= height, label.toString())
            }
        }
        override fun setColor(color: Color) = Unit
        override fun setFont(font: Font) = Unit
        override fun setStrokeWidth(size: Double) = Unit
        override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) = Unit
        override fun fillRect(x: Double, y: Double, width: Double, height: Double) = Unit
        override fun fillRoundRect(x: Double, y: Double, width: Double, height: Double, cornerRadius: Double) = Unit
        override fun drawRect(x: Double, y: Double, width: Double, height: Double) = Unit
        override fun fillCircle(centerX: Double, centerY: Double, radius: Double) = Unit
        override fun fillArc(centerX: Double, centerY: Double, radius: Double, startAngle: Double, swipeAngle: Double) = Unit
        override fun toImage(): Image = error("Not used by geometry tests")
    }
}
