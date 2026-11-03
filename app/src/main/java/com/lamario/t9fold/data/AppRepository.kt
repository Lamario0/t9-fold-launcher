package com.lamario.t9fold.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(
    /** Stable id: component plus the user profile's serial number (work profiles get their own). */
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    internal val info: LauncherActivityInfo,
)

/** A shortcut an app offers on long-press, e.g. a specific chat or "New tab". */
data class ShortcutEntry(
    val packageName: String,
    val id: String,
    val label: String,
    val userSerial: Long,
    /** Key of the app it belongs to, for grouping. */
    val appKey: String,
    val appLabel: String,
)

/** Full-colour icon, plus a tiny copy that is drawn blown-up for the pixelated LCD style. */
class AppIcon(val full: ImageBitmap, val tiny: ImageBitmap)

class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loadJob: Job? = null

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var byKey by mutableStateOf<Map<String, AppEntry>>(emptyMap())
        private set
    val icons = mutableStateMapOf<String, AppIcon>()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload()
    }

    fun start() {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun stop() {
        launcherApps.unregisterCallback(callback)
        scope.cancel()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch {
            val list = withContext(Dispatchers.IO) { loadApps() }
            apps = list
            byKey = list.associateBy { it.key }
            icons.keys.retainAll(byKey.keys)
            val missing = list.filter { it.key !in icons }
            for (chunk in missing.chunked(12)) {
                val loaded = withContext(Dispatchers.IO) { chunk.mapNotNull { app -> loadIcon(app)?.let { app.key to it } } }
                icons.putAll(loaded)
            }
        }
    }

    private fun loadApps(): List<AppEntry> = userManager.userProfiles.flatMap { user ->
        val serial = userManager.getSerialNumberForUser(user)
        launcherApps.getActivityList(null, user).map { info ->
            AppEntry(
                key = "${info.componentName.flattenToShortString()}@$serial",
                label = info.label?.toString()?.trim().orEmpty().ifEmpty { info.componentName.packageName },
                component = info.componentName,
                user = user,
                info = info,
            )
        }
    }.filter { it.component.packageName != context.packageName }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

    private fun loadIcon(app: AppEntry): AppIcon? = try {
        val drawable = app.info.getBadgedIcon(0)
        val full = drawable.toBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val tiny = Bitmap.createScaledBitmap(full, TINY_ICON_PX, TINY_ICON_PX, true)
        AppIcon(full.asImageBitmap(), tiny.asImageBitmap())
    } catch (e: Exception) {
        Log.w(TAG, "No icon for ${app.key}", e)
        null
    }

    /** App shortcuts are only shared with the default home app. */
    val canUseShortcuts: Boolean
        get() = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    fun shortcuts(): List<ShortcutEntry> {
        if (!canUseShortcuts) return emptyList()
        val query = LauncherApps.ShortcutQuery().setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
        )
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            runCatching { launcherApps.getShortcuts(query, user) }.getOrNull().orEmpty()
                .filter(ShortcutInfo::isEnabled)
                .map { info ->
                    val appKey = info.activity?.let { "${it.flattenToShortString()}@$serial" }
                        ?: apps.firstOrNull { it.component.packageName == info.`package` && it.user == user }?.key
                        ?: "${info.`package`}@$serial"
                    ShortcutEntry(
                        packageName = info.`package`,
                        id = info.id,
                        label = (info.shortLabel ?: info.longLabel ?: info.id).toString(),
                        userSerial = serial,
                        appKey = appKey,
                        appLabel = byKey[appKey]?.label
                            ?: apps.firstOrNull { it.component.packageName == info.`package` }?.label
                            ?: info.`package`,
                    )
                }
        }.distinctBy { Triple(it.userSerial, it.packageName, it.id) }
    }

    fun startShortcut(userSerial: Long, packageName: String, id: String): Boolean = try {
        launcherApps.startShortcut(packageName, id, null, null, userManager.getUserForSerialNumber(userSerial))
        true
    } catch (e: Exception) {
        Log.w(TAG, "Could not start shortcut $packageName/$id", e)
        false
    }

    /** Replaces the set of this package's shortcuts that the launcher holds on to. */
    fun pinShortcuts(userSerial: Long, packageName: String, ids: List<String>) {
        runCatching {
            launcherApps.pinShortcuts(packageName, ids, userManager.getUserForSerialNumber(userSerial))
        }.onFailure { Log.w(TAG, "Could not pin shortcuts for $packageName", it) }
    }

    fun launch(app: AppEntry) {
        try {
            launcherApps.startMainActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch ${app.key}", e)
            reload()
        }
    }

    fun openAppInfo(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.component.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Intent.EXTRA_USER, app.user)
        runCatching { context.startActivity(intent) }
    }

    private companion object {
        const val TAG = "AppRepository"
        const val ICON_PX = 112
        const val TINY_ICON_PX = 20
    }
}
