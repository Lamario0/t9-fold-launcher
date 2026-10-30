package com.lamario.t9fold.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.lamario.t9fold.widgets.RetroWidgetType
import com.lamario.t9fold.widgets.WidgetHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class ProviderRow(val info: AppWidgetProviderInfo, val label: String, val appLabel: String, val icon: ImageBitmap?)

@Composable
fun WidgetPicker(
    widgets: WidgetHostController,
    onRetro: (RetroWidgetType) -> Unit,
    onSystem: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val providers by produceState<List<ProviderRow>?>(null) {
        value = withContext(Dispatchers.IO) { loadProviders(context, widgets) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LcdPanel(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.86f)) {
            Column(Modifier.fillMaxSize()) {
                PixelText("Add widget", size = 26.dp)
                Box(Modifier.fillMaxWidth().height(2.dp).background(Retro.Ink))
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    item { Section("Retro") }
                    items(RetroWidgetType.entries) { type ->
                        PickerRow(type.title, "Built in", null) { onRetro(type) }
                    }
                    item { Section("Android") }
                    val list = providers
                    if (list == null) {
                        item { PixelText("Loading...", Modifier.padding(8.dp)) }
                    } else {
                        items(list, key = { it.info.provider.flattenToString() + it.info.profile.hashCode() }) { row ->
                            val cells = "${row.info.targetCellWidth.coerceAtLeast(1)}x${row.info.targetCellHeight.coerceAtLeast(1)}"
                            PickerRow(row.label, "${row.appLabel}  $cells", row.icon) { onSystem(row.info) }
                        }
                    }
                }
                PixelText("Back", Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(top = 4.dp), size = 22.dp, align = TextAlign.End)
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    PixelText(
        title.uppercase(),
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp).background(Retro.Ink).padding(horizontal = 6.dp),
        size = 20.dp,
        color = Retro.Lcd,
    )
}

@Composable
private fun PickerRow(title: String, subtitle: String, icon: ImageBitmap?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp)) {
            if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            PixelText(title, size = 22.dp)
            PixelText(subtitle, size = 17.dp)
        }
    }
}

private fun loadProviders(context: Context, widgets: WidgetHostController): List<ProviderRow> {
    val pm = context.packageManager
    val density = context.resources.displayMetrics.densityDpi
    return widgets.manager.installedProviders.mapNotNull { info ->
        runCatching {
            val appLabel = pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
            val icon = info.loadIcon(context, density)?.toBitmap(96, 96)?.asImageBitmap()
            ProviderRow(info, info.loadLabel(pm) ?: appLabel, appLabel, icon)
        }.getOrNull()
    }.sortedWith(
        compareBy<ProviderRow, String>(String.CASE_INSENSITIVE_ORDER) { it.appLabel }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
    )
}
