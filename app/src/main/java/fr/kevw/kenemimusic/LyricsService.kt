package fr.kevw.kenemimusic

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class LyricsService(private val context: Context) {

    private val cachePrefs: SharedPreferences =
        context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE)

    companion object {
        private const val CACHE_PREFIX = "lyrics_"
        private const val CACHE_EXPIRY_DAYS = 90
    }

    suspend fun getLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            if (title.isBlank() || artist.isBlank()) return@withContext null

            val cleanTitle = cleanSongTitle(title)
            val cleanArtist = cleanArtistName(artist)
            val cacheKey = CACHE_PREFIX + "${cleanArtist}_${cleanTitle}".lowercase()

            // 1. Cache
            getCachedLyrics(cacheKey)?.let {
                Log.d("LyricsService", "Lyrics from cache: $cleanTitle - $cleanArtist")
                return@withContext it
            }

            Log.d("LyricsService", "Searching lyrics: $cleanTitle - $cleanArtist")

            // 2. Main attempt
            tryLyricsOvh(cleanArtist, cleanTitle)?.let {
                cacheLyrics(cacheKey, it)
                return@withContext it
            }

            // 3. Variants
            val variants = getTitleVariants(title, artist)
            for ((variantTitle, variantArtist) in variants) {
                tryLyricsOvh(variantArtist, variantTitle)?.let {
                    val variantKey =
                        CACHE_PREFIX + "${variantArtist}_${variantTitle}".lowercase()
                    cacheLyrics(variantKey, it)
                    return@withContext it
                }
            }

            Log.w("LyricsService", "Lyrics not found: $cleanTitle - $cleanArtist")
            null

        } catch (e: Exception) {
            Log.e("LyricsService", "Lyrics error", e)
            null
        }
    }

    private suspend fun tryLyricsOvh(artist: String, title: String): String? {
        return try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

            val response = URL(url).readText()
            val json = JSONObject(response)

            json.optString("lyrics")
                .takeIf { it.isNotBlank() }
                ?.trim()
        } catch (e: Exception) {
            Log.d("LyricsService", "Lyrics.ovh miss: $artist - $title")
            null
        }
    }

    // ===== Variants =====

    private fun getTitleVariants(title: String, artist: String): List<Pair<String, String>> {
        val variants = mutableListOf<Pair<String, String>>()

        val noBrackets = title
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .trim()
        if (noBrackets != title) variants += noBrackets to artist

        val noFeat = title
            .replace(Regex("(?i)feat\\.?.*|ft\\.?.*"), "")
            .trim()
        if (noFeat != title) variants += noFeat to artist

        val noVersion = title
            .replace(Regex("(?i)- .*?(version|remix).*"), "")
            .trim()
        if (noVersion != title) variants += noVersion to artist

        if (artist.contains(",") || artist.contains("&")) {
            val firstArtist = artist.split(",", "&").first().trim()
            variants += title to firstArtist
            variants += noBrackets to firstArtist
        }

        return variants.distinct()
    }

    // ===== Cleaning =====

    private fun cleanSongTitle(title: String): String =
        title.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("(?i)feat\\.?.*|ft\\.?.*"), "")
            .replace(Regex("(?i)- .*?(version|remix).*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun cleanArtistName(artist: String): String =
        artist.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("(?i)feat\\.?.*|ft\\.?.*"), "")
            .replace(Regex("\\s+"), " ")
            .split(",", "&")[0]
            .trim()

    // ===== Cache =====

    private fun getCachedLyrics(key: String): String? {
        val cached = cachePrefs.getString(key, null) ?: return null
        val parts = cached.split("|||")
        if (parts.size != 2) return null

        val lyrics = parts[0]
        val timestamp = parts[1].toLongOrNull() ?: return null

        val expiry =
            timestamp + CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L

        return if (System.currentTimeMillis() < expiry) lyrics
        else {
            cachePrefs.edit().remove(key).apply()
            null
        }
    }

    private fun cacheLyrics(key: String, lyrics: String) {
        cachePrefs.edit()
            .putString(key, "$lyrics|||${System.currentTimeMillis()}")
            .apply()
    }
}
