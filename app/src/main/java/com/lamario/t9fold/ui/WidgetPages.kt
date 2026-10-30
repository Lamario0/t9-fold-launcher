package com.lamario.t9fold.ui

import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lamario.t9fold.LauncherState
import com.lamario.t9fold.widgets.RetroWidget
import com.lamario.t9fold.widgets.SystemWidget
import com.lamario.t9fold.widgets.WidgetHostController
import com.lamario.t9fold.widgets.WidgetItem
import com.lamario.t9fold.widgets.WidgetStore

/** One widget page. Blank until you tap EDIT and add something. */
@Composable
fun WidgetPage(
    page: Int,
    state: LauncherState,
    store: WidgetStore,
    widgets: WidgetHostController,
    modifier: Modifier = Modifier,
) {
    val editing = state.editingWidgetPage == page
    val items = store.pages[page]
    val scroll = rememberScrollState()

    Column(modifier.fillMaxSize().background(Retro.Body).padding(bottom = 22.dp)) {
        if (state.prefs.retroStatusBar) {
            LcdPanel(Modifier.fillMaxWidth().height(44.dp).padding(start = 14.dp, end = 14.dp, top = 6.dp), bezel = 4.dp) {
                RetroStatusRow(Modifier.fillMaxSize())
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelText("WIDGETS ${page + 1}", Modifier.weight(1f), size = 18.dp, color = Retro.ChromeDark)
            Chip(if (editing) "DONE" else "EDIT") {
                state.editingWidgetPage = if (editing) null else page
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .widgetPageScroll(scroll, state::expandNotifications),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll, enabled = false)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { item ->
                    key(item.uid) {
                        WidgetFrame(page, item, editing, store, widgets)
                    }
                }
                if (editing) {
                    Chip("+ ADD WIDGET", Modifier.fillMaxWidth().height(52.dp)) { state.widgetPickerPage = page }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WidgetFrame(page: Int, item: WidgetItem, editing: Boolean, store: WidgetStore, widgets: WidgetHostController) {
    Column {
        if (editing) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Chip("UP", Modifier.weight(1f)) { store.move(page, item.uid, -1) }
                Chip("DOWN", Modifier.weight(1f)) { store.move(page, item.uid, 1) }
                Chip("-", Modifier.weight(0.6f)) { store.resize(page, item.uid, -40) }
                Chip("+", Modifier.weight(0.6f)) { store.resize(page, item.uid, 40) }
                Chip("X", Modifier.weight(0.6f)) { widgets.remove(page, item.uid) }
            }
        }
        val frame = Modifier.fillMaxWidth().height(item.heightDp.dp)
        when (item) {
            is RetroWidget -> RetroWidgetContent(item.type, frame)
            is SystemWidget -> SystemWidgetView(item, widgets, frame.clip(RoundedCornerShape(20.dp)))
        }
    }
}

@Composable
private fun SystemWidgetView(item: SystemWidget, widgets: WidgetHostController, modifier: Modifier) {
    val info: AppWidgetProviderInfo? = remember(item.appWidgetId) { widgets.manager.getAppWidgetInfo(item.appWidgetId) }
    if (info == null) {
        Box(modifier.border(1.dp, Retro.ChromeDark, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            PixelText("Widget unavailable", color = Retro.Chrome, align = TextAlign.Center)
        }
        return
    }
    BoxWithConstraints(modifier) {
        val w = maxWidth.value
        val h = maxHeight.value
        AndroidView(
            factory = { context -> widgets.host.createView(context, item.appWidgetId, info) },
            update = { view -> view.updateAppWidgetSize(Bundle(), listOf(SizeF(w, h))) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.5.dp, Retro.ChromeDark, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        PixelText(text, size = 20.dp, color = Retro.Chrome, align = TextAlign.Center)
    }
}

/** Small square dots under the pager, LCD style. */
@Composable
fun PageDots(pager: PagerState, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pager.pageCount) { i ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (i == pager.currentPage) Retro.Chrome else Retro.ChromeDark.copy(alpha = 0.4f)),
            )
        }
    }
}
