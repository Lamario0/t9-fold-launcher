package com.lamario.t9fold.system

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import com.lamario.t9fold.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateStatus {
    /** Short text for the LCD. */
    val label: String

    data object Idle : UpdateStatus { override val label = "Not checked" }
    data object NoToken : UpdateStatus { override val label = "No token set" }
    data object Checking : UpdateStatus { override val label = "Checking..." }
    data object UpToDate : UpdateStatus { override val label = "Up to date" }
    data class Available(val build: Long, val assetUrl: String) : UpdateStatus { override val label = "Build $build ready" }
    data class Downloading(val percent: Int) : UpdateStatus { override val label = "Downloading $percent%" }
    data object Installing : UpdateStatus { override val label = "Installing..." }
    data class Note(override val label: String) : UpdateStatus
    data class Failed(override val label: String) : UpdateStatus
}

/** Process-wide, so the install result receiver can report back to the screen. */
private object UpdateState {
    var status by mutableStateOf<UpdateStatus>(UpdateStatus.Idle)
}

/**
 * Self-update from this repo's GitHub releases. The repo is private, so requests carry a
 * read-only token the user pastes in once. Each CI build is released as "build-N" with
 * versionCode N, so a higher N means a newer app.
 */
class Updater(context: Context, private val prefs: Prefs) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val status: UpdateStatus get() = UpdateState.status

    val installedBuild: Long =
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode

    val busy: Boolean
        get() = status is UpdateStatus.Checking || status is UpdateStatus.Downloading || status is UpdateStatus.Installing

    /** Quietly checks at most every few hours, when the home screen comes up. */
    fun checkIfDue() {
        val due = System.currentTimeMillis() - prefs.lastUpdateCheck > AUTO_CHECK_INTERVAL_MS
        if (prefs.autoUpdate && prefs.githubToken != null && due && !busy && status !is UpdateStatus.Available) check()
    }

    fun check() {
        if (busy) return
        scope.launch {
            UpdateState.status = UpdateStatus.Checking
            UpdateState.status = withContext(Dispatchers.IO) { fetchLatest() }
            prefs.lastUpdateCheck = System.currentTimeMillis()
        }
    }

    fun install(update: UpdateStatus.Available) {
        if (busy) return
        // Android asks once whether T9 Fold may install apps; send the user there first.
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
            UpdateState.status = UpdateStatus.Note("Allow installs, then retry")
            return
        }
        scope.launch {
            UpdateState.status = UpdateStatus.Downloading(0)
            UpdateState.status = withContext(Dispatchers.IO) {
                try {
                    downloadAndCommit(update.assetUrl)
                    UpdateStatus.Installing
                } catch (e: Exception) {
                    Log.w(TAG, "Update download failed", e)
                    UpdateStatus.Failed(if (e is IOException) "Download failed" else e.message ?: "Update failed")
                }
            }
        }
    }

    /** Reads a GitHub token from the clipboard. */
    fun pasteToken() {
        val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
        if (text.startsWith("github_pat_") || text.startsWith("ghp_")) {
            prefs.githubToken = text
            UpdateState.status = UpdateStatus.Note("Token saved")
            check()
        } else {
            UpdateState.status = UpdateStatus.Failed("No token on clipboard")
        }
    }

    fun removeToken() {
        prefs.githubToken = null
        UpdateState.status = UpdateStatus.NoToken
    }

    fun stop() = scope.cancel()

    private fun fetchLatest(): UpdateStatus {
        val token = prefs.githubToken ?: return UpdateStatus.NoToken
        return try {
            val conn = open(LATEST_RELEASE_URL, "application/vnd.github+json", token)
            when (conn.responseCode) {
                200 -> Unit
                401 -> return UpdateStatus.Failed("Token rejected")
                403 -> return UpdateStatus.Failed("Token not allowed")
                404 -> return UpdateStatus.Failed("Repo or release not found")
                else -> return UpdateStatus.Failed("GitHub error ${conn.responseCode}")
            }
            val release = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val build = release.getString("tag_name").removePrefix("build-").toLongOrNull()
                ?: return UpdateStatus.Failed("Odd release name")
            val assets = release.getJSONArray("assets")
            val asset = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name") == APK_NAME }
                ?: return UpdateStatus.Failed("Release has no APK")
            if (build > installedBuild) UpdateStatus.Available(build, asset.getString("url")) else UpdateStatus.UpToDate
        } catch (e: IOException) {
            Log.w(TAG, "Update check failed", e)
            UpdateStatus.Failed("No connection")
        }
    }

    /** Streams the APK straight into an install session, then hands it to Android to confirm. */
    private suspend fun downloadAndCommit(assetUrl: String) {
        val token = prefs.githubToken ?: error("No token set")
        // GitHub answers with a redirect to a pre-signed download link, which must be fetched without the token.
        var conn = open(assetUrl, "application/octet-stream", token)
        if (conn.responseCode in 300..399) {
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            conn = open(location, "application/octet-stream", token = null)
        }
        if (conn.responseCode != 200) error("Download error ${conn.responseCode}")
        val total = conn.contentLengthLong

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                session.openWrite(APK_NAME, 0, total).use { out ->
                    conn.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        var lastPercent = -1
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            written += n
                            val percent = if (total > 0) (written * 100 / total).toInt() else 0
                            if (percent != lastPercent) {
                                lastPercent = percent
                                withContext(Dispatchers.Main) { UpdateState.status = UpdateStatus.Downloading(percent) }
                            }
                        }
                    }
                    session.fsync(out)
                }
                val callback = Intent(context, UpdateResultReceiver::class.java)
                val pending = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callback,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pending.intentSender)
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        }
    }

    private fun open(url: String, accept: String, token: String?): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", accept)
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }

    companion object {
        private const val TAG = "Updater"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Lamario0/t9-fold-launcher/releases/latest"
        private const val APK_NAME = "t9-fold-launcher.apk"
        private const val AUTO_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}

/** Android reports install progress here; the first report asks us to show its "Update this app?" prompt. */
class UpdateResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            PackageInstaller.STATUS_SUCCESS -> UpdateState.status = UpdateStatus.UpToDate
            PackageInstaller.STATUS_FAILURE_ABORTED -> UpdateState.status = UpdateStatus.Failed("Install cancelled")
            else -> UpdateState.status = UpdateStatus.Failed(
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Install failed",
            )
        }
    }
}
