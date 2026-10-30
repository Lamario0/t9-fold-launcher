package com.lamario.t9fold.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhoneStatus(
    val time: String = "",
    val date: String = "",
    val batteryPercent: Int = 100,
    val charging: Boolean = false,
    /** 0..4 bars. */
    val signal: Int = 0,
)

val LocalPhoneStatus = staticCompositionLocalOf { PhoneStatus() }

@Composable
fun rememberPhoneStatus(): State<PhoneStatus> {
    val context = LocalContext.current
    return produceState(readStatus(context), context) {
        while (true) {
            value = readStatus(context)
            delay(10_000L - System.currentTimeMillis() % 10_000L)
        }
    }
}

private fun readStatus(context: Context): PhoneStatus {
    val now = Date()
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val plugged = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
    val signal = runCatching {
        context.getSystemService(TelephonyManager::class.java)?.signalStrength?.level
    }.getOrNull() ?: 0
    return PhoneStatus(
        time = SimpleDateFormat(timePattern, Locale.getDefault()).format(now),
        date = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now),
        batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else 100,
        charging = plugged,
        signal = signal.coerceIn(0, 4),
    )
}
