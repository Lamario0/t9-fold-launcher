package com.lamario.t9fold.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class QuickAction(val label: String) {
    FLASHLIGHT("Flashlight"),
    WIFI("Wi-Fi"),
    INTERNET("Internet"),
    BLUETOOTH("Bluetooth"),
    VOLUME("Volume"),
    DO_NOT_DISTURB("Do not disturb"),
    NOTIFICATIONS("Notifications"),
    QUICK_SETTINGS("Quick settings"),
    CAMERA("Camera"),
    SETTINGS("Settings"),
}

/**
 * Built-in actions for the desk keys. Only the flashlight is a true toggle; Android doesn't let
 * apps switch Wi-Fi or Bluetooth themselves, so those open the system panel or settings page.
 */
class QuickActions(context: Context) {
    private val context = context.applicationContext
    private val camera = context.getSystemService(CameraManager::class.java)
    private val torchId: String? = runCatching {
        camera.cameraIdList.firstOrNull { camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
    }.getOrNull()

    var torchOn by mutableStateOf(false)
        private set

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchId) torchOn = enabled
        }
    }

    fun start() {
        runCatching { camera.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper())) }
    }

    fun stop() {
        runCatching { camera.unregisterTorchCallback(torchCallback) }
    }

    fun run(action: QuickAction) {
        when (action) {
            QuickAction.FLASHLIGHT -> torchId?.let { id ->
                runCatching { camera.setTorchMode(id, !torchOn) }.onFailure { Log.w(TAG, "Torch failed", it) }
            }
            QuickAction.WIFI -> open(Intent(Settings.Panel.ACTION_WIFI))
            QuickAction.INTERNET -> open(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
            QuickAction.BLUETOOTH -> open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            QuickAction.VOLUME -> open(Intent(Settings.Panel.ACTION_VOLUME))
            QuickAction.DO_NOT_DISTURB -> open(Intent("android.settings.ZEN_MODE_SETTINGS"))
            QuickAction.NOTIFICATIONS -> SystemActions.expandNotifications(context)
            QuickAction.QUICK_SETTINGS -> SystemActions.expandQuickSettings(context)
            QuickAction.CAMERA -> open(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            QuickAction.SETTINGS -> open(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun open(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Log.w(TAG, "Could not open ${intent.action}", it) }
    }

    private companion object {
        const val TAG = "QuickActions"
    }
}
