package org.isoron.uhabits.activities.habits.list

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.utils.StyledResources
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
@MediumTest
class ListHabitsMenuTest : BaseAndroidTest() {
    @Test
    fun testFilterCueResetsWithoutTintingSharedDrawables() {
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val listMenu = ListHabitsMenu(activity, prefs, mock(), mock(), mock())
                val menu = Toolbar(activity).menu
                for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
                    setTheme(style)
                    activity.setTheme(style)
                    val resources = StyledResources(activity)
                    val original = render(checkNotNull(resources.getDrawable(R.attr.iconFilter)))
                    val active = render(
                        checkNotNull(resources.getDrawable(R.attr.iconFilter)).mutate().apply {
                            setTint(DarkTheme().color(PaletteColor.DEFAULT).toInt())
                        }
                    )
                    assertFalse(original.sameAs(active))
                    for ((tags, showCompleted) in listOf(
                        emptySet<String>() to true,
                        setOf("Morning") to true,
                        emptySet<String>() to false,
                        setOf("Morning") to false,
                        emptySet<String>() to true
                    )) {
                        prefs.selectedTags = tags
                        prefs.showCompleted = showCompleted
                        listMenu.onCreate(activity.menuInflater, menu)
                        val actual = render(checkNotNull(menu.findItem(R.id.action_filter).icon))
                        val expected = if (tags.isNotEmpty() || !showCompleted) active else original
                        assertTrue(actual.sameAs(expected))
                        actual.recycle()
                        val fresh = render(checkNotNull(resources.getDrawable(R.attr.iconFilter)))
                        assertTrue(fresh.sameAs(original))
                        fresh.recycle()
                    }
                    original.recycle()
                    active.recycle()
                }
            }
        }
    }

    private fun render(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 48, 48)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }
}
