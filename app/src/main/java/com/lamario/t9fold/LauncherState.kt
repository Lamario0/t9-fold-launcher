package com.lamario.t9fold

import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lamario.t9fold.data.AppEntry
import com.lamario.t9fold.data.AppFolder
import com.lamario.t9fold.data.AppRepository
import com.lamario.t9fold.data.Prefs
import com.lamario.t9fold.search.MultiTapText
import com.lamario.t9fold.search.SearchIndex
import com.lamario.t9fold.search.SearchInput
import com.lamario.t9fold.search.SearchMode
import com.lamario.t9fold.system.KeySound
import com.lamario.t9fold.ui.PhoneStyle

enum class PhoneKey(val char: Char? = null) {
    K1('1'), K2('2'), K3('3'), K4('4'), K5('5'), K6('6'), K7('7'), K8('8'), K9('9'),
    STAR('*'), K0('0'), HASH('#'),
    SOFT_LEFT, SOFT_RIGHT, UP, DOWN, CALL, END;

    companion object {
        fun of(c: Char): PhoneKey = entries.first { it.char == c }
    }
}

/** What the activity does on the state's behalf. */
interface LauncherActions {
    fun launch(app: AppEntry)
    fun openAppInfo(app: AppEntry)
    fun uninstall(app: AppEntry)
    fun openHomeSettings()
    fun expandNotifications()
    fun keyFeedback(key: PhoneKey)
}

enum class Setting(val title: String) {
    PHONE_STYLE("Phone style"),
    SEARCH_MODE("Search mode"),
    KEY_SOUND("Key sounds"),
    VIBRATION("Vibration"),
    STATUS_BAR("Status bar"),
    UNFOLD_ANIMATION("Unfold animation"),
    MIRROR_INSIDE("Mirror inside"),
    ICON_STYLE("App icons"),
}

sealed interface Screen {
    /** Menu-style screens: numbered rows, and keys 1-9 pick a row like on the old phones. */
    val numbered: Boolean get() = false

    data object Idle : Screen
    data object Search : Screen
    data object AllApps : Screen
    data object Recents : Screen
    data object HiddenApps : Screen
    data class PickFavorite(val key: Char) : Screen
    data object Menu : Screen { override val numbered = true }
    data class Choice(val setting: Setting) : Screen { override val numbered = true }
    data object Favorites : Screen { override val numbered = true }
    data class AppActions(val appKey: String) : Screen { override val numbered = true }
    data object Folders : Screen
    data class Folder(val id: Long) : Screen
    data class FolderApps(val id: Long) : Screen
    data class AddToFolder(val appKey: String) : Screen
    data class FolderActions(val id: Long) : Screen { override val numbered = true }

    /** Typing a folder name on the keypad. [folderId] null means a new folder, optionally holding [appKey]. */
    data class NameFolder(val folderId: Long?, val appKey: String?) : Screen
}

class LcdItem(
    val id: String,
    val title: String,
    val app: AppEntry? = null,
    val folderId: Long? = null,
    val value: String? = null,
    /** Non-null for radio-style option rows. */
    val checked: Boolean? = null,
    val onLongPress: (() -> Unit)? = null,
    val onSelect: () -> Unit,
)

/**
 * Everything the phone "is" right now: typed keys, which LCD screen is open, the highlighted row.
 * Shared by the cover screen and the unfolded screen, so folding never loses your place.
 */
