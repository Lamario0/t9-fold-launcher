package com.lamario.t9fold.widgets

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

/** Widget pages are shared: the cover screen's widget pages and the main screen's right half show the same list. */
const val WIDGET_PAGE_COUNT = 2

enum class RetroWidgetType(val title: String, val defaultHeightDp: Int) {
    CLOCK("Pixel clock", 170),
    CALENDAR("Calendar", 250),
    BATTERY("Battery", 120),
}

sealed interface WidgetItem {
    val uid: Long
    val heightDp: Int
}

data class SystemWidget(override val uid: Long, val appWidgetId: Int, override val heightDp: Int) : WidgetItem

data class RetroWidget(override val uid: Long, val type: RetroWidgetType, override val heightDp: Int) : WidgetItem

class WidgetStore(context: Context) {
    private val sp = context.getSharedPreferences("t9fold_widgets", Context.MODE_PRIVATE)
    private var lastUid = 0L

    val pages = mutableStateListOf<List<WidgetItem>>().apply { addAll(load()) }

    fun newUid(): Long {
        lastUid = maxOf(lastUid + 1, System.currentTimeMillis())
        return lastUid
    }

    fun add(page: Int, item: WidgetItem) = update(page) { it + item }

    fun remove(page: Int, uid: Long): WidgetItem? {
        val item = pages[page].firstOrNull { it.uid == uid }
        update(page) { list -> list.filter { it.uid != uid } }
        return item
    }

    fun move(page: Int, uid: Long, delta: Int) = update(page) { list ->
        val from = list.indexOfFirst { it.uid == uid }
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (from < 0 || from == to) list else list.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun resize(page: Int, uid: Long, deltaDp: Int) = update(page) { list ->
        list.map {
            if (it.uid != uid) it else {
                val h = (it.heightDp + deltaDp).coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
                when (it) {
                    is SystemWidget -> it.copy(heightDp = h)
                    is RetroWidget -> it.copy(heightDp = h)
                }
            }
        }
    }

    private inline fun update(page: Int, change: (List<WidgetItem>) -> List<WidgetItem>) {
        pages[page] = change(pages[page])
        save()
    }

    private fun load(): List<List<WidgetItem>> {
        val parsed = runCatching {
            val root = JSONArray(sp.getString(KEY_PAGES, "[]"))
            List(root.length()) { p ->
                val arr = root.getJSONArray(p)
                List(arr.length()) { i -> fromJson(arr.getJSONObject(i)) }.filterNotNull()
            }
        }.getOrDefault(emptyList())
        parsed.flatten().maxOfOrNull { it.uid }?.let { lastUid = it }
        return List(WIDGET_PAGE_COUNT) { parsed.getOrElse(it) { emptyList() } }
    }

    private fun save() {
        val root = JSONArray()
        pages.forEach { page -> root.put(JSONArray().apply { page.forEach { put(toJson(it)) } }) }
        sp.edit().putString(KEY_PAGES, root.toString()).apply()
    }

    private fun toJson(item: WidgetItem) = JSONObject().apply {
        put("uid", item.uid)
        put("h", item.heightDp)
        when (item) {
            is SystemWidget -> put("kind", "system").put("id", item.appWidgetId)
            is RetroWidget -> put("kind", "retro").put("type", item.type.name)
        }
    }

    private fun fromJson(o: JSONObject): WidgetItem? = when (o.optString("kind")) {
        "system" -> SystemWidget(o.getLong("uid"), o.getInt("id"), o.getInt("h"))
        "retro" -> RetroWidgetType.entries.firstOrNull { it.name == o.optString("type") }
            ?.let { RetroWidget(o.getLong("uid"), it, o.getInt("h")) }
        else -> null
    }

    companion object {
        private const val KEY_PAGES = "pages"
        const val MIN_HEIGHT_DP = 60
        const val MAX_HEIGHT_DP = 900
    }
}
