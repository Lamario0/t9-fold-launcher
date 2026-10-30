package com.lamario.t9fold.search

import java.text.Normalizer

enum class SearchMode(val label: String) {
    /** Tap a key repeatedly to cycle J, K, L, then "any of JKL"; matches the start of the name. */
    MULTI_TAP("Multi-tap"),

    /** Classic predictive T9: one tap per letter, matches the start of any word in the name. */
    T9_WORDS("T9 words"),
}

object T9 {
    val KEY_LETTERS: Map<Char, String> = mapOf(
        '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz",
    )

    private val LETTER_TO_DIGIT: Map<Char, Char> = buildMap {
        for ((digit, letters) in KEY_LETTERS) for (c in letters) put(c, digit)
    }
    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
    private val CAMEL_HUMP = Regex("(?<=\\p{Ll})(?=\\p{Lu})")

    /** "YouTube Music" -> [you, tube, music]; accents are dropped so "Café" matches 2-2-3-3. */
    fun words(label: String): List<String> {
        val plain = COMBINING_MARKS.replace(Normalizer.normalize(label, Normalizer.Form.NFD), "")
        return plain.split(NON_ALPHANUMERIC)
            .flatMap { it.split(CAMEL_HUMP) }
            .map { it.lowercase() }
            .filter { it.isNotEmpty() }
    }

    /** Maps letters to their key digit; digits stay themselves, anything else maps to '1'. */
    fun digitsOf(text: String): String = buildString {
        for (c in text) append(LETTER_TO_DIGIT[c] ?: if (c in '0'..'9') c else '1')
    }
}

/** One key position in the query: which key, and how many times it was tapped. */
data class KeySlot(val key: Char, val taps: Int = 1) {
    val letters: String get() = T9.KEY_LETTERS[key].orEmpty()

    /** Index into [letters]; `letters.length` means "any letter on this key". */
    private val choice: Int get() = if (letters.isEmpty()) 0 else (taps - 1) % (letters.length + 1)

    val isWildcard: Boolean get() = letters.isEmpty() || choice == letters.length

    fun matches(c: Char): Boolean = if (isWildcard) c in letters else c == letters[choice]

    /** What the LCD shows for this slot: "K", or "JKL" for the wildcard. */
    val label: String get() = if (isWildcard) letters.uppercase() else letters[choice].uppercase()
}

data class SearchInput(
    val slots: List<KeySlot> = emptyList(),
    val lastPressAt: Long = 0L,
    /** True while another tap of the same key would still cycle the last slot. */
    val composing: Boolean = false,
) {
    val isEmpty: Boolean get() = slots.isEmpty()
    val digits: String get() = slots.joinToString("") { it.key.toString() }

    fun press(key: Char, now: Long, multiTap: Boolean, timeoutMs: Long = MULTI_TAP_TIMEOUT_MS): SearchInput {
        val last = slots.lastOrNull()
        val cycles = multiTap && composing && last != null && last.key == key && now - lastPressAt <= timeoutMs
        return if (cycles) {
            copy(slots = slots.dropLast(1) + last!!.copy(taps = last.taps + 1), lastPressAt = now)
        } else {
            copy(slots = slots + KeySlot(key), lastPressAt = now, composing = multiTap)
        }
    }

    fun commit(): SearchInput = copy(composing = false)

    fun backspace(): SearchInput = copy(slots = slots.dropLast(1), composing = false)

    companion object {
        const val MULTI_TAP_TIMEOUT_MS = 1000L
    }
}

class SearchIndex<T>(items: List<T>, label: (T) -> String) {
    private class Entry<T>(val item: T, val sortKey: String, words: List<String>) {
        val compact = words.joinToString("")
        val compactDigits = T9.digitsOf(compact)
        val wordDigits = words.map { T9.digitsOf(it) }
    }

    private val entries = items.map { item ->
        val name = label(item)
        Entry(item, name.lowercase(), T9.words(name))
    }

    fun search(input: SearchInput, mode: SearchMode): List<T> = when (mode) {
        SearchMode.MULTI_TAP -> multiTap(input.slots)
        SearchMode.T9_WORDS -> t9Words(input.digits)
    }

    /** Names whose first letters match the tapped letters, in the order the items were given. */
    fun multiTap(slots: List<KeySlot>): List<T> {
        if (slots.isEmpty()) return emptyList()
        return entries.filter { e ->
            e.compact.length >= slots.size && slots.indices.all { slots[it].matches(e.compact[it]) }
        }.map { it.item }
    }

    /** Names where the whole name, or any word in it, starts with the key sequence. */
    fun t9Words(digits: String): List<T> {
        if (digits.isEmpty()) return emptyList()
        return entries.mapNotNull { e ->
            val rank = when {
                e.compactDigits.startsWith(digits) -> 0
                e.wordDigits.any { it.startsWith(digits) } -> 1
                else -> return@mapNotNull null
            }
            rank to e
        }.sortedWith(compareBy({ it.first }, { it.second.sortKey })).map { it.second.item }
    }
}
