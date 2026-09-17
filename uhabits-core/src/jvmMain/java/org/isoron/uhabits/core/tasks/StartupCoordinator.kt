package org.isoron.uhabits.core.tasks

import java.util.concurrent.Executor
import java.util.concurrent.TimeoutException

class StartupCoordinator(
    private val worker: Executor,
    private val callbacks: Executor,
    private val initialize: () -> Unit,
    private val finish: () -> Unit = {}
) {
    sealed class State {
        object Loading : State()
        object Ready : State()
        data class Failed(val cause: Exception) : State()
    }

    private val monitor = Object()
    private val listeners = linkedSetOf<(State) -> Unit>()
    private var running = false

    @Volatile
    var state: State = State.Loading
        private set

    fun start() {
        synchronized(monitor) {
            if (running || state == State.Ready) return
            running = true
        }
        publish(State.Loading)
        worker.execute {
            val failure = try {
                initialize()
                null
            } catch (e: Exception) {
                e
            }
            callbacks.execute {
                val result = if (failure != null) {
                    State.Failed(failure)
                } else {
                    try {
                        finish()
                        State.Ready
                    } catch (e: Exception) {
                        State.Failed(e)
                    }
                }
                publish(result)
            }
        }
    }

    fun observe(listener: (State) -> Unit): AutoCloseable {
        synchronized(monitor) { listeners.add(listener) }
        callbacks.execute {
            if (synchronized(monitor) { listener in listeners }) listener(state)
        }
        return AutoCloseable { synchronized(monitor) { listeners.remove(listener) } }
    }

    fun awaitReady(timeoutMillis: Long = 30_000) {
        val deadline = System.nanoTime() + timeoutMillis * 1_000_000
        synchronized(monitor) {
            while (state == State.Loading) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) throw TimeoutException("Habit initialization timed out")
                monitor.wait(remaining / 1_000_000, (remaining % 1_000_000).toInt())
            }
            val result = state
            if (result is State.Failed) throw IllegalStateException("Habit initialization failed", result.cause)
        }
    }

    private fun publish(next: State) {
        val observers = synchronized(monitor) {
            if (next != State.Loading) running = false
            state = next
            monitor.notifyAll()
            listeners.toList()
        }
        for (listener in observers) {
            callbacks.execute {
                if (synchronized(monitor) { listener in listeners }) listener(state)
            }
        }
    }
}
