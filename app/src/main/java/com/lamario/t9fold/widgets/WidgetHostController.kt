package com.lamario.t9fold.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.util.Log
import kotlin.math.roundToInt

/**
 * Hosts real Android widgets. Adding one goes: allocate an id -> bind it (asks the user once
 * if this launcher isn't trusted yet) -> run the widget's own setup screen if it has one -> save.
 */
class WidgetHostController(private val activity: Activity, private val store: WidgetStore) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)
    val host = AppWidgetHost(activity, HOST_ID)

    private data class Pending(val page: Int, val appWidgetId: Int, val info: AppWidgetProviderInfo)

    private var pending: Pending? = null

    fun startListening() {
        runCatching { host.startListening() }.onFailure { Log.w(TAG, "startListening failed", it) }
    }

    fun stopListening() {
        runCatching { host.stopListening() }
    }

    @Suppress("DEPRECATION")
    fun addSystemWidget(page: Int, info: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        pending = Pending(page, id, info)
        if (manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)) {
            configure()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            activity.startActivityForResult(intent, REQUEST_BIND)
        }
    }

    fun addRetroWidget(page: Int, type: RetroWidgetType) {
        store.add(page, RetroWidget(store.newUid(), type, type.defaultHeightDp))
    }

    fun remove(page: Int, uid: Long) {
        val removed = store.remove(page, uid)
        if (removed is SystemWidget) host.deleteAppWidgetId(removed.appWidgetId)
    }

    /** Returns true if the result belonged to the widget flow. */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        when (requestCode) {
            REQUEST_BIND -> if (resultCode == Activity.RESULT_OK) configure() else finish(false)
            REQUEST_CONFIGURE -> finish(resultCode == Activity.RESULT_OK)
            else -> return false
        }
        return true
    }

    private fun configure() {
        val p = pending ?: return
        if (p.info.configure == null) return finish(true)
        try {
            host.startAppWidgetConfigureActivityForResult(activity, p.appWidgetId, 0, REQUEST_CONFIGURE, null)
        } catch (e: Exception) {
            Log.w(TAG, "Widget setup screen failed to open, adding without it", e)
            finish(true)
        }
    }

    private fun finish(ok: Boolean) {
        val p = pending ?: return
        pending = null
        if (ok) {
            store.add(p.page, SystemWidget(store.newUid(), p.appWidgetId, defaultHeightDp(p.info)))
        } else {
            host.deleteAppWidgetId(p.appWidgetId)
        }
    }

    private fun defaultHeightDp(info: AppWidgetProviderInfo): Int {
        val density = activity.resources.displayMetrics.density
        return (info.minHeight / density).roundToInt().plus(24).coerceIn(110, 420)
    }

    private companion object {
        const val TAG = "WidgetHost"
        const val HOST_ID = 0x7919
        const val REQUEST_BIND = 7101
        const val REQUEST_CONFIGURE = 7102
    }
}
