package org.isoron.uhabits.core.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class StartupCoordinatorTest {
    private class QueueExecutor : Executor {
        val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) {
            tasks.addLast(command)
        }
        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }

    @Test
    fun initializationNeverRunsInlineAndReadinessWaitsForCompletion() {
        val worker = QueueExecutor()
        val callbacks = QueueExecutor()
        val phases = mutableListOf<String>()
        val startup = StartupCoordinator(worker, callbacks, { phases.add("initialize") }, { phases.add("finish") })
        startup.observe { phases.add(it.javaClass.simpleName) }
        startup.start()
        assertTrue(phases.isEmpty())
        assertEquals(StartupCoordinator.State.Loading, startup.state)
        worker.runAll()
        assertEquals(listOf("initialize"), phases)
        assertEquals(StartupCoordinator.State.Loading, startup.state)
        callbacks.runAll()
        assertEquals(StartupCoordinator.State.Ready, startup.state)
        assertTrue(phases.indexOf("finish") < phases.indexOf("Ready"))
    }

    @Test
    fun repeatedStartsDoNotDuplicateInitialization() {
        val worker = QueueExecutor()
        val callbacks = QueueExecutor()
        var calls = 0
        val startup = StartupCoordinator(worker, callbacks, { calls++ })
        startup.start()
        startup.start()
        assertEquals(1, worker.tasks.size)
        worker.runAll()
        callbacks.runAll()
        startup.start()
        assertEquals(1, calls)
        assertTrue(worker.tasks.isEmpty())
    }

    @Test
    fun failureIsObservableAndRetryCanComplete() {
        val worker = QueueExecutor()
        val callbacks = QueueExecutor()
        val failure = IllegalStateException("history unavailable")
        var attempts = 0
        val startup = StartupCoordinator(
            worker,
            callbacks,
            { if (attempts++ == 0) throw failure }
        )
        startup.start()
        worker.runAll()
        callbacks.runAll()
        assertSame(failure, (startup.state as StartupCoordinator.State.Failed).cause)
        startup.start()
        assertEquals(StartupCoordinator.State.Loading, startup.state)
        worker.runAll()
        callbacks.runAll()
        assertEquals(StartupCoordinator.State.Ready, startup.state)
        assertEquals(2, attempts)
    }

    @Test
    fun failedCompletionNeverPublishesReady() {
        val worker = QueueExecutor()
        val callbacks = QueueExecutor()
        val startup = StartupCoordinator(worker, callbacks, {}, { throw IllegalStateException("listeners") })
        val states = mutableListOf<StartupCoordinator.State>()
        startup.observe { states.add(it) }
        startup.start()
        worker.runAll()
        callbacks.runAll()
        assertTrue(startup.state is StartupCoordinator.State.Failed)
        assertFalse(states.contains(StartupCoordinator.State.Ready))
    }

    @Test
    fun cancelledObserverDoesNotReceiveQueuedCallbacks() {
        val worker = QueueExecutor()
        val callbacks = QueueExecutor()
        val startup = StartupCoordinator(worker, callbacks, {})
        var calls = 0
        val subscription = startup.observe { calls++ }
        startup.start()
        subscription.close()
        worker.runAll()
        callbacks.runAll()
        assertEquals(0, calls)
    }

    @Test
    fun backgroundReadersWaitUntilModelsAreReady() {
        val worker = QueueExecutor()
        val callbacks = QueueExecutor()
        val startup = StartupCoordinator(worker, callbacks, {})
        startup.start()
        val reader = FutureTask { startup.awaitReady() }
        Thread(reader).start()
        assertFalse(reader.isDone)
        worker.runAll()
        callbacks.runAll()
        reader.get(5, TimeUnit.SECONDS)
        assertTrue(reader.isDone)
    }

    @Test
    fun backgroundFailurePreservesOriginalCause() {
        val failure = IllegalArgumentException("invalid history")
        val direct = Executor { it.run() }
        val startup = StartupCoordinator(direct, direct, { throw failure })
        startup.start()
        try {
            startup.awaitReady()
            throw AssertionError("Expected initialization failure")
        } catch (e: IllegalStateException) {
            assertSame(failure, e.cause)
        }
    }

    @Test(expected = TimeoutException::class)
    fun backgroundWaitHasABoundedTimeout() {
        val startup = StartupCoordinator(QueueExecutor(), QueueExecutor(), {})
        startup.start()
        startup.awaitReady(0)
    }
}
