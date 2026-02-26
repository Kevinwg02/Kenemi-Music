package fr.kevw.kenemimusic

/**
 * Parse le champ artist d'une chanson pour extraire tous les artistes individuels.
 * Gère les formats courants : "Artist1 feat. Artist2", "Artist1 & Artist2", etc.
 */
object ArtistUtils {

    // Séparateurs courants entre artistes
    private val SEPARATORS = listOf(
        " feat. ", " feat ", " ft. ", " ft ", " featuring ",
        " & ", " x ", " X ", " vs. ", " vs ", " with ",
        ", ", " / ", "/"
    )

    /**
     * Retourne la liste de tous les artistes présents dans le champ artist.
     * Ex: "Drake feat. Lil Baby" → ["Drake", "Lil Baby"]
     * Ex: "Jay-Z & Kanye West" → ["Jay-Z", "Kanye West"]
     */
    fun parseArtists(artistField: String): List<String> {
        if (artistField.isBlank() || artistField == "Artiste inconnu") {
            return listOf(artistField)
        }

        var remaining = artistField.trim()
        val artists = mutableListOf<String>()

        // Chercher le premier séparateur présent
        for (sep in SEPARATORS) {
            val idx = remaining.indexOf(sep, ignoreCase = true)
            if (idx != -1) {
                // Artiste principal (avant le séparateur)
                val main = remaining.substring(0, idx).trim()
                if (main.isNotBlank()) artists.add(main)

                // Le reste peut contenir d'autres artistes
                val rest = remaining.substring(idx + sep.length).trim()
                // Retirer les parenthèses éventuelles autour de la feature
                val clean = rest.removeSuffix(")").removePrefix("(").trim()
                if (clean.isNotBlank()) artists.add(clean)

                return artists.distinct()
            }
        }

        // Aucun séparateur trouvé → un seul artiste
        return listOf(artistField.trim())
    }

    /**
     * Vérifie si un artiste donné est l'un des artistes d'une chanson.
     */
    fun songBelongsToArtist(song: Song, artistName: String): Boolean {
        val songArtists = parseArtists(song.artist)
        return songArtists.any { it.equals(artistName, ignoreCase = true) }
    }

    /**
     * Retourne le nom "principal" (avant toute feature) pour l'affichage.
     */
    fun primaryArtist(artistField: String): String {
        return parseArtists(artistField).firstOrNull() ?: artistField
    }
}