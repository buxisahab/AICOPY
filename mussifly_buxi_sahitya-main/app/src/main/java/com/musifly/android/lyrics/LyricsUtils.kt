package com.musifly.android.lyrics

import android.text.format.DateUtils
import com.musifly.android.ui.component.ANIMATE_SCROLL_DURATION

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    // Relaxed Regex: Allows optional milliseconds (.xx or .xxx) and handles spacing
    val LINE_REGEX = "^((?:\\[\\d{2}:\\d{2}(?:\\.\\d{2,3})?\\]\\s*)+)(.*)$".toRegex()
    val TIME_REGEX = "\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?\\]".toRegex()

    fun parseLyrics(lyrics: String): List<LyricsEntry> =
        lyrics
            .lines()
            .mapNotNull { line ->
                parseLine(line)
            }.flatten()
            .sorted()

    private fun parseLine(line: String): List<LyricsEntry>? {
        if (line.isBlank()) return null
        
        val matchResult = LINE_REGEX.find(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        val text = matchResult.groupValues[2].trim() // text is in group 2 now
        
        val timeMatchResults = TIME_REGEX.findAll(times)

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                
                var mil = 0L
                if (milString.isNotEmpty()) {
                    mil = milString.toLong()
                    if (milString.length == 2) {
                        mil *= 10
                    }
                }
                
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                LyricsEntry(time, text)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        for (index in lines.indices) {
            if (lines[index].time >= position + ANIMATE_SCROLL_DURATION) {
                return index - 1
            }
        }
        return lines.lastIndex
    }
}
