package fr.kevw.kenemimusic

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class LyricsService(private val context: Context) {

    private val cachePrefs: SharedPreferences =
        context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE)

    companion object {
        private const val CACHE_PREFIX = "lyrics_"
        private const val MANUAL_PREFIX = "manual_lyrics_"
        private const val CACHE_EXPIRY_DAYS = 90
    }

    // ===== RECHERCHE PRINCIPALE =====
    suspend fun getLyrics(title: String, artist: String): LyricsResult = withContext(Dispatchers.IO) {
        try {
            if (title.isBlank() || artist.isBlank()) {
                return@withContext LyricsResult.Error("Titre ou artiste manquant")
            }

            val cleanTitle = cleanSongTitle(title)
            val cleanArtist = cleanArtistName(artist)
            val cacheKey = CACHE_PREFIX + "${cleanArtist}_${cleanTitle}".lowercase()
            val manualKey = MANUAL_PREFIX + "${cleanArtist}_${cleanTitle}".lowercase()

            // 1. Vérifier si paroles manuelles (PRIORITÉ)
            getManualLyrics(manualKey)?.let {
                Log.d("LyricsService", "✓ Manual lyrics found")
                return@withContext LyricsResult.Success(it, source = "Manuel")
            }

            // 2. Cache auto
            getCachedLyrics(cacheKey)?.let { (lyrics, source) ->
                Log.d("LyricsService", "✓ Cached lyrics from $source")
                return@withContext LyricsResult.Success(lyrics, source)
            }

            Log.d("LyricsService", "Searching: $cleanTitle - $cleanArtist")

            // 3. Chercher dans toutes les sources GRATUITES
            tryAllSources(cleanArtist, cleanTitle)?.let { (lyrics, source) ->
                cacheLyrics(cacheKey, lyrics, source)
                return@withContext LyricsResult.Success(lyrics, source)
            }

            // 4. Essayer les variantes (5 variations différentes)
            val variants = getTitleVariants(title, artist)
            Log.d("LyricsService", "Trying ${variants.size} variants...")

            for ((index, pair) in variants.withIndex()) {
                val (variantTitle, variantArtist) = pair
                Log.d("LyricsService", "Variant ${index + 1}: $variantTitle - $variantArtist")

                tryAllSources(variantArtist, variantTitle)?.let { (lyrics, source) ->
                    val variantKey = CACHE_PREFIX + "${variantArtist}_${variantTitle}".lowercase()
                    cacheLyrics(variantKey, lyrics, source)
                    Log.d("LyricsService", "✓ Found with variant ${index + 1}")
                    return@withContext LyricsResult.Success(lyrics, source)
                }
            }

            Log.w("LyricsService", "✗ Not found after checking all sources and variants")
            LyricsResult.NotFound

        } catch (e: Exception) {
            Log.e("LyricsService", "Error", e)
            LyricsResult.Error(e.message ?: "Erreur inconnue")
        }
    }

    // ===== RECHERCHE MANUELLE =====
    suspend fun searchLyricsManual(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<SearchResult>()

            // Chercher dans Lyrics.ovh (API de suggestions gratuite)
            searchInLyricsOvh(query)?.let { results.addAll(it) }

            results.distinctBy { "${it.artist.lowercase()}-${it.title.lowercase()}" }
        } catch (e: Exception) {
            Log.e("LyricsService", "Manual search error", e)
            emptyList()
        }
    }

    // Sauvegarder des paroles manuellement
    fun saveManualLyrics(title: String, artist: String, lyrics: String) {
        val cleanTitle = cleanSongTitle(title)
        val cleanArtist = cleanArtistName(artist)
        val key = MANUAL_PREFIX + "${cleanArtist}_${cleanTitle}".lowercase()

        cachePrefs.edit()
            .putString(key, lyrics)
            .apply()

        Log.d("LyricsService", "✓ Manual lyrics saved for: $cleanTitle - $cleanArtist")
    }

    // ===== 4 SOURCES 100% GRATUITES (sans clé API) =====
    private suspend fun tryAllSources(artist: String, title: String): Pair<String, String>? {
        // SOURCE 1: Lyrics.ovh (rapide et fiable, basé sur plusieurs APIs)
        tryLyricsOvh(artist, title)?.let {
            Log.d("LyricsService", "✓ Found on Lyrics.ovh")
            return it to "Lyrics.ovh"
        }

        // SOURCE 2: ChartLyrics (SOAP API publique, pas de limite)
        tryChartLyrics(artist, title)?.let {
            Log.d("LyricsService", "✓ Found on ChartLyrics")
            return it to "ChartLyrics"
        }

        // SOURCE 3: Lyrist (API Vercel gratuite)
        tryLyrist(artist, title)?.let {
            Log.d("LyricsService", "✓ Found on Lyrist")
            return it to "Lyrist"
        }

        // SOURCE 4: APISEEDS (API gratuite alternative)
        tryApiSeeds(artist, title)?.let {
            Log.d("LyricsService", "✓ Found on API Alternative")
            return it to "API Alternative"
        }

        return null
    }

    // ===== API 1: LYRICS.OVH (Gratuite, sans clé) =====
    private suspend fun tryLyricsOvh(artist: String, title: String): String? {
        return try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

            val response = URL(url).readText()
            val json = JSONObject(response)

            json.optString("lyrics")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.trim()
        } catch (e: Exception) {
            Log.d("LyricsService", "Lyrics.ovh miss: ${e.message}")
            null
        }
    }

    // ===== API 2: CHARTLYRICS (Gratuite SOAP API) =====
    private suspend fun tryChartLyrics(artist: String, title: String): String? {
        return try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "http://api.chartlyrics.com/apiv1.asmx/SearchLyricDirect?artist=$encodedArtist&song=$encodedTitle"

            val response = URL(url).readText()

            // Parser XML response
            val lyricsRegex = "<Lyric>(.*?)</Lyric>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = lyricsRegex.find(response)

            match?.groupValues?.get(1)
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")
                ?.replace("&amp;", "&")
                ?.replace("&quot;", "\"")
                ?.replace("&apos;", "'")
                ?.trim()
                ?.takeIf {
                    it.isNotBlank() &&
                            !it.contains("We do not have", ignoreCase = true) &&
                            !it.contains("Sorry, we don't have", ignoreCase = true)
                }
        } catch (e: Exception) {
            Log.d("LyricsService", "ChartLyrics miss: ${e.message}")
            null
        }
    }

    // ===== API 3: LYRIST (API Vercel gratuite) =====
    private suspend fun tryLyrist(artist: String, title: String): String? {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "https://lyrist.vercel.app/api/$encodedTitle/$encodedArtist"

            val response = URL(url).readText()
            val json = JSONObject(response)

            json.optString("lyrics")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.trim()
        } catch (e: Exception) {
            Log.d("LyricsService", "Lyrist miss: ${e.message}")
            null
        }
    }

    // ===== API 4: API ALTERNATIVE (format public) =====
    private suspend fun tryApiSeeds(artist: String, title: String): String? {
        return try {
            // Alternative: essayer avec un format différent
            val cleanArtist = artist.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            val cleanTitle = title.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            // Réessayer Lyrics.ovh avec un format nettoyé différemment
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

            val response = URL(url).readText()
            val json = JSONObject(response)

            json.optString("lyrics")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.trim()
        } catch (e: Exception) {
            Log.d("LyricsService", "Alternative miss: ${e.message}")
            null
        }
    }

    // ===== RECHERCHE MANUELLE - SUGGESTIONS =====
    private suspend fun searchInLyricsOvh(query: String): List<SearchResult>? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.lyrics.ovh/suggest/$encodedQuery"

            val response = URL(url).readText()
            val json = JSONObject(response)
            val data = json.optJSONArray("data") ?: return null

            (0 until data.length()).mapNotNull { i ->
                try {
                    val item = data.getJSONObject(i)
                    val artistObj = item.optJSONObject("artist")
                    val albumObj = item.optJSONObject("album")

                    SearchResult(
                        title = item.optString("title", ""),
                        artist = artistObj?.optString("name", "") ?: "",
                        album = albumObj?.optString("title", "") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }.take(10)
        } catch (e: Exception) {
            Log.e("LyricsService", "Search error: ${e.message}")
            null
        }
    }

    // ===== VARIANTES AMÉLIORÉES (5 variations) =====
    private fun getTitleVariants(title: String, artist: String): List<Pair<String, String>> {
        val variants = mutableSetOf<Pair<String, String>>()

        // VARIANTE 1: Sans parenthèses/crochets
        val noBrackets = title
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noBrackets.isNotBlank() && noBrackets != title) {
            variants += noBrackets to artist
        }

        // VARIANTE 2: Sans featuring/ft
        val noFeat = title
            .replace(Regex("(?i)\\s*[\\(\\[]?(?:feat\\.?|ft\\.?|featuring)\\s+.*?[\\)\\]]?"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noFeat.isNotBlank() && noFeat != title) {
            variants += noFeat to artist
        }

        // VARIANTE 3: Premier artiste seul (collaborations)
        val mainArtist = artist
            .split(Regex("[,&]|(?i)feat|(?i)ft\\.?"))
            .first()
            .trim()
        if (mainArtist.isNotBlank() && mainArtist != artist) {
            variants += title to mainArtist
            if (noBrackets.isNotBlank()) {
                variants += noBrackets to mainArtist
            }
        }

        // VARIANTE 4: Sans version/remix/edit
        val noVersion = title
            .replace(Regex("(?i)\\s*-?\\s*\\(?(?:version|remix|mix|edit|remaster|remastered|live|acoustic|radio).*?\\)?"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noVersion.isNotBlank() && noVersion != title) {
            variants += noVersion to artist
            if (mainArtist != artist) {
                variants += noVersion to mainArtist
            }
        }

        // VARIANTE 5: Première partie du titre (avant tiret ou parenthèse)
        val firstPart = title
            .split(Regex("[\\(\\[-]"))
            .first()
            .trim()
        if (firstPart.isNotBlank() && firstPart != title && firstPart.length >= 3) {
            variants += firstPart to artist
            if (mainArtist != artist) {
                variants += firstPart to mainArtist
            }
        }

        return variants.toList()
    }

    // ===== NETTOYAGE OPTIMISÉ =====
    private fun cleanSongTitle(title: String): String {
        return title
            // Supprimer les versions/remixes entre parenthèses
            .replace(Regex("(?i)\\s*\\(.*?(?:remix|mix|version|edit|remaster|live|acoustic).*?\\)"), "")
            // Supprimer les crochets
            .replace(Regex("\\s*\\[.*?\\]"), "")
            // Supprimer featuring
            .replace(Regex("(?i)\\s*[\\(\\[]?(?:feat\\.?|ft\\.?)\\s+.*"), "")
            // Normaliser les espaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            // Prendre le premier artiste
            .split(Regex("[,&]|(?i)feat|(?i)ft\\.?"))[0]
            // Garder seulement lettres, chiffres, espaces, apostrophes et tirets
            .replace(Regex("[^a-zA-Z0-9\\s'\\-àâäéèêëïîôùûüÿæœç]"), "")
            // Normaliser les espaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ===== CACHE =====
    private fun getCachedLyrics(key: String): Pair<String, String>? {
        val cached = cachePrefs.getString(key, null) ?: return null
        val parts = cached.split("|||")
        if (parts.size < 2) return null

        val lyrics = parts[0]
        val source = if (parts.size >= 3) parts[2] else "Cache"
        val timestamp = parts[1].toLongOrNull() ?: return null

        val expiry = timestamp + CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L

        return if (System.currentTimeMillis() < expiry) {
            lyrics to source
        } else {
            // Supprimer le cache expiré
            cachePrefs.edit().remove(key).apply()
            null
        }
    }

    private fun cacheLyrics(key: String, lyrics: String, source: String) {
        cachePrefs.edit()
            .putString(key, "$lyrics|||${System.currentTimeMillis()}|||$source")
            .apply()
    }

    private fun getManualLyrics(key: String): String? {
        return cachePrefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }
}

// ===== CLASSES DE RÉSULTATS =====
sealed class LyricsResult {
    data class Success(val lyrics: String, val source: String) : LyricsResult()
    object NotFound : LyricsResult()
    data class Error(val message: String) : LyricsResult()
}

data class SearchResult(
    val title: String,
    val artist: String,
    val album: String = ""
)