package org.isoron.uhabits.activities.habits.list.views

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.DaggerHabitsActivityTestComponent
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.ListDensity
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache.ListItem.Header
import org.isoron.uhabits.core.utils.ColorContrast
import org.isoron.uhabits.inject.ActivityContextModule
import org.isoron.uhabits.utils.StyledResources
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class HabitListAppearanceTest : BaseViewTest() {
    @Test
    fun testHabitNamesMeetContrastForEveryPaletteColorAndSelectionState() {
        val habit = fixtures.createLongHabit()
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            withThemedActivity(style) { activity, factory ->
                val card = factory.create()
                val inner = card.getChildAt(0) as ViewGroup
                val label = (0 until inner.childCount).map { inner.getChildAt(it) }.filterIsInstance<TextView>().single()
                val styled = StyledResources(activity)
                for (index in 0 until PaletteColor.COUNT) {
                    habit.color = PaletteColor(index)
                    card.habit = habit
                    for (selected in listOf(false, true, false)) {
                        card.isSelected = selected
                        val background = styled.getColor(
                            if (selected) R.attr.highlightedBackgroundColor else R.attr.cardBgColor
                        )
                        assertTrue(
                            "Palette $index, style $style, selected=$selected",
                            ColorContrast.ratio(label.currentTextColor, background) >= 4.5
                        )
                        assertEquals(PaletteColor(index), habit.color)
                    }
                }
            }
        }
    }

    @Test
    fun testRenderLight() = render(R.style.AppBaseTheme, "light.png")

    @Test
    fun testRenderDark() = render(R.style.AppBaseThemeDark, "dark.png")

    @Test
    fun testRenderPureBlack() = render(R.style.AppBaseThemeDark_PureBlack, "pure_black.png")

    @Test
    fun testRenderCompactLight() = render(R.style.AppBaseTheme, "compact_light.png", ListDensity.COMPACT)

    @Test
    fun testRenderCompactDark() = render(R.style.AppBaseThemeDark, "compact_dark.png", ListDensity.COMPACT)

    @Test
    fun testRenderCompactPureBlack() = render(R.style.AppBaseThemeDark_PureBlack, "compact_pure_black.png", ListDensity.COMPACT)

    @Test
    fun testDensityChangesResizeBothEntryTypesWithoutChangingDateColumns() {
        val habits = listOf(fixtures.createLongHabit(), fixtures.createLongNumericalHabit())
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            withThemedActivity(style) { _, factory ->
                for (habit in habits) {
                    val card = factory.create().apply {
                        this.habit = habit
                        buttonCount = 5
                        isSelected = false
                    }
                    for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                        card.layoutDirection = direction
                        for (density in ListDensity.entries) {
                            card.listDensity = density
                            card.measure(
                                MeasureSpec.makeMeasureSpec(dpToPixels(400).toInt(), MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                            )
                            card.layout(0, 0, card.measuredWidth, card.measuredHeight)
                            assertEquals(dpToPixels(density.rowHeightDp + density.rowGapDp).toInt(), card.height)
                            val inner = card.getChildAt(0) as ViewGroup
                            val panel = (0 until inner.childCount).map { inner.getChildAt(it) }
                                .filterIsInstance<ButtonPanelView<*>>().single { it.visibility == View.VISIBLE }
                            assertEquals(dpToPixels(density.rowHeightDp).toInt(), panel.height)
                            assertEquals(dpToPixels(48 * 5).toInt(), panel.width)
                            for (button in panel.buttons) {
                                assertEquals(dpToPixels(48).toInt(), button.width)
                                assertEquals(dpToPixels(density.rowHeightDp).toInt(), button.height)
                                assertTrue(button.isClickable)
                                assertTrue(button.isLongClickable)
                            }
                            assertSame(habit, card.habit)
                            assertEquals(5, card.buttonCount)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testRowSeparatorsInEveryThemeAndSelectionState() {
        val habits = listOf(fixtures.createLongHabit(), fixtures.createLongNumericalHabit())
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            withThemedActivity(style) { activity, factory ->
                val styled = StyledResources(activity)
                val background = styled.getColor(R.attr.windowBackgroundColor)
                val divider = styled.getColor(R.attr.habitRowDividerColor)
                for (habit in habits) {
                    val card = factory.create().apply {
                        this.habit = habit
                        buttonCount = 5
                        setBackgroundColor(background)
                    }
                    for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                        card.layoutDirection = direction
                        for (selected in listOf(false, true, false)) {
                            card.isSelected = selected
                            measureView(card, dpToPixels(400), dpToPixels(51))
                            renderView(card).let {
                                val x = it.width / 2
                                val y = it.height - card.paddingBottom
                                val expected = if (style == R.style.AppBaseTheme) background else divider
                                assertEquals(expected, it.getPixel(x, y))
                                assertEquals(expected, it.getPixel(card.paddingLeft, y))
                                assertEquals(expected, it.getPixel(it.width - card.paddingRight - 1, y))
                                assertEquals(background, it.getPixel(card.paddingLeft - 1, y))
                                assertEquals(background, it.getPixel(it.width - card.paddingRight, y))
                                assertEquals(background, it.getPixel(x, y + dpToPixels(1).toInt()))
                                if (style != R.style.AppBaseTheme) {
                                    assertTrue(ColorUtils.calculateContrast(expected, background) >= 1.5)
                                    val surface = styled.getColor(
                                        if (selected) R.attr.highlightedBackgroundColor else R.attr.cardBgColor
                                    )
                                    assertTrue(ColorUtils.calculateContrast(expected, surface) >= 1.5)
                                }
                                if (!selected) {
                                    val label = (card.getChildAt(0) as ViewGroup).getChildAt(1)
                                    val labelX = card.paddingLeft + label.left + label.width / 2
                                    assertEquals(
                                        styled.getColor(R.attr.cardBgColor),
                                        it.getPixel(labelX, dpToPixels(1).toInt())
                                    )
                                }
                                it.recycle()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun render(style: Int, filename: String, density: ListDensity = ListDensity.COMPACT) {
        var bitmap: Bitmap? = null
        withThemedActivity(style) { activity, factory ->
            bitmap = renderView(createList(activity, factory, density))
        }
        val rendered = checkNotNull(bitmap)
        try {
            assertRenders(rendered, "habits/list/HabitListAppearance/$filename")
        } finally {
            rendered.recycle()
        }
    }

    private fun createList(activity: EditHabitActivity, factory: HabitCardViewFactory, density: ListDensity): LinearLayout {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(StyledResources(activity).getColor(R.attr.windowBackgroundColor))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        fun header(value: Header) {
            root.addView(
                SectionHeaderView(activity).apply {
                    listDensity = density
                    bind(value)
                }
            )
        }
        fun habit(name: String, color: Int, values: IntArray, numerical: Boolean = false) {
            val model = fixtures.createEmptyHabit().apply {
                this.name = name
                this.color = PaletteColor(color)
                if (numerical) {
                    type = HabitType.NUMERICAL
                    targetValue = 30.0
                    unit = "min"
                }
            }
            root.addView(
                factory.create().apply {
                    habit = model
                    this.values = values
                    score = 0.65
                    isSelected = false
                    buttonCount = 5
                    listDensity = density
                }
            )
        }
        header(Header(1, "Morning", 1, 2, isFirstSection = true))
        habit("Stretch", 24, intArrayOf(Entry.YES_MANUAL, Entry.NO, Entry.YES_MANUAL, Entry.YES_MANUAL, Entry.SKIP))
        habit("Drink water", 33, intArrayOf(Entry.UNKNOWN, Entry.YES_MANUAL, Entry.NO, Entry.YES_MANUAL, Entry.YES_MANUAL))
        header(Header(2, "Evening", 0, 1))
        habit("Read", 29, intArrayOf(15000, 30000, 30000, Entry.UNKNOWN, 45000), numerical = true)
        root.measure(
            MeasureSpec.makeMeasureSpec(dpToPixels(400).toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        return root
    }

    private fun withThemedActivity(style: Int, block: (EditHabitActivity, HabitCardViewFactory) -> Unit) {
        setTheme(style)
        prefs.theme = if (style == R.style.AppBaseTheme) ThemeSwitcher.THEME_LIGHT else ThemeSwitcher.THEME_DARK
        prefs.isPureBlackEnabled = style == R.style.AppBaseThemeDark_PureBlack
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val factory = DaggerHabitsActivityTestComponent.builder()
                    .activityContextModule(ActivityContextModule(activity))
                    .habitsApplicationComponent(appComponent)
                    .build()
                    .getHabitCardViewFactory()
                block(activity, factory)
            }
        }
    }
}
