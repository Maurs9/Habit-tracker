package org.isoron.uhabits

import android.content.Context
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class BaseExceptionHandlerTest : BaseAndroidJVMTest() {
    private var savedHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var previousHandler: Thread.UncaughtExceptionHandler
    private lateinit var activityContext: Context
    private lateinit var appContext: Context

    @Before
    fun rememberHandler() {
        savedHandler = Thread.getDefaultUncaughtExceptionHandler()
        previousHandler = mock()
        activityContext = mock()
        appContext = mock()
        whenever(activityContext.applicationContext).thenReturn(appContext)
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    @After
    fun restoreHandler() {
        Thread.setDefaultUncaughtExceptionHandler(savedHandler)
    }

    @Test
    fun testInstallDoesNotChainHandlers() {
        BaseExceptionHandler.install(activityContext)
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        repeat(3) { BaseExceptionHandler.install(activityContext) }
        assertSame(handler, Thread.getDefaultUncaughtExceptionHandler())
        verify(activityContext).applicationContext
        verifyNoMoreInteractions(activityContext, previousHandler)
    }

    @Test
    fun testReportsWithApplicationContextAndDelegatesWhenReportingFails() {
        whenever(appContext.getExternalFilesDirs(null)).thenThrow(IllegalStateException("Storage unavailable"))
        BaseExceptionHandler.install(activityContext)
        val thread = Thread.currentThread()
        val error = IllegalStateException("Test crash")
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(thread, error)
        verify(appContext).getExternalFilesDirs(null)
        verify(activityContext).applicationContext
        verify(previousHandler).uncaughtException(thread, error)
        verifyNoMoreInteractions(activityContext, previousHandler)
    }
}
