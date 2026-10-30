package com.lamario.t9fold.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

object SystemActions {
    /** Pulls down the notification shade. Hidden API, but launchers have relied on it for years. */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        try {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
        } catch (e: Exception) {
            Log.w("SystemActions", "Could not open the notification shade", e)
        }
    }

    fun openHomeSettings(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
