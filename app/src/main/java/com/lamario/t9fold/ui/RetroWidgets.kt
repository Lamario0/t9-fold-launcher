package com.lamario.t9fold.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lamario.t9fold.system.LocalPhoneStatus
import com.lamario.t9fold.widgets.RetroWidgetType
import java.text.DateFormatSymbols
import java.util.Calendar

@Composable
fun RetroWidgetContent(type: RetroWidgetType, modifier: Modifier = Modifier) {
    LcdPanel(modifier) {
        when (type) {
            RetroWidgetType.CLOCK -> ClockWidget()
            RetroWidgetType.CALENDAR -> CalendarWidget()
            RetroWidgetType.BATTERY -> BatteryWidget()
        }
    }
}

@Composable
private fun ClockWidget() {
    val status = LocalPhoneStatus.current
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val big = (maxHeight * 0.55f).coerceAtMost(maxWidth * 0.3f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PixelText(status.time, size = big, align = TextAlign.Center)
            PixelText(status.date.uppercase(), size = (big * 0.32f).coerceAtLeast(16.dp), align = TextAlign.Center)
        }
    }
}

@Composable
private fun CalendarWidget() {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val monthName = DateFormatSymbols.getInstance().months[cal.get(Calendar.MONTH)]
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = cal.clone().let { it as Calendar }.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    val weekStart = cal.firstDayOfWeek
    val lead = (firstDow - weekStart + 7) % 7
    val weekdayLetters = DateFormatSymbols.getInstance().shortWeekdays
    val headers = List(7) { i -> weekdayLetters[(weekStart - 1 + i) % 7 + 1].take(2) }
    val cells = List(lead) { 0 } + (1..daysInMonth).toList()
    val weeks = cells.chunked(7)

    Column(Modifier.fillMaxSize()) {
        PixelText("$monthName ${cal.get(Calendar.YEAR)}".uppercase(), Modifier.fillMaxWidth(), size = 22.dp, align = TextAlign.Center)
        Row(Modifier.fillMaxWidth()) {
            headers.forEach { PixelText(it, Modifier.weight(1f), size = 16.dp, align = TextAlign.Center) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Retro.Ink))
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    (week + List(7 - week.size) { 0 }).forEach { day ->
                        val isToday = day == today
                        Box(
                            Modifier.weight(1f).then(if (isToday) Modifier.background(Retro.Ink) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            PixelText(
                                if (day == 0) "" else day.toString(),
                                size = 18.dp,
                                color = if (isToday) Retro.Lcd else Retro.Ink,
                                align = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryWidget() {
    val status = LocalPhoneStatus.current
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.fillMaxHeight(0.7f).width(110.dp)) {
            val stroke = 3.dp.toPx()
            val nub = size.width * 0.08f
            drawRect(Retro.Ink, Offset.Zero, Size(size.width - nub, size.height), style = Stroke(stroke))
            drawRect(Retro.Ink, Offset(size.width - nub, size.height * 0.3f), Size(nub, size.height * 0.4f))
            val bars = ((status.batteryPercent + 9) / 20).coerceIn(0, 5)
            val inner = size.width - nub - stroke * 4
            val bw = inner / 5
            for (i in 0 until bars) {
                drawRect(Retro.Ink, Offset(stroke * 2 + i * bw, stroke * 2), Size(bw - stroke, size.height - stroke * 4))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            PixelText("${status.batteryPercent}%", size = 44.dp)
            PixelText(if (status.charging) "CHARGING" else "BATTERY", size = 18.dp)
        }
    }
}
