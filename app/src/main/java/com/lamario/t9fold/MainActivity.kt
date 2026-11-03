package com.lamario.t9fold

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lamario.t9fold.data.AppEntry
import com.lamario.t9fold.data.AppRepository
import com.lamario.t9fold.data.Prefs
import com.lamario.t9fold.system.KeyFeedback
import com.lamario.t9fold.system.QuickActions
import com.lamario.t9fold.system.SystemActions
import com.lamario.t9fold.system.Updater
import com.lamario.t9fold.ui.LauncherRoot
import com.lamario.t9fold.widgets.WidgetHostController
import com.lamario.t9fold.widgets.WidgetStore

class MainActivity : ComponentActivity(), LauncherActions {
    private lateinit var repo: AppRepository
    private lateinit var prefs: Prefs
    private lateinit var feedback: KeyFeedback
    private lateinit var widgetStore: WidgetStore
    private lateinit var widgets: WidgetHostController
    private lateinit var updater: Updater
    private lateinit var quick: QuickActions
    private lateinit var state: LauncherState

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        repo = AppRepository(applicationContext)
        prefs = Prefs(applicationContext)
        feedback = KeyFeedback(applicationContext)
        widgetStore = WidgetStore(applicationContext)
        widgets = WidgetHostController(this, widgetStore)
        updater = Updater(applicationContext, prefs)
        quick = QuickActions(applicationContext)
        state = LauncherState(repo, prefs, updater, quick, this)
        repo.start()

        setContent {
            val retroStatusBar = prefs.retroStatusBar
            LaunchedEffect(retroStatusBar) { showSystemStatusBar(!retroStatusBar) }
            val lightBody = prefs.phoneStyle.skin.lightBody
            LaunchedEffect(lightBody) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBody
                    isAppearanceLightNavigationBars = lightBody
                }
            }
            // A launcher is the bottom of the stack: Back only steps back through the phone's menus.
            BackHandler { state.back() }
            LauncherRoot(state, widgetStore, widgets)
        }
    }

    private fun showSystemStatusBar(show: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (show) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onStart() {
        super.onStart()
        widgets.startListening()
        quick.start()
        updater.checkIfDue()
    }

    override fun onStop() {
        super.onStop()
        widgets.stopListening()
        quick.stop()
    }

    /** Home pressed while we're already in front: back to the idle screen and the dialer page. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        state.home()
    }

    override fun onDestroy() {
        repo.stop()
        updater.stop()
        feedback.release()
        super.onDestroy()
    }

    @Deprecated("Widget binding and setup still report back through onActivityResult")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!widgets.onActivityResult(requestCode, resultCode)) super.onActivityResult(requestCode, resultCode, data)
    }

    // ---- LauncherActions --------------------------------------------------------------------

    override fun launch(app: AppEntry) = repo.launch(app)

    override fun openAppInfo(app: AppEntry) = repo.openAppInfo(app)

    override fun uninstall(app: AppEntry) = repo.uninstall(app)

    override fun openHomeSettings() = SystemActions.openHomeSettings(this)

    override fun expandNotifications() = SystemActions.expandNotifications(this)

    override fun keyFeedback(key: PhoneKey) = feedback.play(key, prefs.keySound, prefs.vibration)
}
