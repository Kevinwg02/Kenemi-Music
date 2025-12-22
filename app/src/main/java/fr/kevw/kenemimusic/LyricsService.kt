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

            // 2. Tentative principale avec toutes les sources
            tryAllSources(cleanArtist, cleanTitle)?.let {
                cacheLyrics(cacheKey, it)
                return@withContext it
            }

            // 3. Variants (essayer avec différentes variantes du titre)
            val variants = getTitleVariants(title, artist)
            for ((variantTitle, variantArtist) in variants) {
                tryAllSources(variantArtist, variantTitle)?.let {
                    val variantKey = CACHE_PREFIX + "${variantArtist}_${variantTitle}".lowercase()
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

    // ===== SOURCES MULTIPLES =====

    private suspend fun tryAllSources(artist: String, title: String): String? {
        // Ordre de priorité des sources (du plus fiable au moins fiable)

        // 1. Lyrics.ovh (rapide et fiable)
        tryLyricsOvh(artist, title)?.let {
            Log.d("LyricsService", "Found on Lyrics.ovh")
            return it
        }

        // 2. ChartLyrics (SOAP API, parfois plus complet)
        tryChartLyrics(artist, title)?.let {
            Log.d("LyricsService", "Found on ChartLyrics")
            return it
        }

        // 3. Lyrics.ovh avec format alternatif
        tryLyricsOvhAlt(artist, title)?.let {
            Log.d("LyricsService", "Found on Lyrics.ovh (alt)")
            return it
        }

        // 4. Lyrist (API alternative)
        tryLyrist(artist, title)?.let {
            Log.d("LyricsService", "Found on Lyrist")
            return it
        }

        return null
    }

    // ===== SOURCE 1: LYRICS.OVH =====

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

    private suspend fun tryLyricsOvhAlt(artist: String, title: String): String? {
        return try {
            // Essayer avec moins de nettoyage
            val simpleArtist = artist.lowercase().replace(Regex("[^a-z0-9\\s]"), "").trim()
            val simpleTitle = title.lowercase().replace(Regex("[^a-z0-9\\s]"), "").trim()

            val encodedArtist = URLEncoder.encode(simpleArtist, "UTF-8")
            val encodedTitle = URLEncoder.encode(simpleTitle, "UTF-8")
            val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

            val response = URL(url).readText()
            val json = JSONObject(response)

            json.optString("lyrics")
                .takeIf { it.isNotBlank() }
                ?.trim()
        } catch (e: Exception) {
            null
        }
    }

    // ===== SOURCE 2: CHARTLYRICS (SOAP API) =====

    private suspend fun tryChartLyrics(artist: String, title: String): String? {
        return try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")

            // ChartLyrics utilise une API SOAP
            val url = "http://api.chartlyrics.com/apiv1.asmx/SearchLyricDirect?artist=$encodedArtist&song=$encodedTitle"

            val response = URL(url).readText()

            // Parser la réponse XML
            val lyricsRegex = "<Lyric>(.*?)</Lyric>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = lyricsRegex.find(response)

            match?.groupValues?.get(1)
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")
                ?.replace("&amp;", "&")
                ?.replace("&quot;", "\"")
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.contains("We do not have the lyrics", ignoreCase = true) }
        } catch (e: Exception) {
            Log.d("LyricsService", "ChartLyrics miss: $artist - $title")
            null
        }
    }

    // ===== SOURCE 3: LYRIST =====

    private suspend fun tryLyrist(artist: String, title: String): String? {
        return try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")

            // API Lyrist (gratuite, sans clé)
            val url = "https://lyrist.vercel.app/api/$encodedTitle/$encodedArtist"

            val response = URL(url).readText()
            val json = JSONObject(response)

            json.optString("lyrics")
                .takeIf { it.isNotBlank() }
                ?.trim()
        } catch (e: Exception) {
            Log.d("LyricsService", "Lyrist miss: $artist - $title")
            null
        }
    }

    // ===== VARIANTS =====

    private fun getTitleVariants(title: String, artist: String): List<Pair<String, String>> {
        val variants = mutableListOf<Pair<String, String>>()

        // Variante 1: Sans parenthèses/crochets
        val noBrackets = title
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noBrackets != title && noBrackets.isNotBlank()) {
            variants += noBrackets to artist
        }

        // Variante 2: Sans featuring
        val noFeat = title
            .replace(Regex("(?i)\\s*[\\(\\[]?feat\\.?\\s+.*?[\\)\\]]?"), "")
            .replace(Regex("(?i)\\s*[\\(\\[]?ft\\.?\\s+.*?[\\)\\]]?"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noFeat != title && noFeat.isNotBlank()) {
            variants += noFeat to artist
        }

        // Variante 3: Premier artiste seulement (pour collaborations)
        if (artist.contains(",") || artist.contains("&") || artist.contains("feat", ignoreCase = true)) {
            val firstArtist = artist
                .split(",", "&")
                .first()
                .replace(Regex("(?i)feat.*"), "")
                .trim()
            if (firstArtist.isNotBlank() && firstArtist != artist) {
                variants += title to firstArtist
                if (noBrackets.isNotBlank()) {
                    variants += noBrackets to firstArtist
                }
            }
        }

        // Variante 4: Sans version/remix
        val noVersion = title
            .replace(Regex("(?i)\\s*-\\s*(.*?(version|remix|mix|edit|remaster)).*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noVersion != title && noVersion.isNotBlank()) {
            variants += noVersion to artist
        }

        // Variante 5: Titre minimal (première partie avant parenthèses)
        if (title.contains("(") || title.contains("[")) {
            val minimal = title.split("(", "[").first().trim()
            if (minimal != title && minimal.isNotBlank()) {
                variants += minimal to artist
            }
        }

        return variants.distinct()
    }

    // ===== CLEANING =====

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex("\\s*\\(.*?\\)"), "")
            .replace(Regex("\\s*\\[.*?\\]"), "")
            .replace(Regex("(?i)\\s*feat\\.?\\s+.*"), "")
            .replace(Regex("(?i)\\s*ft\\.?\\s+.*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex("\\s*\\(.*?\\)"), "")
            .replace(Regex("\\s*\\[.*?\\]"), "")
            .split(",", "&", "feat", "ft.")[0]
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ===== CACHE =====

    private fun getCachedLyrics(key: String): String? {
        val cached = cachePrefs.getString(key, null) ?: return null
        val parts = cached.split("|||")
        if (parts.size != 2) return null

        val lyrics = parts[0]
        val timestamp = parts[1].toLongOrNull() ?: return null

        val expiry = timestamp + CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L

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