package com.lamario.t9fold.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.lamario.t9fold.search.SearchMode
import com.lamario.t9fold.system.KeySound
import com.lamario.t9fold.ui.PhoneStyle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Settings backed by SharedPreferences and exposed as Compose state, so the UI updates live. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("t9fold", Context.MODE_PRIVATE)

    var phoneStyle by enumPref("phone_style", PhoneStyle.CANDYBAR)
    var searchMode by enumPref("search_mode", SearchMode.MULTI_TAP)
    var keySound by enumPref("key_sound", KeySound.TONES)
    var vibration by boolPref("vibration", true)
    var retroStatusBar by boolPref("retro_status_bar", false)
    var unfoldAnimation by boolPref("unfold_animation", true)
    var mirrorInside by boolPref("mirror_inside", true)
    var retroIcons by boolPref("retro_icons", false)
    var hiddenApps by stringSetPref("hidden_apps")
    var recents by stringListPref("recents")

    /** App keys assigned to the 1, * and # keys. */
    val favorites = mutableStateMapOf<Char, String>().apply {
        for (k in FAVORITE_KEYS) sp.getString(favoritePrefKey(k), null)?.let { put(k, it) }
    }

    fun setFavorite(key: Char, appKey: String?) {
        if (appKey == null) favorites.remove(key) else favorites[key] = appKey
        sp.edit().putString(favoritePrefKey(key), appKey).apply()
    }

    fun toggleHidden(appKey: String) {
        hiddenApps = if (appKey in hiddenApps) hiddenApps - appKey else hiddenApps + appKey
    }

    fun pushRecent(appKey: String) {
        recents = (listOf(appKey) + recents.filter { it != appKey }).take(MAX_RECENTS)
    }

    private fun favoritePrefKey(key: Char) = "favorite_" + when (key) {
        '*' -> "star"
        '#' -> "hash"
        else -> key.toString()
    }

    private inline fun <reified E : Enum<E>> enumPref(key: String, default: E): ReadWriteProperty<Any?, E> {
        val stored = sp.getString(key, null)
        val initial = enumValues<E>().firstOrNull { it.name == stored } ?: default
        return prefState(initial) { putString(key, it.name) }
    }

    private fun boolPref(key: String, default: Boolean) =
        prefState(sp.getBoolean(key, default)) { putBoolean(key, it) }

    private fun stringSetPref(key: String) =
        prefState(sp.getStringSet(key, null)?.toSet() ?: emptySet()) { putStringSet(key, it) }

    private fun stringListPref(key: String) =
        prefState(sp.getString(key, "").orEmpty().split('\n').filter { it.isNotEmpty() }) {
            putString(key, it.joinToString("\n"))
        }

    private fun <T> prefState(initial: T, write: SharedPreferences.Editor.(T) -> Unit) =
        object : ReadWriteProperty<Any?, T> {
            val state = mutableStateOf(initial)
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                state.value = value
                sp.edit().apply { write(value) }.apply()
            }
        }

    companion object {
        val FAVORITE_KEYS = listOf('1', '*', '#')
        private const val MAX_RECENTS = 12
    }
}
