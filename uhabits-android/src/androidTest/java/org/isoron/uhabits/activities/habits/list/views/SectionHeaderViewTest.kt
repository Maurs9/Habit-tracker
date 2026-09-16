package org.isoron.uhabits.activities.habits.list.views

import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.R
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
            view.bind(Header(1, "Morning", 2, 3))
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
            assertEquals(dpToPixels(4).toInt(), view.paddingBottom)
            assertEquals(0.08f, view.nameView.letterSpacing)
            assertEquals(styled.getColor(R.attr.contrast60), view.nameView.currentTextColor)
            assertTrue(
                ColorUtils.calculateContrast(
                    view.nameView.currentTextColor,
                    styled.getColor(R.attr.windowBackgroundColor)
                ) >= 4.5
            )
            view.bind(Header(null, "Ignored", 0, 0))
            assertEquals("Other", view.nameView.text.toString())
            assertEquals(View.GONE, view.countView.visibility)
            assertEquals("Other, 0 of 0 done", view.contentDescription.toString())
        }
    }

    private fun render(header: Header, filename: String) {
        val root = FrameLayout(targetContext).apply {
            setBackgroundColor(StyledResources(targetContext).getColor(R.attr.windowBackgroundColor))
            addView(SectionHeaderView(targetContext).apply { bind(header) })
        }
        measureView(root, dpToPixels(400), dpToPixels(48))
        assertRenders(root, "habits/list/SectionHeaderView/$filename")
    }
}
