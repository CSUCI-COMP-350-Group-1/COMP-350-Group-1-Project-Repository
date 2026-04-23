package com.example.cicompanion.utils

import android.os.Build
import android.text.Html
import androidx.core.text.HtmlCompat

object HtmlUtils {
    /**
     * Aggressively strips HTML tags and unescapes entities.
     * Handles multiple levels of encoding.
     */
    fun stripHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        
        var result = html
        
        // Replace the block tags with newlines to keep it readable
        result = result.replace(Regex("(?i)<br\\s*/?>"), "\n")
        result = result.replace(Regex("(?i)</p>"), "\n")
        result = result.replace(Regex("(?i)</div>"), "\n")
        result = result.replace(Regex("(?i)<li>"), "\n• ")
        
        // Use HtmlCompat for initial strip and entity unescaping
        result = HtmlCompat.fromHtml(result, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        
        // If it looks like there are still tags (e.g. double escaped &lt;p&gt;), strip again
        if (result.contains("<") || result.contains("&")) {
            result = HtmlCompat.fromHtml(result, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        }
        
        return result.trim()
    }
}
