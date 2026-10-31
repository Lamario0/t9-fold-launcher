package com.lamario.t9fold.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lamario.t9fold.LauncherState
import com.lamario.t9fold.LcdItem
import com.lamario.t9fold.Screen
import com.lamario.t9fold.search.SearchInput
import com.lamario.t9fold.search.SearchMode
import com.lamario.t9fold.system.LocalPhoneStatus
import kotlinx.coroutines.delay

@Composable
fun LcdContent(state: LauncherState, mirrored: Boolean) {
    val screen = state.screen
    val input = state.input

    // After a short pause the multi-tap letter locks in, so the same key starts a new letter.
    LaunchedEffect(input.lastPressAt, input.composing) {
        if (input.composing) {
            delay(SearchInput.MULTI_TAP_TIMEOUT_MS)
            state.commitInput(input.lastPressAt)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (state.prefs.retroStatusBar) {
            RetroStatusRow(Modifier.fillMaxWidth().height(20.dp))
            Spacer(Modifier.height(2.dp))
        }
        if (screen != Screen.Idle) LcdHeader(state)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (screen == Screen.Idle) {
                IdleScreen(state, Modifier.fillMaxSize().swipeDownToExpand(state::expandNotifications))
            } else {
                LcdList(state)
            }
        }
        SoftLabels(state, mirrored)
    }
}

@Composable
private fun LcdHeader(state: LauncherState) {
    Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (state.screen == Screen.Search) {
            PixelText(queryText(state), Modifier.weight(1f), size = 26.dp)
            PixelText("${state.results.size}", size = 20.dp)
        } else {
            PixelText(state.title(), Modifier.weight(1f), size = 24.dp)
        }
    }
    Box(Modifier.fillMaxWidth().height(2.dp).background(Retro.Ink))
    Spacer(Modifier.height(3.dp))
}

/** "KI" with the letter still being cycled shown inverted; "[JKL]" for a whole key. */
@Composable
private fun queryText(state: LauncherState): AnnotatedString {
    val input = state.input
    var cursorOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cursorOn = !cursorOn
        }
    }
    return buildAnnotatedString {
        if (state.prefs.searchMode == SearchMode.T9_WORDS) {
            append(input.digits)
        } else {
            input.slots.forEachIndexed { i, slot ->
                val text = if (slot.isWildcard) "[${slot.label}]" else slot.label
                if (i == input.slots.lastIndex && input.composing) {
                    withStyle(SpanStyle(background = Retro.Ink, color = Retro.Lcd)) { append(text) }
                } else {
                    append(text)
                }
            }
        }
        if (!input.composing) append(if (cursorOn) "_" else " ")
    }
}

@Composable
private fun LcdList(state: LauncherState) {
    val items = state.items()
    val screen = state.screen
    val listState = remember(screen, state.input.slots.size) { LazyListState() }
    val pullDown = rememberPullDownConnection(state::expandNotifications)

    // Keep the highlighted row on screen when moving with the rocker.
    LaunchedEffect(state.selection, listState) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return@LaunchedEffect
        val first = visible.first().index
        val last = visible.last().index
        if (state.selection <= first) {
            listState.scrollToItem(state.selection)
        } else if (state.selection >= last) {
            listState.scrollToItem((state.selection - (last - first) + 1).coerceAtLeast(0))
        }
    }

    if (items.isEmpty()) {
        PixelText(
            state.emptyMessage(),
            Modifier.fillMaxWidth().padding(top = 24.dp).swipeDownToExpand(state::expandNotifications),
            align = TextAlign.Center,
        )
        return
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().nestedScroll(pullDown)) {
        itemsIndexed(items, key = { _, item -> item.id }) { i, item ->
            LcdRow(
                state = state,
                item = item,
                number = if (screen.numbered) i + 1 else null,
                selected = i == state.selection,
                onTap = {
                    state.selection = i
                    item.onSelect()
                },
            )
        }
    }
}

