package travel.roame.takehome

private val whitespace = Regex("\\s+")

/** Trims, lowercases and collapses inner whitespace: "  New   York " -> "new york". */
internal fun normalizeSearch(text: String): String = text.trim().lowercase().replace(whitespace, " ")

/**
 * One search field (origin or destination). Decided 2026-11-03, see docs/NOTES.md §2.1:
 * text that equals a known airport code matches that code only ("dub" is Dublin, not Dubai);
 * otherwise the code must start with the text, or the city must start with it at a word boundary.
 */
internal class AirportMatcher(text: String, knownCodes: Set<String>) {
    private val query = normalizeSearch(text)
    private val exactCode = query.uppercase().takeIf { query.isNotEmpty() && it in knownCodes }

    val isActive: Boolean get() = query.isNotEmpty()

    /** [cityKey] must already be normalized with [normalizeSearch]. */
    fun matches(code: String, cityKey: String): Boolean = when {
        query.isEmpty() -> true
        exactCode != null -> code == exactCode
        else -> code.lowercase().startsWith(query) || cityStartsAtWordBoundary(cityKey)
    }

    private fun cityStartsAtWordBoundary(cityKey: String): Boolean {
        if (cityKey.startsWith(query)) return true
        var space = cityKey.indexOf(' ')
        while (space >= 0) {
            if (cityKey.startsWith(query, startIndex = space + 1)) return true
            space = cityKey.indexOf(' ', space + 1)
        }
        return false
    }
}
