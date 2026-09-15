package org.isoron.uhabits.utils

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class RootInsetsTest : BaseAndroidTest() {
    @Test
    @UiThreadTest
    fun preservesExplicitBackgroundAndAppliesInsets() {
        val view = View(ContextThemeWrapper(targetContext, R.style.AppBaseTheme))
        val background = ColorDrawable(Color.WHITE)
        view.background = background
        view.applyRootViewInsets()
        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(12, 24, 16, 32))
                .build()
        )
        assertSame(background, view.background)
        assertEquals(12, view.paddingLeft)
        assertEquals(16, view.paddingRight)
    }
}
