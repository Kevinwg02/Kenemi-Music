package fr.kevw.kenemimusic

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

/**
 * Service pour récupérer les images des artistes via Deezer API avec système de cache
 * Aucune clé API n'est nécessaire !
 */
class ArtistImageService(private val context: Context) {

    // ===== SYSTÈME DE CACHE =====
    private val cachePrefs: SharedPreferences =
        context.getSharedPreferences("artist_image_cache", Context.MODE_PRIVATE)

    companion object {
        private const val CACHE_PREFIX_ARTIST = "artist_"
        private const val CACHE_PREFIX_ALBUM = "album_"
        private const val CACHE_EXPIRY_DAYS = 30 // Cache valide pendant 30 jours
    }

    /**
     * Récupère l'URL de l'image d'un artiste depuis le cache ou l'API Deezer
     * @param artistName Nom de l'artiste
     * @return URL de l'image en haute qualité ou null si non trouvée
     */
    suspend fun getArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        try {
            if (artistName.isBlank() || artistName == "Artiste inconnu") {
                return@withContext null
            }

            val cleanArtistName = cleanArtistName(artistName)
            val cacheKey = CACHE_PREFIX_ARTIST + cleanArtistName.lowercase()

            // ✅ 1. VÉRIFIER LE CACHE D'ABORD
            val cachedUrl = getCachedImageUrl(cacheKey)
            if (cachedUrl != null) {
                Log.d("ArtistImageService", "Image trouvée en cache pour: $cleanArtistName")
                return@withContext cachedUrl
            }

            // ✅ 2. SI PAS EN CACHE, TÉLÉCHARGER DEPUIS L'API
            Log.d("ArtistImageService", "Téléchargement de l'image pour: $cleanArtistName")
            val encodedArtist = URLEncoder.encode(cleanArtistName, "UTF-8")
            val urlString = "https://api.deezer.com/search/artist?q=$encodedArtist&limit=1"

            val response = URL(urlString).readText()
            val json = JSONObject(response)

            if (json.has("data")) {
                val data = json.getJSONArray("data")
                if (data.length() > 0) {
                    val artist = data.getJSONObject(0)

                    // Vérifier que c'est bien le bon artiste
                    val foundArtistName = artist.optString("name", "")
                    if (!isArtistMatch(cleanArtistName, foundArtistName)) {
                        Log.w("ArtistImageService", "Artiste non correspondant: cherché '$cleanArtistName', trouvé '$foundArtistName'")
                        return@withContext null
                    }

                    // Récupérer l'URL de la meilleure qualité
                    val imageUrl = when {
                        artist.has("picture_xl") -> artist.getString("picture_xl")
                        artist.has("picture_big") -> artist.getString("picture_big")
                        artist.has("picture_medium") -> artist.getString("picture_medium")
                        else -> null
                    }

                    // ✅ 3. SAUVEGARDER EN CACHE
                    if (imageUrl != null) {
                        cacheImageUrl(cacheKey, imageUrl)
                        Log.d("ArtistImageService", "Image mise en cache pour: $cleanArtistName")
                    }

                    return@withContext imageUrl
                }
            }

            return@withContext null

        } catch (e: Exception) {
            Log.e("ArtistImageService", "Erreur lors de la récupération de l'image pour $artistName", e)
            return@withContext null
        }
    }

    /**
     * Récupère l'URL de la pochette d'un album depuis le cache ou l'API Deezer
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
            val cacheKey = CACHE_PREFIX_ALBUM + "${cleanAlbumName}_${cleanArtistName}".lowercase()

            // ✅ 1. VÉRIFIER LE CACHE D'ABORD
            val cachedUrl = getCachedImageUrl(cacheKey)
            if (cachedUrl != null) {
                Log.d("ArtistImageService", "Pochette trouvée en cache pour: $cleanAlbumName")
                return@withContext cachedUrl
            }

            // ✅ 2. SI PAS EN CACHE, TÉLÉCHARGER DEPUIS L'API
            Log.d("ArtistImageService", "Téléchargement de la pochette pour: $cleanAlbumName")

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

                    // Récupérer l'URL de la meilleure qualité
                    val coverUrl = when {
                        album.has("cover_xl") -> album.getString("cover_xl")
                        album.has("cover_big") -> album.getString("cover_big")
                        album.has("cover_medium") -> album.getString("cover_medium")
                        else -> null
                    }

                    // ✅ 3. SAUVEGARDER EN CACHE
                    if (coverUrl != null) {
                        cacheImageUrl(cacheKey, coverUrl)
                        Log.d("ArtistImageService", "Pochette mise en cache pour: $cleanAlbumName")
                    }

                    return@withContext coverUrl
                }
            }

            return@withContext null

        } catch (e: Exception) {
            Log.e("ArtistImageService", "Erreur lors de la récupération de la pochette pour $albumName", e)
            return@withContext null
        }
    }

    // ===== GESTION DU CACHE =====

    /**
     * Récupère une URL d'image depuis le cache si elle existe et est valide
     */
    private fun getCachedImageUrl(key: String): String? {
        val cachedData = cachePrefs.getString(key, null) ?: return null

        try {
            val parts = cachedData.split("|")
            if (parts.size != 2) return null

            val url = parts[0]
            val timestamp = parts[1].toLongOrNull() ?: return null

            // Vérifier si le cache n'est pas expiré
            val now = System.currentTimeMillis()
            val expiryTime = timestamp + (CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)

            return if (now < expiryTime) {
                url
            } else {
                // Cache expiré, le supprimer
                cachePrefs.edit().remove(key).apply()
                null
            }
        } catch (e: Exception) {
            Log.e("ArtistImageService", "Erreur lors de la lecture du cache", e)
            return null
        }
    }

    /**
     * Sauvegarde une URL d'image dans le cache avec un timestamp
     */
    private fun cacheImageUrl(key: String, url: String) {
        val timestamp = System.currentTimeMillis()
        val cachedData = "$url|$timestamp"
        cachePrefs.edit().putString(key, cachedData).apply()
    }

    /**
     * Efface tout le cache des images
     */
    fun clearCache() {
        cachePrefs.edit().clear().apply()
        Log.d("ArtistImageService", "Cache d'images effacé")
    }

    /**
     * Efface les entrées expirées du cache
     */
    fun cleanExpiredCache() {
        val allEntries = cachePrefs.all
        val now = System.currentTimeMillis()
        var removedCount = 0

        allEntries.forEach { (key, value) ->
            if (value is String) {
                try {
                    val parts = value.split("|")
                    if (parts.size == 2) {
                        val timestamp = parts[1].toLongOrNull() ?: 0L
                        val expiryTime = timestamp + (CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)

                        if (now >= expiryTime) {
                            cachePrefs.edit().remove(key).apply()
                            removedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ArtistImageService", "Erreur lors du nettoyage du cache", e)
                }
            }
        }

        if (removedCount > 0) {
            Log.d("ArtistImageService", "$removedCount entrées expirées supprimées du cache")
        }
    }

    /**
     * Retourne la taille du cache (nombre d'entrées)
     */
    fun getCacheSize(): Int {
        return cachePrefs.all.size
    }

    /**
     * Vérifie si une image est en cache
     */
    fun isImageCached(artistOrAlbumName: String, isArtist: Boolean = true): Boolean {
        val cleanName = if (isArtist) cleanArtistName(artistOrAlbumName) else artistOrAlbumName.trim()
        val prefix = if (isArtist) CACHE_PREFIX_ARTIST else CACHE_PREFIX_ALBUM
        val cacheKey = prefix + cleanName.lowercase()

        return getCachedImageUrl(cacheKey) != null
    }

    // ===== FONCTIONS UTILITAIRES =====

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
}