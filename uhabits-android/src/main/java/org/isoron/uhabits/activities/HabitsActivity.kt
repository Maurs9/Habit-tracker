package org.isoron.uhabits.activities

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.tasks.StartupCoordinator
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres

abstract class HabitsActivity : AppCompatActivity() {
    protected var isContentReady = false
        private set
    private var started = false
    private var contentRecreationRequested = false
    private var subscription: AutoCloseable? = null
    private var savedCreateState: Bundle? = null
    private val pendingResults = mutableListOf<Triple<Int, Int, Intent?>>()
    private val pendingPermissions = mutableListOf<Triple<Int, Array<String>, IntArray>>()

    protected open fun onBeforeCreate() {}

    /** Translucent sheets can opt out of the opaque loading panel while startup completes. */
    protected open val showsStartupPanel: Boolean get() = true
    protected abstract fun onCreateReady(savedInstanceState: Bundle?)
    protected open fun onStartReady() {}
    protected open fun onResumeReady() {}
    protected open fun onPauseReady() {}
    protected open fun onStopReady() {}
    protected open fun onDestroyReady() {}
    protected open fun onSaveInstanceStateReady(outState: Bundle) {}
    protected open fun onNewIntentReady(intent: Intent?) {}
    protected open fun onActivityResultReady(requestCode: Int, resultCode: Int, data: Intent?) {}
    protected open fun onCreateOptionsMenuReady(menu: Menu): Boolean = super.onCreateOptionsMenu(menu)
    protected open fun onPrepareOptionsMenuReady(menu: Menu): Boolean = super.onPrepareOptionsMenu(menu)
    protected open fun onOptionsItemSelectedReady(item: MenuItem): Boolean = super.onOptionsItemSelected(item)
    protected open fun onRequestPermissionsResultReady(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {}

    final override fun onCreate(savedInstanceState: Bundle?) {
        onBeforeCreate()
        val startup = (application as HabitsApplication).startup
        val ready = startup.state == StartupCoordinator.State.Ready
        val contentState = if (savedInstanceState?.containsKey(PENDING_CREATE_STATE) == true) {
            savedInstanceState.getBundle(PENDING_CREATE_STATE)
        } else {
            savedInstanceState
        }
        // Restored fragments must not read partially initialized models from the loading instance.
        super.onCreate(if (ready) contentState else null)
        savedCreateState = contentState
        restorePendingResults(savedInstanceState)
        if (ready) {
            createContent()
        } else {
            showStartupStateIfEnabled(startup.state)
            subscription = startup.observe { state ->
                if (!isFinishing && !isDestroyed) {
                    if (state == StartupCoordinator.State.Ready) {
                        requestContentRecreation()
                    } else {
                        showStartupStateIfEnabled(state)
                    }
                }
            }
        }
    }

    private fun createContent() {
        if (isContentReady || isFinishing || isDestroyed) return
        isContentReady = true
        subscription?.close()
        subscription = null
        onCreateReady(savedCreateState)
        savedCreateState = null
        if (isFinishing) return
        invalidateOptionsMenu()
    }

    private fun requestContentRecreation() {
        if (!started || isFinishing || isDestroyed || supportFragmentManager.isStateSaved) return
        if (contentRecreationRequested) return
        contentRecreationRequested = true
        // Content must initialize in onCreate, before restored fragments and result handlers resume.
        recreate()
    }

    final override fun onStart() {
        super.onStart()
        started = true
        if (isContentReady) {
            onStartReady()
            deliverPendingResults()
        } else if ((application as HabitsApplication).startup.state == StartupCoordinator.State.Ready) {
            requestContentRecreation()
        }
    }

    final override fun onResume() {
        super.onResume()
        if (isContentReady) {
            onResumeReady()
        } else if ((application as HabitsApplication).startup.state == StartupCoordinator.State.Ready) {
            requestContentRecreation()
        }
    }

    final override fun onPause() {
        if (isContentReady) onPauseReady()
        super.onPause()
    }

    final override fun onStop() {
        if (isContentReady) onStopReady()
        started = false
        super.onStop()
    }

    final override fun onDestroy() {
        subscription?.close()
        subscription = null
        if (isContentReady) onDestroyReady()
        super.onDestroy()
    }

    final override fun onSaveInstanceState(outState: Bundle) {
        if (isContentReady) {
            onSaveInstanceStateReady(outState)
            super.onSaveInstanceState(outState)
        } else {
            super.onSaveInstanceState(outState)
            // Preserve null for a fresh launch; the loading view's state is not content state.
            outState.putBundle(PENDING_CREATE_STATE, savedCreateState)
        }
        outState.putParcelableArrayList(
            PENDING_RESULTS,
            ArrayList(
                pendingResults.map { (request, result, data) ->
                    Bundle().apply {
                        putInt("request", request)
                        putInt("result", result)
                        putParcelable("data", data)
                    }
                }
            )
        )
        outState.putParcelableArrayList(
            PENDING_PERMISSIONS,
            ArrayList(
                pendingPermissions.map { (request, permissions, results) ->
                    Bundle().apply {
                        putInt("request", request)
                        putStringArray("permissions", permissions)
                        putIntArray("results", results)
                    }
                }
            )
        )
    }

    final override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isContentReady) onNewIntentReady(intent)
    }

    final override fun onCreateOptionsMenu(menu: Menu): Boolean =
        isContentReady && onCreateOptionsMenuReady(menu)

    final override fun onPrepareOptionsMenu(menu: Menu): Boolean =
        isContentReady && onPrepareOptionsMenuReady(menu)

    final override fun onOptionsItemSelected(item: MenuItem): Boolean =
        isContentReady && onOptionsItemSelectedReady(item)

    @Deprecated("Deprecated in Java")
    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (isContentReady) {
            super.onActivityResult(requestCode, resultCode, data)
            onActivityResultReady(requestCode, resultCode, data)
        } else {
            pendingResults.add(Triple(requestCode, resultCode, data))
        }
    }

    final override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (isContentReady) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            onRequestPermissionsResultReady(requestCode, permissions, grantResults)
        } else {
            pendingPermissions.add(Triple(requestCode, permissions.toList().toTypedArray(), grantResults.copyOf()))
        }
    }

    @Suppress("DEPRECATION")
    private fun deliverPendingResults() {
        val results = pendingResults.toList()
        pendingResults.clear()
        results.forEach { (request, result, data) -> onActivityResult(request, result, data) }
        val permissions = pendingPermissions.toList()
        pendingPermissions.clear()
        permissions.forEach { (request, names, grants) -> onRequestPermissionsResult(request, names, grants) }
    }

    @Suppress("DEPRECATION")
    private fun restorePendingResults(savedInstanceState: Bundle?) {
        savedInstanceState?.getParcelableArrayList<Bundle>(PENDING_RESULTS)?.forEach {
            pendingResults.add(Triple(it.getInt("request"), it.getInt("result"), it.getParcelable("data")))
        }
        savedInstanceState?.getParcelableArrayList<Bundle>(PENDING_PERMISSIONS)?.forEach {
            pendingPermissions.add(
                Triple(
                    it.getInt("request"),
                    requireNotNull(it.getStringArray("permissions")),
                    requireNotNull(it.getIntArray("results"))
                )
            )
        }
    }

    final override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // The framework restores the view hierarchy from the outer bundle. After a loading
        // recreate that bundle holds the loading panel's hierarchy and the content hierarchy
        // is nested under PENDING_CREATE_STATE, so dispatch the matching one.
        val contentState = if (savedInstanceState.containsKey(PENDING_CREATE_STATE)) {
            savedInstanceState.getBundle(PENDING_CREATE_STATE)
        } else {
            savedInstanceState
        }
        if (isContentReady) {
            contentState?.let { super.onRestoreInstanceState(it) }
        } else {
            super.onRestoreInstanceState(savedInstanceState)
        }
    }

    private fun showStartupStateIfEnabled(state: StartupCoordinator.State) {
        when {
            showsStartupPanel -> showStartupState(state)
            state is StartupCoordinator.State.Failed -> finish()
        }
    }

    private fun showStartupState(state: StartupCoordinator.State) {
        val app = application as HabitsApplication
        AndroidThemeSwitcher(this, app.component.preferences).apply()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(sres.getColor(R.attr.windowBackgroundColor))
            val padding = dp(24f).toInt()
            setPadding(padding, padding, padding, padding)
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
                insets
            }
        }
        val failed = state is StartupCoordinator.State.Failed
        if (!failed) {
            panel.addView(
                ProgressBar(this).apply {
                    id = R.id.startup_progress
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            )
        }
        panel.addView(
            TextView(this).apply {
                id = R.id.startup_message
                setText(if (failed) R.string.startup_failed else R.string.startup_loading)
                setTextColor(panel.sres.getColor(R.attr.contrast100))
                textSize = 16f
                gravity = Gravity.CENTER
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                setPadding(0, panel.dp(16f).toInt(), 0, panel.dp(16f).toInt())
            }
        )
        if (failed) {
            panel.addView(
                MaterialButton(this).apply {
                    id = R.id.startup_retry
                    setText(R.string.startup_retry)
                    setOnClickListener { app.startup.start() }
                }
            )
        }
        panel.addView(
            MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                id = R.id.startup_cancel
                setText(android.R.string.cancel)
                setOnClickListener { finish() }
            }
        )
        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                addView(panel, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        )
    }

    companion object {
        private const val PENDING_CREATE_STATE = "habits.startup.pendingCreateState"
        private const val PENDING_RESULTS = "habits.startup.pendingResults"
        private const val PENDING_PERMISSIONS = "habits.startup.pendingPermissions"
    }
}
