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
 * Service pour récupérer les paroles via Lyrics.ovh API
 * API gratuite, aucune clé nécessaire !
 */
class LyricsService(private val context: Context) {

    private val cachePrefs: SharedPreferences =
        context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE)

    companion object {
        private const val CACHE_PREFIX = "lyrics_"
        private const val CACHE_EXPIRY_DAYS = 90 // Les paroles changent rarement
    }

    /**
     * Récupère les paroles d'une chanson
     * @param title Titre de la chanson
     * @param artist Nom de l'artiste
     * @return Les paroles ou null si non trouvées
     */
    suspend fun getLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            if (title.isBlank() || artist.isBlank()) {
                return@withContext null
            }

            val cleanTitle = cleanSongTitle(title)
            val cleanArtist = cleanArtistName(artist)
            val cacheKey = CACHE_PREFIX + "${cleanArtist}_${cleanTitle}".lowercase()

            // 1. Vérifier le cache
            val cachedLyrics = getCachedLyrics(cacheKey)
            if (cachedLyrics != null) {
                Log.d("LyricsService", "Paroles trouvées en cache pour: $cleanTitle - $cleanArtist")
                return@withContext cachedLyrics
            }

            // 2. Télécharger depuis l'API
            Log.d("LyricsService", "Téléchargement des paroles pour: $cleanTitle - $cleanArtist")

            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val urlString = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

            val response = URL(urlString).readText()
            val json = JSONObject(response)

            if (json.has("lyrics")) {
                val lyrics = json.getString("lyrics").trim()

                if (lyrics.isNotBlank() && !lyrics.contains("error", ignoreCase = true)) {
                    // 3. Sauvegarder en cache
                    cacheLyrics(cacheKey, lyrics)
                    Log.d("LyricsService", "Paroles récupérées et mises en cache")
                    return@withContext lyrics
                }
            }

            // Si on arrive ici, les paroles n'ont pas été trouvées
            Log.w("LyricsService", "Paroles non trouvées pour: $cleanTitle - $cleanArtist")
            return@withContext null

        } catch (e: Exception) {
            Log.e("LyricsService", "Erreur lors de la récupération des paroles", e)
            return@withContext null
        }
    }

    /**
     * Nettoie le titre de la chanson pour améliorer les résultats
     */
    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex("\\(.*?\\)"), "") // Enlever (feat. xyz)
            .replace(Regex("\\[.*?\\]"), "") // Enlever [Remix]
            .replace(Regex("(?i)feat\\.?.*"), "") // Enlever feat.
            .replace(Regex("(?i)ft\\.?.*"), "") // Enlever ft.
            .replace(Regex("(?i)- .*remix.*", RegexOption.IGNORE_CASE), "") // Enlever remix
            .replace(Regex("(?i)- .*version.*", RegexOption.IGNORE_CASE), "") // Enlever version
            .replace(Regex("\\s+"), " ") // Normaliser espaces
            .trim()
    }

    /**
     * Nettoie le nom de l'artiste
     */
    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("(?i)feat\\.?.*"), "")
            .replace(Regex("(?i)ft\\.?.*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ===== GESTION DU CACHE =====

    private fun getCachedLyrics(key: String): String? {
        val cachedData = cachePrefs.getString(key, null) ?: return null

        try {
            val parts = cachedData.split("|||")
            if (parts.size != 2) return null

            val lyrics = parts[0]
            val timestamp = parts[1].toLongOrNull() ?: return null

            // Vérifier si le cache n'est pas expiré
            val now = System.currentTimeMillis()
            val expiryTime = timestamp + (CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)

            return if (now < expiryTime) {
                lyrics
            } else {
                // Cache expiré
                cachePrefs.edit().remove(key).apply()
                null
            }
        } catch (e: Exception) {
            Log.e("LyricsService", "Erreur lecture cache", e)
            return null
        }
    }

    private fun cacheLyrics(key: String, lyrics: String) {
        val timestamp = System.currentTimeMillis()
        val cachedData = "$lyrics|||$timestamp"
        cachePrefs.edit().putString(key, cachedData).apply()
    }

    /**
     * Efface tout le cache des paroles
     */
    fun clearCache() {
        cachePrefs.edit().clear().apply()
        Log.d("LyricsService", "Cache des paroles effacé")
    }

    /**
     * Retourne la taille du cache
     */
    fun getCacheSize(): Int {
        return cachePrefs.all.size
    }

    /**
     * Vérifie si des paroles sont en cache
     */
    fun areLyricsCached(title: String, artist: String): Boolean {
        val cleanTitle = cleanSongTitle(title)
        val cleanArtist = cleanArtistName(artist)
        val cacheKey = CACHE_PREFIX + "${cleanArtist}_${cleanTitle}".lowercase()
        return getCachedLyrics(cacheKey) != null
    }
}