package com.example.cicompanion.calendar

import android.text.Html

/** Formats raw event text from the calendar feed into plain text for display. */
fun formatEventTextForDisplay(rawText: String): String {
    val withLineBreaks = replaceHtmlBreakTags(rawText)
    val plainText = decodeHtmlToPlainText(withLineBreaks)
    return cleanupEventText(plainText)
}

/** Replaces common HTML break and paragraph tags with newline characters. */
private fun replaceHtmlBreakTags(text: String): String {
    return text
        .replace("<br>", "\n", ignoreCase = true)
        .replace("<br/>", "\n", ignoreCase = true)
        .replace("<br />", "\n", ignoreCase = true)
        .replace("</p>", "\n", ignoreCase = true)
        .replace("<p>", "", ignoreCase = true)
}

/** Decodes HTML entities and removes any remaining HTML tags. */
private fun decodeHtmlToPlainText(text: String): String {
    return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()
}

/** Removes extra spacing so the event text reads cleanly on screen. */
private fun cleanupEventText(text: String): String {
    return text
        .replace('\u00A0', ' ')
        .replace(Regex("\\n\\s*\\n+"), "\n\n")
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .trim()
}