@Composable
private fun LcdRow(state: LauncherState, item: LcdItem, number: Int?, selected: Boolean, onTap: () -> Unit) {
    val ink = if (selected) Retro.Lcd else Retro.Ink
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(item.onLongPress)
    Row(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(if (selected) Retro.Ink else Color.Transparent)
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { tap() },
                    onLongPress = { longPress?.invoke() ?: tap() },
                )
            }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            PixelText("$number", Modifier.width(26.dp), color = ink)
        }
        item.app?.let { app ->
            val icon = state.icon(app)
            Box(Modifier.size(28.dp)) {
                if (icon != null) {
                    if (state.prefs.retroIcons) {
                        Image(
                            icon.tiny,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = Retro.LcdIconFilter,
                            filterQuality = FilterQuality.None,
                        )
                    } else {
                        Image(icon.full, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        item.checked?.let { checked ->
            RadioBox(checked, ink)
            Spacer(Modifier.width(8.dp))
        }
        PixelText(item.title, Modifier.weight(1f), color = ink)
        item.value?.let { PixelText(it, Modifier.padding(start = 6.dp), size = 20.dp, color = ink) }
    }
}

@Composable
private fun RadioBox(checked: Boolean, ink: Color) {
    Box(Modifier.size(14.dp).border(2.dp, ink), contentAlignment = Alignment.Center) {
        if (checked) Box(Modifier.size(6.dp).background(ink))
    }
}

@Composable
private fun SoftLabels(state: LauncherState, mirrored: Boolean) {
    val (left, center, right) = state.softLabels()
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        PixelText(if (mirrored) right else left, Modifier.weight(1f), size = 20.dp)
        PixelText(center, Modifier.weight(1f), size = 20.dp, align = TextAlign.Center)
        PixelText(if (mirrored) left else right, Modifier.weight(1f), size = 20.dp, align = TextAlign.End)
    }
}

/** Signal bars, clock, battery: the old status line drawn in LCD pixels. */
@Composable
fun RetroStatusRow(modifier: Modifier = Modifier, ink: Color = Retro.Ink) {
    val status = LocalPhoneStatus.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Canvas(Modifier.size(26.dp, 16.dp)) {
            val barW = size.width / 5
            for (i in 0 until 4) {
                val h = size.height * (i + 1) / 4f
                val on = i < status.signal
                drawRect(
                    color = if (on) ink else ink.copy(alpha = 0.18f),
                    topLeft = Offset(i * barW * 1.3f, size.height - h),
                    size = Size(barW, h),
                )
            }
        }
        PixelText(status.time, size = 20.dp, color = ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelText("${status.batteryPercent}%", size = 18.dp, color = ink)
            Spacer(Modifier.width(4.dp))
            if (status.charging) PixelText("+", size = 18.dp, color = ink)
            Canvas(Modifier.size(28.dp, 14.dp)) {
                val nub = size.width * 0.1f
                val stroke = 2.dp.toPx()
                drawRect(ink, Offset(0f, 0f), Size(size.width - nub, size.height), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawRect(ink, Offset(size.width - nub, size.height * 0.3f), Size(nub, size.height * 0.4f))
                val inner = size.width - nub - stroke * 3
                val bars = ((status.batteryPercent + 12) / 25).coerceIn(0, 4)
                val bw = inner / 4
                for (i in 0 until bars) {
                    drawRect(ink, Offset(stroke * 1.5f + i * bw, stroke * 1.5f), Size(bw - stroke * 0.6f, size.height - stroke * 3))
                }
            }
        }
    }
}

@Composable
private fun IdleScreen(state: LauncherState, modifier: Modifier) {
    val status = LocalPhoneStatus.current
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PixelText("T9 FOLD", Modifier.weight(1f), size = 24.dp)
            if (!state.prefs.retroStatusBar) PixelText(status.time, size = 24.dp)
        }
        Spacer(Modifier.height(4.dp))
        RetroGames(Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(4.dp))
        PixelText(status.date, Modifier.fillMaxWidth(), size = 20.dp, align = TextAlign.Center)
    }
}
