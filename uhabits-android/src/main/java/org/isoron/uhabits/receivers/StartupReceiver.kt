package org.isoron.uhabits.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.core.tasks.StartupCoordinator

fun BroadcastReceiver.whenHabitsReady(context: Context, intent: Intent, action: () -> Unit) {
    val startup = (context.applicationContext as HabitsApplication).startup
    if (startup.state == StartupCoordinator.State.Ready) {
        action()
        return
    }
    val pending = goAsync()
    var subscription: AutoCloseable? = null
    var finished = false
    subscription = startup.observe { state ->
        if (state != StartupCoordinator.State.Loading && !finished) {
            finished = true
            subscription?.close()
            try {
                when (state) {
                    StartupCoordinator.State.Ready -> action()
                    is StartupCoordinator.State.Failed ->
                        Log.e("StartupReceiver", "Cannot process ${intent.action}: habit history unavailable", state.cause)
                    StartupCoordinator.State.Loading -> Unit
                }
            } finally {
                pending?.finish()
            }
        }
    }
    if (finished) subscription?.close()
}
