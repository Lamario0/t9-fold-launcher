package com.lamario.t9fold.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T9Test {
    private val apps = listOf("Calculator", "Google Maps", "Jellyfin", "Keep Notes", "Kindle", "Lens", "Spotify", "YouTube")
    private val index = SearchIndex(apps) { it }

    private fun tap(vararg keys: Char, multiTap: Boolean = true, gapMs: Long = 100): SearchInput {
        var input = SearchInput()
        var now = 0L
        for (k in keys) {
            now += gapMs
            input = input.press(k, now, multiTap)
        }
        return input
    }

    @Test
    fun repeatedTapsCycleThroughLettersThenAll() {
        assertEquals(listOf("J"), tap('5').slots.map { it.label })
        assertEquals(listOf("K"), tap('5', '5').slots.map { it.label })
        assertEquals(listOf("L"), tap('5', '5', '5').slots.map { it.label })
        assertEquals(listOf("JKL"), tap('5', '5', '5', '5').slots.map { it.label })
        assertEquals(listOf("J"), tap('5', '5', '5', '5', '5').slots.map { it.label })
    }

    @Test
    fun multiTapFiltersByFirstLetter() {
        assertEquals(listOf("Jellyfin"), index.search(tap('5'), SearchMode.MULTI_TAP))
        assertEquals(listOf("Keep Notes", "Kindle"), index.search(tap('5', '5'), SearchMode.MULTI_TAP))
        assertEquals(listOf("Lens"), index.search(tap('5', '5', '5'), SearchMode.MULTI_TAP))
        assertEquals(
            listOf("Jellyfin", "Keep Notes", "Kindle", "Lens"),
            index.search(tap('5', '5', '5', '5'), SearchMode.MULTI_TAP),
        )
    }

    @Test
    fun pausingStartsTheNextLetter() {
        val input = tap('5', '5', gapMs = 100).let { it.press('5', 5_000, multiTap = true) }
        assertEquals(listOf("K", "J"), input.slots.map { it.label })
    }

    @Test
    fun differentKeyStartsTheNextLetter() {
        // K then I -> "Ki"ndle
        val input = tap('5', '5', '4', '4', '4')
        assertEquals(listOf("K", "I"), input.slots.map { it.label })
        assertEquals(listOf("Kindle"), index.search(input, SearchMode.MULTI_TAP))
    }

    @Test
    fun t9WordsMatchesStartOfAnyWord() {
        // 6-2-7 spells "map" -> Google Maps
        assertEquals(listOf("Google Maps"), index.search(tap('6', '2', '7', multiTap = false), SearchMode.T9_WORDS))
        // 8-8-2 spells "tub" -> You*Tub*e (camel case split)
        assertEquals(listOf("YouTube"), index.search(tap('8', '8', '2', multiTap = false), SearchMode.T9_WORDS))
    }

    @Test
    fun t9WordsRanksNameStartFirst() {
        val idx = SearchIndex(listOf("Camera", "Bluetooth Audio", "Amazon")) { it }
        // 2: Amazon/Bluetooth Audio/Camera start with 2; "Audio" is only a word match for B.A.
        assertEquals(listOf("Amazon", "Bluetooth Audio", "Camera"), idx.t9Words("2"))
        assertEquals(listOf("Bluetooth Audio"), idx.t9Words("283"))
    }

    @Test
    fun accentsAreIgnored() {
        val idx = SearchIndex(listOf("Café Finder")) { it }
        assertEquals(listOf("Café Finder"), idx.t9Words("2233"))
    }

    @Test
    fun backspaceStopsComposing() {
        val input = tap('5', '5').backspace()
        assertTrue(input.isEmpty)
        assertFalse(input.composing)
    }
}
