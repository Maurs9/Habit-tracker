package org.isoron.uhabits.activities.habits.list.views

import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.ListDensity
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache.ListItem.Header
import org.isoron.uhabits.utils.StyledResources
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class SectionHeaderViewTest : BaseViewTest() {
    @Test
    fun testRender() = render(Header(1, "Morning", 2, 3), "render.png")

    @Test
    fun testRender_other() = render(Header(null, "", 1, 4), "render_other.png")

    @Test
    fun testHeadingCountsSpacingAndContrastInEveryTheme() {
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            setTheme(style)
            val styled = StyledResources(targetContext)
            val view = SectionHeaderView(targetContext)
            view.bind(Header(1, "Morning", 2, 3, isFirstSection = true))
            assertEquals("Morning", view.nameView.text.toString())
            assertEquals("2/3", view.countView.text.toString())
            assertEquals("Morning, 2 of 3 done", view.contentDescription.toString())
            assertTrue(ViewCompat.isAccessibilityHeading(view))
            assertFalse(view.isClickable)
            assertFalse(view.isLongClickable)
            assertNull(view.background)
            assertEquals(dpToPixels(34).toInt(), view.paddingStart)
            assertEquals(dpToPixels(16).toInt(), view.paddingEnd)
            assertEquals(dpToPixels(12).toInt(), view.paddingTop)
            assertEquals(dpToPixels(6).toInt(), view.paddingBottom)
            assertEquals(0.08f, view.nameView.letterSpacing)
            assertEquals(styled.getColor(R.attr.contrast80), view.nameView.currentTextColor)
            assertEquals(styled.getColor(R.attr.contrast60), view.countView.currentTextColor)
            assertTrue(view.nameView.typeface.isBold)
            assertFalse(view.countView.typeface.isBold)
            assertTrue(
                ColorUtils.calculateContrast(
                    view.nameView.currentTextColor,
                    styled.getColor(R.attr.windowBackgroundColor)
                ) >= 4.5
            )
            view.bind(Header(null, "Ignored", 0, 0))
            assertEquals("Other", view.nameView.text.toString())
            assertEquals(View.GONE, view.countView.visibility)
            assertEquals(dpToPixels(28).toInt(), view.paddingTop)
            assertEquals("Other, 0 of 0 done", view.contentDescription.toString())
        }
    }

    @Test
    fun testDividerAndSpacingResetWhenHeaderBecomesFirstInEveryTheme() {
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            setTheme(style)
            val styled = StyledResources(targetContext)
            val background = styled.getColor(R.attr.windowBackgroundColor)
            val view = SectionHeaderView(targetContext).apply {
                setBackgroundColor(background)
            }
            for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                view.layoutDirection = direction
                for (first in listOf(false, true, false)) {
                    view.bind(Header(1, "Morning", 2, 3, isFirstSection = first))
                    measureView(view, dpToPixels(320), dpToPixels(64))
                    assertEquals(dpToPixels(if (first) 12 else 28).toInt(), view.paddingTop)
                    renderView(view).let {
                        val y = dpToPixels(8).toInt()
                        val expected = if (first) background else styled.getColor(R.attr.contrast20)
                        assertEquals(expected, it.getPixel(view.paddingLeft, y))
                        assertEquals(expected, it.getPixel(it.width - view.paddingRight - 1, y))
                        assertEquals(background, it.getPixel(view.paddingLeft - 1, y))
                        assertEquals(background, it.getPixel(it.width - view.paddingRight, y))
                        assertEquals(background, it.getPixel(it.width / 2, y + dpToPixels(1).toInt()))
                        it.recycle()
                    }
                }
            }
        }
    }

    @Test
    fun testLongHeadingWrapsWithoutClippingCountAtLargeTextSize() {
        val view = SectionHeaderView(targetContext).apply {
            bind(Header(1, "Morning routine and personal wellbeing", 123, 456))
            nameView.textSize = 24f
            countView.textSize = 24f
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            layoutParams = ViewGroup.LayoutParams(dpToPixels(320).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        view.measure(
            MeasureSpec.makeMeasureSpec(dpToPixels(320).toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        assertTrue(view.nameView.lineCount > 1)
        assertEquals(1, view.countView.lineCount)
        assertEquals("123/456", view.countView.text.toString())
        assertTrue(view.nameView.height >= view.nameView.layout.height)
        assertTrue(view.countView.right <= view.nameView.left)
        assertTrue(view.countView.left >= view.paddingLeft)
        assertTrue(view.nameView.right <= view.width - view.paddingRight)
    }

    @Test
    fun testCompactHeaderSpacing() {
        val view = SectionHeaderView(targetContext)
        val density = ListDensity.COMPACT
        view.listDensity = density
        for (first in listOf(true, false)) {
            view.bind(Header(1, "Morning", 2, 3, isFirstSection = first))
            assertEquals(
                dpToPixels(if (first) density.firstSectionTopDp else density.sectionTopDp).toInt(),
                view.paddingTop
            )
            assertEquals(dpToPixels(density.sectionBottomDp).toInt(), view.paddingBottom)
            assertEquals(dpToPixels(34).toInt(), view.paddingStart)
            assertEquals("2/3", view.countView.text.toString())
            assertTrue(view.nameView.typeface.isBold)
        }
    }

    private fun render(header: Header, filename: String) {
        val root = FrameLayout(targetContext).apply {
            setBackgroundColor(StyledResources(targetContext).getColor(R.attr.windowBackgroundColor))
            addView(SectionHeaderView(targetContext).apply { bind(header) })
        }
        measureView(root, dpToPixels(400), dpToPixels(64))
        assertRenders(root, "habits/list/SectionHeaderView/$filename")
    }
}
