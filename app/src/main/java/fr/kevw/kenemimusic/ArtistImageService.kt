package fr.kevw.kenemimusic

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

/**
 * Service pour récupérer les images des artistes via Deezer API
 * Aucune clé API n'est nécessaire !
 */
class ArtistImageService(private val context: Context) {

    /**
     * Récupère l'URL de l'image d'un artiste depuis Deezer
     * @param artistName Nom de l'artiste
     * @return URL de l'image en haute qualité ou null si non trouvée
     */
    suspend fun getArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        try {
            if (artistName.isBlank() || artistName == "Artiste inconnu") {
                return@withContext null
            }

            // Nettoyer le nom de l'artiste (enlever les feat., etc.)
            val cleanArtistName = cleanArtistName(artistName)
            val encodedArtist = URLEncoder.encode(cleanArtistName, "UTF-8")
            val urlString = "https://api.deezer.com/search/artist?q=$encodedArtist&limit=1"

            val response = URL(urlString).readText()
            val json = JSONObject(response)

            if (json.has("data")) {
                val data = json.getJSONArray("data")
                if (data.length() > 0) {
                    val artist = data.getJSONObject(0)

                    // Vérifier que c'est bien le bon artiste (correspondance approximative)
                    val foundArtistName = artist.optString("name", "")
                    if (!isArtistMatch(cleanArtistName, foundArtistName)) {
                        Log.w("ArtistImageService", "Artiste non correspondant: cherché '$cleanArtistName', trouvé '$foundArtistName'")
                        return@withContext null
                    }

                    // Deezer fournit plusieurs tailles d'images, prendre la meilleure qualité
                    return@withContext when {
                        artist.has("picture_xl") -> artist.getString("picture_xl")
                        artist.has("picture_big") -> artist.getString("picture_big")
                        artist.has("picture_medium") -> artist.getString("picture_medium")
                        else -> null
                    }
                }
            }

            return@withContext null

        } catch (e: Exception) {
            Log.e("ArtistImageService", "Erreur lors de la récupération de l'image pour $artistName", e)
            return@withContext null
        }
    }

    /**
     * Nettoie le nom de l'artiste pour améliorer les résultats de recherche
     */
    private fun cleanArtistName(artistName: String): String {
        return artistName
            .replace(Regex("\\(.*?\\)"), "") // Enlever tout entre parenthèses
            .replace(Regex("\\[.*?\\]"), "") // Enlever tout entre crochets
            .replace(Regex("(?i)feat\\.?.*"), "") // Enlever "feat." et ce qui suit
            .replace(Regex("(?i)ft\\.?.*"), "") // Enlever "ft." et ce qui suit
            .replace(Regex("(?i)featuring.*"), "") // Enlever "featuring" et ce qui suit
            .replace(Regex("\\s+"), " ") // Normaliser les espaces
            .trim()
    }

    /**
     * Vérifie si deux noms d'artistes correspondent approximativement
     */
    private fun isArtistMatch(searched: String, found: String): Boolean {
        val normalizedSearched = searched.lowercase().trim()
        val normalizedFound = found.lowercase().trim()

        // Correspondance exacte
        if (normalizedSearched == normalizedFound) return true

        // L'un contient l'autre
        if (normalizedFound.contains(normalizedSearched) || normalizedSearched.contains(normalizedFound)) {
            return true
        }

        // Similarité de Levenshtein (distance d'édition)
        val distance = levenshteinDistance(normalizedSearched, normalizedFound)
        val maxLength = maxOf(normalizedSearched.length, normalizedFound.length)
        val similarity = 1.0 - (distance.toDouble() / maxLength)

        // Accepter si similarité > 70%
        return similarity > 0.7
    }

    /**
     * Calcule la distance de Levenshtein entre deux chaînes
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // suppression
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Récupère l'URL de la pochette d'un album depuis Deezer
     * @param albumName Nom de l'album
     * @param artistName Nom de l'artiste (pour affiner la recherche)
     * @return URL de la pochette en haute qualité ou null si non trouvée
     */
    suspend fun getAlbumCoverUrl(albumName: String, artistName: String = ""): String? = withContext(Dispatchers.IO) {
        try {
            if (albumName.isBlank() || albumName == "Album inconnu") {
                return@withContext null
            }

            val cleanAlbumName = albumName.trim()
            val cleanArtistName = cleanArtistName(artistName)

            // Recherche combinée album + artiste pour plus de précision
            val searchQuery = if (cleanArtistName.isNotBlank()) {
                "$cleanAlbumName $cleanArtistName"
            } else {
                cleanAlbumName
            }

            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val urlString = "https://api.deezer.com/search/album?q=$encodedQuery&limit=1"

            val response = URL(urlString).readText()
            val json = JSONObject(response)

            if (json.has("data")) {
                val data = json.getJSONArray("data")
                if (data.length() > 0) {
                    val album = data.getJSONObject(0)

                    // Deezer fournit plusieurs tailles de pochettes
                    return@withContext when {
                        album.has("cover_xl") -> album.getString("cover_xl")
                        album.has("cover_big") -> album.getString("cover_big")
                        album.has("cover_medium") -> album.getString("cover_medium")
                        else -> null
                    }
                }
            }

            return@withContext null

        } catch (e: Exception) {
            Log.e("ArtistImageService", "Erreur lors de la récupération de la pochette pour $albumName", e)
            return@withContext null
        }
    }
}