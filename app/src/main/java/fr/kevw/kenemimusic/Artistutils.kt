package fr.kevw.kenemimusic

/**
 * Parse le champ artist d'une chanson pour extraire tous les artistes individuels.
 * Gère : "A feat. B", "A & B", "A / B", "A/B", "A; B", "A, B & C", "A (feat. B)", "A x B"...
 */
object ArtistUtils {

    /**
     * Noms d'artistes qui contiennent un séparateur mais sont UN SEUL artiste.
     * Ajoute ici ceux qui sont mal découpés dans ta bibliothèque.
     */
    private val PROTECTED_NAMES = listOf(
        "AC/DC", "Tyler, The Creator", "Earth, Wind & Fire", "Simon & Garfunkel",
        "Hall & Oates", "Years & Years", "Florence + The Machine", "Brooks & Dunn",
        "Crosby, Stills, Nash & Young", "Mumford & Sons"
    )

    // Mots-séparateurs entourés d'espaces  |  virgule + espace  |  "/" ou ";" avec ou sans espaces
    private val SPLIT_REGEX = Regex(
        "(?i)\\s+(?:feat\\.?|ft\\.?|featuring|vs\\.?|with|x|&)\\s+|\\s*,\\s+|\\s*[;/]\\s*"
    )
    private val FEAT_IN_BRACKETS = Regex("(?i)[(\\[]\\s*(?:feat\\.?|ft\\.?|featuring|with)\\s+")

    fun parseArtists(artistField: String): List<String> {
        if (artistField.isBlank() || artistField == "Artiste inconnu") {
            return listOf(artistField)
        }

        var s = artistField.trim()

        // "A (feat. B)" -> "A feat. B"
        if (FEAT_IN_BRACKETS.containsMatchIn(s)) {
            s = FEAT_IN_BRACKETS.replace(s, " feat. ").trimEnd(')', ']').trim()
        }

        // Protège les noms qui contiennent un séparateur
        val found = PROTECTED_NAMES.filter { s.contains(it, ignoreCase = true) }
        found.forEachIndexed { i, name -> s = s.replace(name, "@@${i}@@", ignoreCase = true) }

        val parts = s.split(SPLIT_REGEX)
            .map { part ->
                var p = part.trim()
                found.forEachIndexed { i, name -> p = p.replace("@@${i}@@", name) }
                p
            }
            .filter { it.isNotBlank() }
            .distinct()

        return parts.ifEmpty { listOf(artistField.trim()) }
    }

    fun songBelongsToArtist(song: Song, artistName: String): Boolean =
        parseArtists(song.artist).any { it.equals(artistName, ignoreCase = true) }

    fun primaryArtist(artistField: String): String =
        parseArtists(artistField).firstOrNull() ?: artistField
}