class LauncherState(
    private val repo: AppRepository,
    val prefs: Prefs,
    private val actions: LauncherActions,
) {
    var input by mutableStateOf(SearchInput())
        private set
    var stack by mutableStateOf<List<Screen>>(listOf(Screen.Idle))
        private set
    var selection by mutableIntStateOf(0)

    /** Bumped when the Home button is pressed while we're already home. */
    var homeSignal by mutableIntStateOf(0)
        private set
    var nameEntry by mutableStateOf(MultiTapText())
        private set
    var editingWidgetPage by mutableStateOf<Int?>(null)
    var widgetPickerPage by mutableStateOf<Int?>(null)

    val screen: Screen get() = stack.last()

    private val visibleApps by derivedStateOf { repo.apps.filter { it.key !in prefs.hiddenApps } }
    private val index by derivedStateOf { SearchIndex(visibleApps) { it.label } }
    val results by derivedStateOf {
        if (input.isEmpty) emptyList() else index.search(input, prefs.searchMode)
    }
    private val sortedFolders by derivedStateOf {
        prefs.folders.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
    private val folderIndex by derivedStateOf { SearchIndex(sortedFolders) { it.name } }
    private val folderResults by derivedStateOf {
        if (input.isEmpty) emptyList() else folderIndex.search(input, prefs.searchMode)
    }
    val resultCount: Int get() = results.size + folderResults.size

    fun icon(app: AppEntry) = repo.icons[app.key]

    /** What a favourite key is set to, for printing on the key: an app or folder name. */
    fun favoriteLabel(key: Char): String? {
        val favorite = prefs.favorites[key] ?: return null
        return Prefs.folderIdOf(favorite)?.let { prefs.folder(it)?.name } ?: repo.byKey[favorite]?.label
    }

    private fun openFavorite(key: Char) {
        val favorite = prefs.favorites[key]
        val folderId = favorite?.let { Prefs.folderIdOf(it) }
        val app = favorite?.let { repo.byKey[it] }
        when {
            folderId != null && prefs.folder(folderId) != null -> open(Screen.Folder(folderId))
            app != null -> launch(app)
            else -> open(Screen.PickFavorite(key))
        }
    }

    private fun folderApps(folder: AppFolder): List<AppEntry> = folder.apps
        .mapNotNull { repo.byKey[it] }
        .filter { it.key !in prefs.hiddenApps }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

    // ---- Keys -------------------------------------------------------------------------------

    fun feedback(key: PhoneKey) = actions.keyFeedback(key)

    fun onKey(key: PhoneKey) {
        (screen as? Screen.NameFolder)?.let { return onNameKey(it, key) }
        when (key) {
            PhoneKey.UP -> moveSelection(-1)
            PhoneKey.DOWN -> moveSelection(1)
            PhoneKey.END -> resetToIdle()
            PhoneKey.CALL -> if (screen == Screen.Idle) open(Screen.Recents) else activateSelected()
            PhoneKey.SOFT_LEFT -> when (screen) {
                Screen.Idle -> open(Screen.Menu)
                Screen.Search, Screen.AllApps, Screen.Recents, Screen.Folders, is Screen.Folder -> {
                    val item = selectedItem()
                    when {
                        item?.app != null -> push(Screen.AppActions(item.app.key))
                        item?.folderId != null -> push(Screen.FolderActions(item.folderId))
                        else -> item?.onSelect?.invoke()
                    }
                }
                else -> activateSelected()
            }
            PhoneKey.SOFT_RIGHT -> if (screen == Screen.Search) {
                input = input.backspace()
                selection = 0
                if (input.isEmpty) resetToIdle()
            } else {
                back()
            }
            else -> onDigit(key.char!!)
        }
    }

    fun onLongKey(key: PhoneKey) {
        if (screen is Screen.NameFolder) {
            if (key == PhoneKey.SOFT_RIGHT) nameEntry = MultiTapText() else onKey(key)
            return
        }
        when (key) {
            PhoneKey.SOFT_RIGHT -> resetToIdle()
            PhoneKey.K1, PhoneKey.STAR, PhoneKey.HASH -> open(Screen.PickFavorite(key.char!!))
            else -> onKey(key)
        }
    }

    private fun onDigit(c: Char) {
        if (screen.numbered && c in '1'..'9') {
            val i = c - '1'
            items().getOrNull(i)?.let {
                selection = i
                it.onSelect()
            }
            return
        }
        when (c) {
            in '2'..'9' -> {
                if (screen != Screen.Search) {
                    input = SearchInput()
                    stack = listOf(Screen.Idle, Screen.Search)
                }
                input = input.press(c, SystemClock.uptimeMillis(), prefs.searchMode == SearchMode.MULTI_TAP)
                selection = 0
            }
            '0' -> open(Screen.AllApps)
            else -> openFavorite(c)
        }
    }

    /** Called once the multi-tap pause has passed, so the next tap of the same key starts a new letter. */
    fun commitInput(pressedAt: Long) {
        if (input.composing && input.lastPressAt == pressedAt) input = input.commit()
        if (nameEntry.composing && nameEntry.lastPressAt == pressedAt) nameEntry = nameEntry.commit()
    }

    private fun onNameKey(screen: Screen.NameFolder, key: PhoneKey) {
        when (key) {
            PhoneKey.SOFT_LEFT, PhoneKey.CALL -> {
                val name = nameEntry.text.trim().ifEmpty { "Folder ${prefs.folders.size + 1}" }
                if (screen.folderId == null) {
                    prefs.createFolder(name, screen.appKey)
                } else {
                    prefs.renameFolder(screen.folderId, name)
                }
                back()
            }
            PhoneKey.SOFT_RIGHT -> if (nameEntry.text.isEmpty()) back() else nameEntry = nameEntry.backspace()
            PhoneKey.END -> resetToIdle()
            PhoneKey.UP, PhoneKey.DOWN -> Unit
            else -> nameEntry = nameEntry.press(key.char!!, SystemClock.uptimeMillis())
        }
    }

    private fun startNaming(folderId: Long?, appKey: String?) {
        nameEntry = MultiTapText(text = folderId?.let { prefs.folder(it)?.name }.orEmpty())
        push(Screen.NameFolder(folderId, appKey))
    }

    private fun deleteFolder(id: Long) {
        prefs.deleteFolder(id)
        stack = stack.filterNot { it == Screen.Folder(id) || it == Screen.FolderActions(id) || it == Screen.FolderApps(id) }
        selection = 0
    }

    // ---- Navigation ------------------------------------------------------------------------

    /** Opens a screen directly from the idle screen, dropping any search in progress. */
    fun open(screen: Screen) {
        input = SearchInput()
        stack = listOf(Screen.Idle, screen)
        selection = 0
    }

    private fun push(screen: Screen) {
        stack = stack + screen
        selection = 0
    }

    fun back() {
        if (stack.size <= 1) return
        if (screen == Screen.Search) input = SearchInput()
        stack = stack.dropLast(1)
        selection = 0
    }

    fun resetToIdle() {
        input = SearchInput()
        stack = listOf(Screen.Idle)
        selection = 0
    }

    /** Home button pressed while the launcher is showing. */
    fun home() {
        resetToIdle()
        editingWidgetPage = null
        widgetPickerPage = null
        homeSignal++
    }

    private fun moveSelection(delta: Int) {
        val size = items().size
        if (size == 0) return
        selection = (selection + delta).mod(size)
    }

    private fun selectedItem(): LcdItem? = items().getOrNull(selection)

    private fun activateSelected() {
        selectedItem()?.onSelect?.invoke()
    }

    fun launch(app: AppEntry) {
        actions.launch(app)
        prefs.pushRecent(app.key)
        resetToIdle()
    }

    fun expandNotifications() = actions.expandNotifications()

    // ---- What the LCD lists ----------------------------------------------------------------

    fun items(): List<LcdItem> = when (val s = screen) {
        Screen.Idle -> emptyList()
        Screen.Search -> folderResults.map(::folderItem) + results.map(::appItem)
        Screen.AllApps -> sortedFolders.map(::folderItem) + visibleApps.map(::appItem)
        Screen.Recents -> prefs.recents.mapNotNull { repo.byKey[it] }.map(::appItem)
        Screen.HiddenApps -> repo.apps.filter { it.key in prefs.hiddenApps }.map { app ->
            LcdItem(app.key, app.label, app = app, value = "Show") { prefs.toggleHidden(app.key) }
        }
        is Screen.PickFavorite -> listOf(
            LcdItem("none", "(Empty)", checked = prefs.favorites[s.key] == null) {
                prefs.setFavorite(s.key, null)
                back()
            },
        ) + sortedFolders.map { folder ->
            LcdItem("folder:${folder.id}", folder.name, folderId = folder.id) {
                prefs.setFavorite(s.key, Prefs.folderFavorite(folder.id))
                back()
            }
        } + visibleApps.map { app ->
            LcdItem(app.key, app.label, app = app) {
                prefs.setFavorite(s.key, app.key)
                back()
            }
        }
        Screen.Menu -> menuItems()
        is Screen.Choice -> choiceItems(s.setting)
        Screen.Favorites -> Prefs.FAVORITE_KEYS.map { k ->
            LcdItem("fav$k", "Key $k", value = favoriteLabel(k) ?: "Empty") { push(Screen.PickFavorite(k)) }
        }
        is Screen.AppActions -> repo.byKey[s.appKey]?.let(::appActionItems) ?: emptyList()
        Screen.Folders -> listOf(LcdItem("new", "+ New folder") { startNaming(null, null) }) +
            sortedFolders.map(::folderItem)
        is Screen.Folder -> prefs.folder(s.id)?.let { folderApps(it).map(::appItem) } ?: emptyList()
        is Screen.FolderApps -> prefs.folder(s.id)?.let { folder ->
            visibleApps.map { app ->
                LcdItem(app.key, app.label, app = app, checked = app.key in folder.apps) {
                    prefs.toggleInFolder(s.id, app.key)
                }
            }
        } ?: emptyList()
        is Screen.AddToFolder -> listOf(LcdItem("new", "+ New folder") { startNaming(null, s.appKey) }) +
            sortedFolders.map { folder ->
                LcdItem("folder:${folder.id}", folder.name, folderId = folder.id, checked = s.appKey in folder.apps) {
                    prefs.toggleInFolder(folder.id, s.appKey)
                }
            }
        is Screen.FolderActions -> prefs.folder(s.id)?.let(::folderActionItems) ?: emptyList()
        is Screen.NameFolder -> emptyList()
    }

    private fun appItem(app: AppEntry) = LcdItem(
        id = app.key,
        title = app.label,
        app = app,
        onLongPress = { push(Screen.AppActions(app.key)) },
    ) { launch(app) }

    private fun folderItem(folder: AppFolder) = LcdItem(
        id = "folder:${folder.id}",
        title = folder.name,
        folderId = folder.id,
        value = folderApps(folder).size.toString(),
        onLongPress = { push(Screen.FolderActions(folder.id)) },
    ) { push(Screen.Folder(folder.id)) }

    private fun folderActionItems(folder: AppFolder): List<LcdItem> = listOf(
        LcdItem("open", "Open") { push(Screen.Folder(folder.id)) },
        LcdItem("apps", "Add / remove apps") { push(Screen.FolderApps(folder.id)) },
        LcdItem("rename", "Rename") { startNaming(folder.id, null) },
        LcdItem("delete", "Delete folder") { deleteFolder(folder.id) },
    ) + Prefs.FAVORITE_KEYS.map { k ->
        val assigned = prefs.favorites[k] == Prefs.folderFavorite(folder.id)
        LcdItem("fav$k", "On key $k", checked = assigned) {
            prefs.setFavorite(k, if (assigned) null else Prefs.folderFavorite(folder.id))
        }
    }

    private fun appActionItems(app: AppEntry): List<LcdItem> {
        val hidden = app.key in prefs.hiddenApps
        // Opened from inside a folder: offer to take it back out.
        val insideFolder = stack.getOrNull(stack.size - 2) as? Screen.Folder
        return listOfNotNull(
            LcdItem("open", "Open") { launch(app) },
            LcdItem("folder", "Add to folder") { push(Screen.AddToFolder(app.key)) },
            insideFolder?.let { f ->
                LcdItem("unfolder", "Remove from folder") {
                    prefs.toggleInFolder(f.id, app.key)
                    back()
                }
            },
            LcdItem("info", "App info") { actions.openAppInfo(app) },
            LcdItem("uninstall", "Uninstall") {
                actions.uninstall(app)
                back()
            },
            LcdItem("hide", if (hidden) "Unhide" else "Hide") {
                prefs.toggleHidden(app.key)
                back()
            },
        ) + Prefs.FAVORITE_KEYS.map { k ->
            val assigned = prefs.favorites[k] == app.key
            LcdItem("fav$k", "On key $k", checked = assigned) {
                prefs.setFavorite(k, if (assigned) null else app.key)
            }
        }
    }

    private fun menuItems(): List<LcdItem> {
        fun setting(s: Setting, value: String) = LcdItem(s.name, s.title, value = value) { push(Screen.Choice(s)) }
        fun onOff(b: Boolean) = if (b) "On" else "Off"
        return listOf(
            setting(Setting.PHONE_STYLE, prefs.phoneStyle.label),
            setting(Setting.SEARCH_MODE, prefs.searchMode.label),
            setting(Setting.KEY_SOUND, prefs.keySound.label),
            setting(Setting.VIBRATION, onOff(prefs.vibration)),
            setting(Setting.STATUS_BAR, if (prefs.retroStatusBar) "Retro" else "Android"),
            setting(Setting.UNFOLD_ANIMATION, onOff(prefs.unfoldAnimation)),
            setting(Setting.MIRROR_INSIDE, onOff(prefs.mirrorInside)),
            setting(Setting.ICON_STYLE, if (prefs.retroIcons) "LCD" else "Colour"),
            LcdItem("favorites", "Favourite keys") { push(Screen.Favorites) },
            LcdItem("folders", "Folders", value = prefs.folders.size.toString()) { push(Screen.Folders) },
            LcdItem("hidden", "Hidden apps", value = prefs.hiddenApps.size.toString()) { push(Screen.HiddenApps) },
            LcdItem("home", "Set as home app") { actions.openHomeSettings() },
        )
    }

    private fun choiceItems(setting: Setting): List<LcdItem> {
        fun <T> options(current: T, vararg choices: Pair<String, T>, apply: (T) -> Unit) =
            choices.map { (label, value) ->
                LcdItem(label, label, checked = value == current) {
                    apply(value)
                    back()
                }
            }
        return when (setting) {
            Setting.PHONE_STYLE -> options(
                prefs.phoneStyle,
                *PhoneStyle.entries.map { it.label to it }.toTypedArray(),
            ) { prefs.phoneStyle = it }
            Setting.SEARCH_MODE -> options(
                prefs.searchMode,
                *SearchMode.entries.map { it.label to it }.toTypedArray(),
            ) { prefs.searchMode = it; input = SearchInput() }
            Setting.KEY_SOUND -> options(
                prefs.keySound,
                *KeySound.entries.map { it.label to it }.toTypedArray(),
            ) { prefs.keySound = it }
            Setting.VIBRATION -> options(prefs.vibration, "On" to true, "Off" to false) { prefs.vibration = it }
            Setting.STATUS_BAR -> options(prefs.retroStatusBar, "Android" to false, "Retro" to true) {
                prefs.retroStatusBar = it
            }
            Setting.UNFOLD_ANIMATION -> options(prefs.unfoldAnimation, "On" to true, "Off" to false) {
                prefs.unfoldAnimation = it
            }
            Setting.MIRROR_INSIDE -> options(prefs.mirrorInside, "On" to true, "Off" to false) {
                prefs.mirrorInside = it
            }
            Setting.ICON_STYLE -> options(prefs.retroIcons, "Colour" to false, "LCD" to true) {
                prefs.retroIcons = it
            }
        }
    }

    fun title(): String = when (val s = screen) {
        Screen.Idle, Screen.Search -> ""
        Screen.AllApps -> "All apps"
        Screen.Recents -> "Recent apps"
        Screen.HiddenApps -> "Hidden apps"
        is Screen.PickFavorite -> "Key ${s.key} opens"
        Screen.Menu -> "Menu"
        is Screen.Choice -> s.setting.title
        Screen.Favorites -> "Favourite keys"
        is Screen.AppActions -> repo.byKey[s.appKey]?.label ?: "App"
        Screen.Folders -> "Folders"
        is Screen.Folder -> prefs.folder(s.id)?.name ?: "Folder"
        is Screen.FolderApps -> "Apps in ${prefs.folder(s.id)?.name ?: "folder"}"
        is Screen.AddToFolder -> "Add to folder"
        is Screen.FolderActions -> prefs.folder(s.id)?.name ?: "Folder"
        is Screen.NameFolder -> if (s.folderId == null) "New folder" else "Rename folder"
    }

    /** Labels shown above the left soft key, green key, and right soft key. */
    fun softLabels(): Triple<String, String, String> = when (screen) {
        Screen.Idle -> Triple("Menu", "", "")
        Screen.Search -> Triple("Options", "Open", "Clear")
        Screen.AllApps, Screen.Recents, Screen.Folders, is Screen.Folder -> Triple("Options", "Open", "Back")
        is Screen.NameFolder -> Triple("Save", "", if (nameEntry.text.isEmpty()) "Back" else "Clear")
        else -> Triple("Select", "", "Back")
    }

    fun emptyMessage(): String = when (screen) {
        Screen.Search -> "No apps"
        Screen.Recents -> "No recent apps"
        Screen.HiddenApps -> "No hidden apps"
        is Screen.Folder -> "Empty folder"
        else -> "Empty"
    }
}
