package org.isoron.uhabits.activities.common.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.platform.gui.AndroidCanvas
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.show.views.BarCardView
import org.isoron.uhabits.activities.habits.show.views.ScoreCardView
import org.isoron.uhabits.core.ui.screens.habits.show.views.BarCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.ScoreCardPresenter
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.widgets.FrequencyWidget
import org.isoron.uhabits.widgets.HistoryWidget
import org.isoron.uhabits.widgets.ScoreWidget
import org.isoron.uhabits.widgets.WidgetDimensions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsAdaptivityTest : BaseAndroidTest() {
    private fun context(scale: Float, theme: Int = R.style.AppBaseTheme): Context {
        val config = Configuration(targetContext.resources.configuration).apply {
            fontScale = scale
            densityDpi = 320
        }
        StyledResources.setFixedTheme(theme)
        return ContextThemeWrapper(targetContext.createConfigurationContext(config), theme)
    }

    private fun measure(view: View, widthDp: Int) {
        val width = (widthDp * view.resources.displayMetrics.density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    private fun bounds(parent: ViewGroup, child: View): Rect =
        Rect(0, 0, child.width, child.height).also { parent.offsetDescendantRectToMyCoords(child, it) }

    private fun assertHeader(card: ViewGroup, selector: Spinner, chart: View) {
        val minimum = 48 * card.resources.displayMetrics.density
        assertTrue(selector.width >= minimum)
        assertTrue(selector.height >= minimum)
        val title = card.findViewById<TextView>(R.id.title)
        val titleBounds = bounds(card, title)
        val selectorBounds = bounds(card, selector)
        val header = card.findViewById<StatisticsHeaderView>(R.id.chartHeader)
        assertTrue(!Rect.intersects(titleBounds, selectorBounds))
        assertTrue(bounds(card, header).bottom <= bounds(card, chart).top)
        assertTrue(selectorBounds.left >= 0 && selectorBounds.right <= card.width)
        assertTrue(title.layout.getLineBottom(title.lineCount - 1) <= title.height - title.paddingBottom)
    }

    @Test
    fun selectorsHaveReal48DpTargetsAndHeadersReflowAtLargeFonts() {
        val boolean = fixtures.createLongHabit()
        val numerical = fixtures.createLongNumericalHabit()
        for (scale in listOf(1f, 2f)) {
            for (width in listOf(240, 600)) {
                val ctx = context(scale)
                val root = LayoutInflater.from(ctx).inflate(R.layout.show_habit, null)
                val score = root.findViewById<ScoreCardView>(R.id.scoreCard)
                score.setState(ScoreCardPresenter.buildState(boolean, 1, 0, LightTheme()))
                measure(score, width)
                assertHeader(score, score.findViewById(R.id.spinner), score.findViewById(R.id.scoreView))
                for (habit in listOf(boolean, numerical)) {
                    val otherRoot = LayoutInflater.from(ctx).inflate(R.layout.show_habit, null)
                    val bar = otherRoot.findViewById<BarCardView>(R.id.barCard)
                    bar.setState(BarCardPresenter.buildState(habit, 1, 0, 0, LightTheme()))
                    measure(bar, width)
                    val id = if (habit.isNumerical) R.id.numericalSpinner else R.id.boolSpinner
                    assertHeader(bar, bar.findViewById(id), bar.findViewById(R.id.chart))
                }
                if (scale == 2f && width == 240) {
                    score.findViewById<TextView>(R.id.title).text = "Habit strength over a longer period"
                    measure(score, width)
                    assertEquals(LinearLayout.VERTICAL, score.findViewById<StatisticsHeaderView>(R.id.chartHeader).orientation)
                    assertHeader(score, score.findViewById(R.id.spinner), score.findViewById(R.id.scoreView))
                }
            }
        }
    }

    @Test
    fun canvasUsesPlatformSpConversionWithoutChangingDrawingDensity() {
        for (scale in listOf(1f, 2f)) {
            val ctx = context(scale)
            val canvas = AndroidCanvas().apply {
                context = ctx
                innerDensity = 3.0
                innerWidth = 300
                innerHeight = 600
            }
            canvas.setFontSize(12.0)
            val expected = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, ctx.resources.displayMetrics)
            assertEquals(expected / ctx.resources.displayMetrics.density * 3, canvas.textPaint.textSize, 0.001f)
            assertEquals(100.0, canvas.getWidth())
            assertEquals(200.0, canvas.getHeight())
        }
    }

    @Test
    fun frequencyScalesTextAndReservesRowsAndTwoFooterLines() {
        for (scale in listOf(1f, 2f)) {
            val ctx = context(scale)
            val chart = FrequencyChart(ctx).apply {
                minimumHeight = (200 * resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            measure(chart, 240)
            val paintField = FrequencyChart::class.java.getDeclaredField("pText").apply { isAccessible = true }
            val paint = paintField.get(chart) as Paint
            val expected = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, ctx.resources.displayMetrics)
            assertEquals(expected, paint.textSize, 0.001f)
            if (scale == 2f) {
                val rowField = FrequencyChart::class.java.getDeclaredField("baseSize").apply { isAccessible = true }
                val row = rowField.getInt(chart)
                assertTrue(row >= paint.fontSpacing)
                assertTrue(7 * row - paint.ascent() + paint.fontSpacing + paint.descent() <= chart.height)
            }
        }
    }

    @Test
    fun scoreScalesTextWithoutClippingYearLabels() {
        for (scale in listOf(1f, 2f)) {
            val ctx = context(scale)
            val chart = ScoreChart(ctx)
            val density = ctx.resources.displayMetrics.density
            val width = (240 * density).toInt()
            val height = (220 * density).toInt()
            chart.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            chart.layout(0, 0, width, height)
            val paint = ScoreChart::class.java.getDeclaredField("pText").apply { isAccessible = true }.get(chart) as Paint
            val expected = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, ctx.resources.displayMetrics)
            assertEquals(expected, paint.textSize, 0.001f)
            if (scale == 2f) {
                val columnWidth = ScoreChart::class.java.getDeclaredField("columnWidth").apply { isAccessible = true }.getFloat(chart)
                val columnHeight = ScoreChart::class.java.getDeclaredField("columnHeight").apply { isAccessible = true }.getInt(chart)
                val top = ScoreChart::class.java.getDeclaredField("internalPaddingTop").apply { isAccessible = true }.getInt(chart)
                assertTrue(paint.measureText("8888") <= columnWidth)
                assertTrue(top + columnHeight + 2.2f * paint.fontSpacing + paint.descent() <= height)
            }
        }
    }

    @Test
    fun widgetBitmapsKeepExactHostDimensionsAtBothFontScales() {
        val habit = fixtures.createLongHabit()
        for (scale in listOf(1f, 2f)) {
            val ctx = context(scale, R.style.WidgetTheme)
            val widgets = listOf(
                HistoryWidget(ctx, 0, habit),
                FrequencyWidget(ctx, 0, habit, prefs.firstWeekdayInt),
                ScoreWidget(ctx, 0, habit)
            )
            for (widget in widgets) {
                widget.setDimensions(WidgetDimensions(200, 200, 300, 160))
                for ((remote, size) in listOf(
                    widget.portraitRemoteViews to (200 to 200),
                    widget.landscapeRemoteViews to (300 to 160)
                )) {
                    val root = remote.apply(ctx, FrameLayout(ctx))
                    val bitmap = (root.findViewById<ImageView>(R.id.imageView).drawable as BitmapDrawable).bitmap
                    assertEquals(size.first, bitmap.width)
                    assertEquals(size.second, bitmap.height)
                }
            }
        }
    }

    @Test
    fun enabledCalendarEditHasAaContrastInEveryTheme() {
        for (theme in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            val ctx = context(1f, theme)
            val root = LayoutInflater.from(ctx).inflate(R.layout.show_habit, null)
            val edit = root.findViewById<Button>(R.id.edit)
            assertTrue(edit.isEnabled)
            val background = StyledResources(ctx).getColor(R.attr.cardBgColor)
            assertTrue(ColorUtils.calculateContrast(edit.currentTextColor, background) >= 4.5)
        }
    }
}
