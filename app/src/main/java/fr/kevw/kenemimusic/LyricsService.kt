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
        private const val TAG = "LyricsService"
        private const val CACHE_PREFIX = "lyrics_"
        private const val MANUAL_PREFIX = "manual_lyrics_"
        private const val CACHE_EXPIRY_DAYS = 90
    }

    // ===== RECHERCHE PRINCIPALE AMÉLIORÉE =====
    suspend fun getLyrics(title: String, artist: String): LyricsResult = withContext(Dispatchers.IO) {
        try {
            if (title.isBlank() || artist.isBlank()) {
                return@withContext LyricsResult.Error("Titre ou artiste manquant")
            }

            Log.d(TAG, "=== RECHERCHE POUR: '$title' par '$artist' ===")

            // 1. Vérifier paroles manuelles (PRIORITÉ ABSOLUE)
            val manualKey = MANUAL_PREFIX + "${artist}_${title}".lowercase()
            getManualLyrics(manualKey)?.let {
                Log.d(TAG, "✓ Paroles manuelles trouvées")
                return@withContext LyricsResult.Success(it, source = "Manuel")
            }

            // 2. Vérifier le cache
            val cacheKey = CACHE_PREFIX + "${artist}_${title}".lowercase()
            getCachedLyrics(cacheKey)?.let { (lyrics, source) ->
                Log.d(TAG, "✓ Cache trouvé ($source)")
                return@withContext LyricsResult.Success(lyrics, source)
            }

            // 3. STRATÉGIE DE RECHERCHE PROGRESSIVE
            // On génère toutes les variantes AVANT de commencer
            val searchVariants = generateSearchVariants(title, artist)

            Log.d(TAG, "Génération de ${searchVariants.size} variantes de recherche")
            searchVariants.forEachIndexed { index, (varTitle, varArtist) ->
                Log.d(TAG, "  Variante $index: '$varTitle' - '$varArtist'")
            }

            // 4. Tester chaque variante sur TOUTES les sources
            for ((index, variant) in searchVariants.withIndex()) {
                val (varTitle, varArtist) = variant
                Log.d(TAG, ">>> Test variante ${index + 1}/${searchVariants.size}: '$varTitle' - '$varArtist'")

                tryAllSources(varArtist, varTitle)?.let { (lyrics, source) ->
                    // Sauvegarder dans le cache avec la clé originale
                    cacheLyrics(cacheKey, lyrics, source)
                    Log.d(TAG, "✓✓✓ TROUVÉ avec variante ${index + 1} sur $source")
                    return@withContext LyricsResult.Success(lyrics, source)
                }
            }

            Log.w(TAG, "✗✗✗ AUCUNE PAROLE TROUVÉE après ${searchVariants.size} variantes")
            LyricsResult.NotFound

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la recherche", e)
            LyricsResult.Error(e.message ?: "Erreur inconnue")
        }
    }

    // ===== GÉNÉRATION DES VARIANTES (ORDRE OPTIMISÉ) =====
    private fun generateSearchVariants(title: String, artist: String): List<Pair<String, String>> {
        val variants = mutableListOf<Pair<String, String>>()

        // VARIANTE 0: Original exact (souvent ignoré par erreur !)
        variants.add(title to artist)

        // VARIANTE 1: Nettoyage léger (supprimer juste les espaces multiples)
        val lightCleanTitle = title.replace(Regex("\\s+"), " ").trim()
        val lightCleanArtist = artist.replace(Regex("\\s+"), " ").trim()
        if (lightCleanTitle != title || lightCleanArtist != artist) {
            variants.add(lightCleanTitle to lightCleanArtist)
        }

        // VARIANTE 2: Sans parenthèses/crochets
        val noBrackets = title
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noBrackets.isNotBlank() && noBrackets != title) {
            variants.add(noBrackets to artist)
        }

        // VARIANTE 3: Sans featuring
        val noFeat = title
            .replace(Regex("(?i)\\s*[\\(\\[]?(?:feat\\.?|ft\\.?|featuring)\\s+.*?[\\)\\]]?"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noFeat.isNotBlank() && noFeat != title) {
            variants.add(noFeat to artist)
        }

        // VARIANTE 4: Premier artiste seul
        val mainArtist = artist
            .split(Regex("[,&]|(?i)feat|(?i)ft\\.?"))
            .first()
            .trim()
        if (mainArtist.isNotBlank() && mainArtist != artist) {
            variants.add(title to mainArtist)
            if (noBrackets.isNotBlank() && noBrackets != title) {
                variants.add(noBrackets to mainArtist)
            }
        }

        // VARIANTE 5: Sans version/remix/edit
        val noVersion = title
            .replace(Regex("(?i)\\s*-?\\s*\\(?(?:version|remix|mix|edit|remaster|remastered|live|acoustic|radio).*?\\)?"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noVersion.isNotBlank() && noVersion != title) {
            variants.add(noVersion to artist)
            if (mainArtist != artist) {
                variants.add(noVersion to mainArtist)
            }
        }

        // VARIANTE 6: Première partie du titre (avant tiret ou parenthèse)
        val firstPart = title
            .split(Regex("[\\(\\[-]"))
            .first()
            .trim()
        if (firstPart.isNotBlank() && firstPart != title && firstPart.length >= 3) {
            variants.add(firstPart to artist)
            if (mainArtist != artist) {
                variants.add(firstPart to mainArtist)
            }
        }

        // VARIANTE 7: Nettoyage agressif (dernier recours)
        val aggressiveTitle = title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val aggressiveArtist = artist
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (aggressiveTitle.isNotBlank() && aggressiveArtist.isNotBlank()) {
            variants.add(aggressiveTitle to aggressiveArtist)
        }

        // Retourner sans doublons
        return variants.distinctBy { "${it.first.lowercase()}-${it.second.lowercase()}" }
    }

    // ===== RECHERCHE DANS TOUTES LES SOURCES =====
    private suspend fun tryAllSources(artist: String, title: String): Pair<String, String>? {
        // SOURCE 1: Lyrics.ovh
        tryLyricsOvh(artist, title)?.let {
            return it to "Lyrics.ovh"
        }

        // SOURCE 2: ChartLyrics
        tryChartLyrics(artist, title)?.let {
            return it to "ChartLyrics"
        }

        // SOURCE 3: Lyrist
        tryLyrist(artist, title)?.let {
            return it to "Lyrist"
        }

        return null
    }

    // ===== API 1: LYRICS.OVH =====
    private suspend fun tryLyricsOvh(artist: String, title: String): String? {
        return try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

            Log.d(TAG, "  → Lyrics.ovh: $url")

            val response = URL(url).readText()
            val json = JSONObject(response)

            json.optString("lyrics")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.trim()
                ?.also { Log.d(TAG, "  ✓ Lyrics.ovh: Trouvé (${it.length} caractères)") }
        } catch (e: Exception) {
            Log.d(TAG, "  ✗ Lyrics.ovh: ${e.message}")
            null
        }
    }

    // ===== API 2: CHARTLYRICS =====
    private suspend fun tryChartLyrics(artist: String, title: String): String? {
        return try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "http://api.chartlyrics.com/apiv1.asmx/SearchLyricDirect?artist=$encodedArtist&song=$encodedTitle"

            Log.d(TAG, "  → ChartLyrics: $url")

            val response = URL(url).readText()
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
                ?.also { Log.d(TAG, "  ✓ ChartLyrics: Trouvé (${it.length} caractères)") }
        } catch (e: Exception) {
            Log.d(TAG, "  ✗ ChartLyrics: ${e.message}")
            null
        }
    }

    // ===== API 3: LYRIST =====
    private suspend fun tryLyrist(artist: String, title: String): String? {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "https://lyrist.vercel.app/api/$encodedTitle/$encodedArtist"

            Log.d(TAG, "  → Lyrist: $url")

            val response = URL(url).readText()
            val json = JSONObject(response)

            json.optString("lyrics")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.trim()
                ?.also { Log.d(TAG, "  ✓ Lyrist: Trouvé (${it.length} caractères)") }
        } catch (e: Exception) {
            Log.d(TAG, "  ✗ Lyrist: ${e.message}")
            null
        }
    }

    // ===== RECHERCHE MANUELLE =====
    suspend fun searchLyricsManual(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<SearchResult>()
            searchInLyricsOvh(query)?.let { results.addAll(it) }
            results.distinctBy { "${it.artist.lowercase()}-${it.title.lowercase()}" }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur recherche manuelle", e)
            emptyList()
        }
    }

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
            Log.e(TAG, "Erreur recherche suggestions", e)
            null
        }
    }

    // ===== SAUVEGARDE MANUELLE =====
    fun saveManualLyrics(title: String, artist: String, lyrics: String) {
        val key = MANUAL_PREFIX + "${artist}_${title}".lowercase()
        cachePrefs.edit().putString(key, lyrics).apply()
        Log.d(TAG, "✓ Paroles manuelles sauvegardées: $title - $artist")
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

// Classes de résultats (inchangées)
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