package fr.kevw.kenemimusic

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Un artiste proposé par l'une des sources (Deezer, TheAudioDB, Wikipédia). */
data class ArtistCandidate(
    val id: String,
    val name: String,
    val fans: Int,          // 0 si la source ne le fournit pas
    val source: String,     // "Deezer", "TheAudioDB", "Wikipédia"
    val thumbUrl: String,
    val fullUrl: String
)

data class ArtistSearchResult(
    val candidates: List<ArtistCandidate>,
    val errors: List<String>
)

/**
 * Clé de cache mémoire pour Coil : elle change quand le fichier local est remplacé
 * (même chemin, nouveau contenu), sinon l'ancienne image resterait affichée.
 */
fun imageCacheKey(url: String?, version: Int = 0): String {
    val stamp = url?.takeIf { it.startsWith("file://") }
        ?.let { java.io.File(it.removePrefix("file://")).lastModified() } ?: 0L
    return "$url#$stamp#$version"
}

/** Une pochette d'album proposée par l'une des sources (Deezer, iTunes). */
data class AlbumCandidate(
    val id: String,
    val title: String,
    val artist: String,
    val source: String,
    val thumbUrl: String,
    val fullUrl: String
)

data class AlbumSearchResult(
    val candidates: List<AlbumCandidate>,
    val errors: List<String>
)

class ArtistImageService(private val context: Context) {

    private val cacheDir: File = File(context.filesDir, "image_cache").apply { mkdirs() }

    // Limite le nombre de requêtes simultanées (Deezer : quota 50 requêtes / 5 s)
    private val netGate = Semaphore(4)

    // Artistes sans résultat (sans erreur réseau) : évite de re-solliciter les API en boucle
    private val notFound = ConcurrentHashMap<String, Long>()

    companion object {
        private const val TAG = "ArtistImageService"
        private const val CACHE_PREFIX_ARTIST = "artist_"
        private const val CACHE_PREFIX_ALBUM = "album_"
        private const val USER_AGENT = "KenemiMusic/1.0 (Android music player)"
        private const val AUDIODB_KEY = "2" // clé de test gratuite de TheAudioDB
        private const val NOT_FOUND_TTL_MS = 10 * 60 * 1000L
    }

    private class SourceResult(
        val candidates: List<ArtistCandidate>,
        val message: String? = null,
        val failed: Boolean = false
    )

    // ===================== ARTISTES =====================

    /** Image en cache, sinon détection automatique du meilleur candidat. */
    suspend fun getArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        try {
            if (artistName.isBlank() || artistName == "Artiste inconnu") return@withContext null

            val clean = cleanArtistName(artistName)
            if (clean.isBlank()) return@withContext null
            val cacheKey = artistCacheKey(clean)

            getCachedImageFile(cacheKey)?.let { return@withContext "file://${it.absolutePath}" }

            val lastMiss = notFound[cacheKey]
            if (lastMiss != null && System.currentTimeMillis() - lastMiss < NOT_FOUND_TTL_MS) {
                return@withContext null
            }

            val auto = autoFind(clean)
            val best = auto.candidate
            if (best == null) {
                Log.w(TAG, "Aucune image trouvée pour '$clean'")
                if (!auto.hadError) notFound[cacheKey] = System.currentTimeMillis()
                return@withContext null
            }

            downloadAndCacheImage(cacheKey, best.fullUrl, overwrite = false)
                ?.let { "file://${it.absolutePath}" }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur image artiste $artistName", e)
            null
        }
    }

    /** Recherche manuelle / liste de choix : toutes les sources, triées par pertinence. */
    suspend fun searchCandidates(query: String, limit: Int = 8): ArtistSearchResult =
        withContext(Dispatchers.IO) {
            val clean = cleanArtistName(query)
            if (clean.isBlank()) return@withContext ArtistSearchResult(emptyList(), emptyList())

            val (deezer, audio, wiki) = coroutineScope {
                val d = async { fetchDeezer(clean, limit) }
                val a = async { fetchAudioDb(clean) }
                val w = async { fetchWikipedia(clean, 5) }
                Triple(d.await(), a.await(), w.await())
            }

            val all = ranked(clean, (deezer.candidates + audio.candidates + wiki.candidates))
                .distinctBy { it.fullUrl }

            ArtistSearchResult(all, listOfNotNull(deezer.message, audio.message, wiki.message))
        }

    /** Applique le choix de l'utilisateur. L'ancienne image n'est remplacée qu'en cas de succès. */
    suspend fun selectArtistImage(artistName: String, candidate: ArtistCandidate): String? =
        withContext(Dispatchers.IO) {
            val key = artistCacheKey(cleanArtistName(artistName))
            notFound.remove(key)
            downloadAndCacheImage(key, candidate.fullUrl)?.let { "file://${it.absolutePath}" }
        }

    /** Relance la détection automatique. Ne touche à rien si rien n'est trouvé. */
    suspend fun redetectArtistImage(artistName: String): String? = withContext(Dispatchers.IO) {
        val clean = cleanArtistName(artistName)
        val best = autoFind(clean).candidate ?: return@withContext null
        selectArtistImage(artistName, best)
    }

    // ===================== SOURCES =====================

    private class AutoResult(val candidate: ArtistCandidate?, val hadError: Boolean)

    private suspend fun autoFind(clean: String): AutoResult {
        var hadError = false

        val d = fetchDeezer(clean, 5)
        hadError = hadError || d.failed
        ranked(clean, d.candidates).firstOrNull { isArtistMatch(clean, it.name) }
            ?.let { return AutoResult(it, false) }

        val a = fetchAudioDb(clean)
        hadError = hadError || a.failed
        ranked(clean, a.candidates).firstOrNull { similarity(clean, baseName(it.name)) >= 0.85 }
            ?.let { return AutoResult(it, false) }

        val w = fetchWikipedia(clean, 3)
        hadError = hadError || w.failed
        ranked(clean, w.candidates).firstOrNull { similarity(clean, baseName(it.name)) >= 0.95 }
            ?.let { return AutoResult(it, false) }

        return AutoResult(null, hadError)
    }

    private suspend fun fetchDeezer(name: String, limit: Int): SourceResult {
        return try {
            val q = URLEncoder.encode(name, "UTF-8")
            val body = netGate.withPermit { httpGet("https://api.deezer.com/search/artist?q=$q&limit=$limit") }
            val json = JSONObject(body)

            json.optJSONObject("error")?.let {
                val msg = "Deezer : ${it.optString("message", "erreur")}"
                Log.w(TAG, msg)
                return SourceResult(emptyList(), msg, failed = true)
            }

            val data = json.optJSONArray("data")
            val list = mutableListOf<ArtistCandidate>()
            if (data != null) {
                for (i in 0 until data.length()) {
                    val o = data.getJSONObject(i)
                    val full = pickPicture(o, "picture_xl", "picture_big", "picture_medium") ?: continue
                    val thumb = pickPicture(o, "picture_medium", "picture_big", "picture_xl") ?: full
                    list.add(
                        ArtistCandidate(
                            id = "deezer:${o.optLong("id")}",
                            name = o.optString("name"),
                            fans = o.optInt("nb_fan", 0),
                            source = "Deezer",
                            thumbUrl = thumb,
                            fullUrl = full
                        )
                    )
                }
            }
            val total = data?.length() ?: 0
            Log.d(TAG, "Deezer '$name' -> $total résultat(s), ${list.size} avec photo")
            val msg = if (total > 0 && list.isEmpty())
                "Deezer : $total artiste(s) trouvé(s) mais sans photo" else null
            SourceResult(list, msg)
        } catch (e: Exception) {
            Log.w(TAG, "Deezer en échec pour '$name'", e)
            SourceResult(emptyList(), "Deezer : ${e.message ?: e.javaClass.simpleName}", failed = true)
        }
    }

    private suspend fun fetchAudioDb(name: String): SourceResult {
        return try {
            val q = URLEncoder.encode(name, "UTF-8")
            val body = netGate.withPermit {
                httpGet("https://www.theaudiodb.com/api/v1/json/$AUDIODB_KEY/search.php?s=$q")
            }
            val arr = JSONObject(body).optJSONArray("artists")
            val list = mutableListOf<ArtistCandidate>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val img = o.str("strArtistThumb") ?: o.str("strArtistFanart") ?: continue
                    list.add(
                        ArtistCandidate(
                            id = "audiodb:${o.optString("idArtist")}",
                            name = o.optString("strArtist"),
                            fans = 0,
                            source = "TheAudioDB",
                            thumbUrl = img,
                            fullUrl = img
                        )
                    )
                }
            }
            Log.d(TAG, "TheAudioDB '$name' -> ${list.size} résultat(s)")
            SourceResult(list)
        } catch (e: Exception) {
            Log.w(TAG, "TheAudioDB en échec pour '$name'", e)
            SourceResult(emptyList(), "TheAudioDB : ${e.message ?: e.javaClass.simpleName}", failed = true)
        }
    }

    private suspend fun fetchWikipedia(name: String, limit: Int): SourceResult {
        return try {
            val search = URLEncoder.encode("\"$name\" musician OR singer OR band OR rapper", "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&format=json" +
                    "&generator=search&gsrsearch=$search&gsrlimit=$limit" +
                    "&prop=pageimages&piprop=thumbnail&pithumbsize=600"
            val body = netGate.withPermit { httpGet(url) }
            val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages")
                ?: return SourceResult(emptyList())

            val items = mutableListOf<Pair<Int, ArtistCandidate>>()
            val keys = pages.keys()
            while (keys.hasNext()) {
                val p = pages.getJSONObject(keys.next())
                val thumb = p.optJSONObject("thumbnail")?.str("source") ?: continue
                items.add(
                    p.optInt("index", 99) to ArtistCandidate(
                        id = "wiki:${p.optLong("pageid")}",
                        name = p.optString("title"),
                        fans = 0,
                        source = "Wikipédia",
                        thumbUrl = thumb,
                        fullUrl = thumb
                    )
                )
            }
            val list = items.sortedBy { it.first }.map { it.second }
            Log.d(TAG, "Wikipédia '$name' -> ${list.size} résultat(s)")
            SourceResult(list)
        } catch (e: Exception) {
            Log.w(TAG, "Wikipédia en échec pour '$name'", e)
            SourceResult(emptyList(), "Wikipédia : ${e.message ?: e.javaClass.simpleName}", failed = true)
        }
    }

    private fun ranked(clean: String, list: List<ArtistCandidate>): List<ArtistCandidate> =
        list.sortedWith(
            compareByDescending<ArtistCandidate> { similarity(clean, baseName(it.name)) }
                .thenByDescending { it.fans }
        )

    // ===================== ALBUMS =====================

    private fun albumCacheKey(albumName: String, artistName: String) =
        safeKey(CACHE_PREFIX_ALBUM + "${albumName.trim()}_${cleanArtistName(artistName)}".lowercase())

    /** Pochette en cache, sinon détection automatique ; en dernier recours, photo de l'artiste. */
    suspend fun getAlbumCoverUrl(albumName: String, artistName: String = ""): String? =
        withContext(Dispatchers.IO) {
            try {
                if (albumName.isBlank() || albumName == "Album inconnu") return@withContext null

                val cleanAlbum = albumName.trim()
                val cleanArtist = cleanArtistName(artistName)
                val cacheKey = albumCacheKey(cleanAlbum, cleanArtist)

                getCachedImageFile(cacheKey)?.let { return@withContext "file://${it.absolutePath}" }

                val lastMiss = notFound[cacheKey]
                val recentMiss = lastMiss != null &&
                        System.currentTimeMillis() - lastMiss < NOT_FOUND_TTL_MS
                if (!recentMiss) {
                    val auto = autoFindAlbum(cleanAlbum, cleanArtist)
                    val best = auto.candidate
                    if (best != null) {
                        downloadAndCacheImage(cacheKey, best.fullUrl, overwrite = false)?.let {
                            return@withContext "file://${it.absolutePath}"
                        }
                    } else {
                        Log.w(TAG, "Aucune pochette trouvée pour '$cleanAlbum' ($cleanArtist)")
                        if (!auto.hadError) notFound[cacheKey] = System.currentTimeMillis()
                    }
                }

                if (cleanArtist.isNotBlank()) return@withContext getArtistImageUrl(cleanArtist)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Erreur pochette $albumName", e)
                null
            }
        }

    /**
     * Liste de choix des pochettes (Deezer + iTunes).
     * Si album/artist sont fournis, le classement se base dessus ; sinon sur le texte tapé.
     */
    suspend fun searchAlbumCandidates(
        query: String,
        limit: Int = 8,
        album: String? = null,
        artist: String? = null
    ): AlbumSearchResult = withContext(Dispatchers.IO) {
        val q = query.replace('"', ' ').trim()
        if (q.isBlank()) return@withContext AlbumSearchResult(emptyList(), emptyList())
        val cleanArtist = artist?.let { cleanArtistName(it) }.orEmpty()

        val results = coroutineScope {
            val adv = if (!album.isNullOrBlank() && cleanArtist.isNotBlank())
                async {
                    fetchDeezerAlbumsRaw(
                        "artist:\"$cleanArtist\" album:\"${album.replace('"', ' ')}\"", limit
                    )
                }
            else null
            val plain = async { fetchDeezerAlbumsRaw(q, limit) }
            val itunes = async { fetchItunesAlbums(q, limit) }
            listOfNotNull(adv?.await(), plain.await(), itunes.await())
        }

        val all = results.flatMap { it.candidates }.distinctBy { it.fullUrl }
        val sorted = if (!album.isNullOrBlank())
            all.sortedByDescending { albumScore(album, cleanArtist, it) }
        else
            all.sortedByDescending { relevance(q, it) }

        AlbumSearchResult(sorted, results.mapNotNull { it.message })
    }

    /** Applique le choix de l'utilisateur. L'ancienne pochette n'est remplacée qu'en cas de succès. */
    suspend fun selectAlbumCover(
        albumName: String, artistName: String, candidate: AlbumCandidate
    ): String? = withContext(Dispatchers.IO) {
        val key = albumCacheKey(albumName, artistName)
        notFound.remove(key)
        downloadAndCacheImage(key, candidate.fullUrl)?.let { "file://${it.absolutePath}" }
    }

    /** Relance la détection automatique. Ne touche à rien si rien n'est trouvé. */
    suspend fun redetectAlbumCover(albumName: String, artistName: String): String? =
        withContext(Dispatchers.IO) {
            val best = autoFindAlbum(albumName.trim(), cleanArtistName(artistName)).candidate
                ?: return@withContext null
            selectAlbumCover(albumName, artistName, best)
        }

    /** Utilise la photo de l'artiste comme pochette de l'album. */
    suspend fun useArtistImageForAlbum(albumName: String, artistName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val artistUrl = getArtistImageUrl(ArtistUtils.primaryArtist(artistName))
                    ?: return@withContext null
                val source = File(artistUrl.removePrefix("file://"))
                if (!source.exists()) return@withContext null

                val key = albumCacheKey(albumName, artistName)
                val extension = source.extension.ifBlank { "jpg" }
                val partFile = File(cacheDir, "$key.$extension.part")
                source.copyTo(partFile, overwrite = true)

                deleteCachedFiles(key)
                val cacheFile = File(cacheDir, "$key.$extension")
                if (!partFile.renameTo(cacheFile)) {
                    partFile.delete()
                    return@withContext null
                }
                notFound.remove(key)
                "file://${cacheFile.absolutePath}"
            } catch (e: Exception) {
                Log.e(TAG, "Erreur : image artiste utilisée pour l'album", e)
                null
            }
        }

    private class AlbumSourceResult(
        val candidates: List<AlbumCandidate>,
        val message: String? = null,
        val failed: Boolean = false
    )

    private class AlbumAuto(val candidate: AlbumCandidate?, val hadError: Boolean)

    private suspend fun autoFindAlbum(album: String, artist: String): AlbumAuto {
        var hadError = false

        val deezer = if (artist.isNotBlank()) {
            val adv = fetchDeezerAlbumsRaw(
                "artist:\"${artist.replace('"', ' ')}\" album:\"${album.replace('"', ' ')}\"", 5
            )
            if (adv.candidates.isNotEmpty() || adv.failed) adv
            else fetchDeezerAlbumsRaw("$album $artist".replace('"', ' '), 5)
        } else {
            fetchDeezerAlbumsRaw(album.replace('"', ' '), 5)
        }
        hadError = hadError || deezer.failed
        pickBestAlbum(album, artist, deezer.candidates)?.let { return AlbumAuto(it, false) }

        val itunes = fetchItunesAlbums("$album $artist".trim().replace('"', ' '), 5)
        hadError = hadError || itunes.failed
        pickBestAlbum(album, artist, itunes.candidates)?.let { return AlbumAuto(it, false) }

        return AlbumAuto(null, hadError)
    }

    private fun pickBestAlbum(album: String, artist: String, list: List<AlbumCandidate>): AlbumCandidate? =
        list.filter { similarity(baseAlbum(album), baseAlbum(it.title)) >= 0.7 }
            .maxByOrNull { albumScore(album, artist, it) }
            ?.takeIf { albumScore(album, artist, it) >= 0.75 }

    private fun albumScore(album: String, artist: String, c: AlbumCandidate): Double {
        val t = similarity(baseAlbum(album), baseAlbum(c.title))
        if (artist.isBlank() || c.artist.isBlank()) return t
        val a = if (isArtistMatch(artist, c.artist)) 1.0 else similarity(artist, c.artist)
        return 0.65 * t + 0.35 * a
    }

    private fun relevance(q: String, c: AlbumCandidate): Double = maxOf(
        similarity(q, c.title),
        similarity(q, "${c.title} ${c.artist}"),
        similarity(q, "${c.artist} ${c.title}")
    )

    /** "Discovery (Deluxe Edition)" -> "Discovery" */
    private fun baseAlbum(title: String): String =
        title.replace(Regex("\\s*[\\(\\[].*?[\\)\\]]"), "")
            .replace(Regex("(?i)\\s+-\\s+(?:deluxe|remaster|special|expanded|bonus|single|ep)\\b.*$"), "")
            .trim()
            .ifBlank { title.trim() }

    private suspend fun fetchDeezerAlbumsRaw(q: String, limit: Int): AlbumSourceResult {
        return try {
            val enc = URLEncoder.encode(q, "UTF-8")
            val body = netGate.withPermit { httpGet("https://api.deezer.com/search/album?q=$enc&limit=$limit") }
            val json = JSONObject(body)

            json.optJSONObject("error")?.let {
                val msg = "Deezer : ${it.optString("message", "erreur")}"
                Log.w(TAG, msg)
                return AlbumSourceResult(emptyList(), msg, failed = true)
            }

            val data = json.optJSONArray("data")
            val list = mutableListOf<AlbumCandidate>()
            if (data != null) {
                for (i in 0 until data.length()) {
                    val o = data.getJSONObject(i)
                    val full = pickPicture(o, "cover_xl", "cover_big", "cover_medium") ?: continue
                    val thumb = pickPicture(o, "cover_medium", "cover_big", "cover_xl") ?: full
                    list.add(
                        AlbumCandidate(
                            id = "deezer-album:${o.optLong("id")}",
                            title = o.optString("title"),
                            artist = o.optJSONObject("artist")?.optString("name").orEmpty(),
                            source = "Deezer",
                            thumbUrl = thumb,
                            fullUrl = full
                        )
                    )
                }
            }
            Log.d(TAG, "Deezer albums '$q' -> ${list.size} résultat(s)")
            AlbumSourceResult(list)
        } catch (e: Exception) {
            Log.w(TAG, "Deezer albums en échec pour '$q'", e)
            AlbumSourceResult(emptyList(), "Deezer : ${e.message ?: e.javaClass.simpleName}", failed = true)
        }
    }

    private suspend fun fetchItunesAlbums(q: String, limit: Int): AlbumSourceResult {
        return try {
            val enc = URLEncoder.encode(q, "UTF-8")
            val body = netGate.withPermit {
                httpGet("https://itunes.apple.com/search?term=$enc&entity=album&limit=$limit")
            }
            val arr = JSONObject(body).optJSONArray("results")
            val list = mutableListOf<AlbumCandidate>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val art = o.str("artworkUrl100") ?: continue
                    list.add(
                        AlbumCandidate(
                            id = "itunes:${o.optLong("collectionId")}",
                            title = o.optString("collectionName"),
                            artist = o.optString("artistName"),
                            source = "iTunes",
                            thumbUrl = resizeItunes(art, 300),
                            fullUrl = resizeItunes(art, 1000)
                        )
                    )
                }
            }
            Log.d(TAG, "iTunes albums '$q' -> ${list.size} résultat(s)")
            AlbumSourceResult(list)
        } catch (e: Exception) {
            Log.w(TAG, "iTunes en échec pour '$q'", e)
            AlbumSourceResult(emptyList(), "iTunes : ${e.message ?: e.javaClass.simpleName}", failed = true)
        }
    }

    private fun resizeItunes(url: String, size: Int) =
        url.replace(Regex("\\d+x\\d+bb(?=\\.\\w+$)"), "${size}x${size}bb")

    // ===================== RÉSEAU / CACHE =====================

    private fun JSONObject.str(name: String): String? =
        optString(name, "").takeIf { it.isNotBlank() && it != "null" }

    private fun isPlaceholder(url: String) =
        url.contains("/artist//") || url.contains("/cover//") ||
                url.contains("d41d8cd98f00b204e9800998ecf8427e") // md5 d'une chaîne vide

    /** Premier champ qui contient une vraie image (pas le silhouette gris de Deezer). */
    private fun pickPicture(o: JSONObject, vararg fields: String): String? {
        for (f in fields) {
            val v = o.str(f) ?: continue
            if (!isPlaceholder(v)) return v
        }
        return null
    }

    private fun httpGet(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun safeKey(key: String) = key.replace(Regex("[\\\\/:*?\"<>|]"), "_")

    private fun artistCacheKey(cleanName: String) = safeKey(CACHE_PREFIX_ARTIST + cleanName.lowercase())

    private fun downloadAndCacheImage(key: String, urlString: String, overwrite: Boolean = true): File? {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                throw IOException("HTTP $code")
            }
            val extension = when {
                urlString.contains(".png", ignoreCase = true) -> "png"
                urlString.contains(".gif", ignoreCase = true) -> "gif"
                else -> "jpg"
            }
            val partFile = File(cacheDir, "$key.$extension.${System.nanoTime()}.part")
            conn.inputStream.use { input ->
                FileOutputStream(partFile).use { out -> input.copyTo(out) }
            }
            conn.disconnect()

            // Détection automatique : si une image existe déjà (choix de l'utilisateur
            // enregistré entre-temps), on la garde.
            if (!overwrite) {
                val existing = getCachedImageFile(key)
                if (existing != null) {
                    partFile.delete()
                    return existing
                }
            }

            // Téléchargement réussi : on remplace l'ancienne image
            deleteCachedFiles(key)
            val cacheFile = File(cacheDir, "$key.$extension")
            if (!partFile.renameTo(cacheFile)) {
                partFile.delete()
                return null
            }
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Erreur téléchargement image: $urlString", e)
            null
        }
    }

    private fun getCachedImageFile(key: String): File? =
        cacheDir.listFiles()?.firstOrNull { it.nameWithoutExtension == key }

    private fun deleteCachedFiles(key: String) {
        cacheDir.listFiles()?.filter { it.nameWithoutExtension == key }?.forEach { it.delete() }
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        notFound.clear()
        Log.d(TAG, "Cache d'images effacé")
    }

    fun cleanExpiredCache() {}

    fun getCacheSize(): Int = cacheDir.listFiles()?.size ?: 0

    fun isImageCached(artistOrAlbumName: String, isArtist: Boolean = true): Boolean {
        val cleanName = if (isArtist) cleanArtistName(artistOrAlbumName) else artistOrAlbumName.trim()
        val prefix = if (isArtist) CACHE_PREFIX_ARTIST else CACHE_PREFIX_ALBUM
        return getCachedImageFile(safeKey(prefix + cleanName.lowercase())) != null
    }

    // ===================== NOMS =====================

    private fun cleanArtistName(artistName: String): String =
        artistName
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("(?i)\\b(?:featuring|feat|ft)\\b\\.?.*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** "Drake (musician)" -> "Drake" */
    private fun baseName(name: String) = name.replace(Regex("\\s*\\(.*?\\)\\s*$"), "").trim()

    private fun similarity(searched: String, found: String): Double {
        val a = searched.lowercase().trim()
        val b = found.lowercase().trim()
        if (a == b) return 1.0
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 0.0
        return 1.0 - levenshteinDistance(a, b).toDouble() / maxLen
    }

    private fun isArtistMatch(searched: String, found: String): Boolean {
        val a = searched.lowercase().trim()
        val b = found.lowercase().trim()
        if (a == b) return true
        if (b.contains(a) || a.contains(b)) return true
        return similarity(searched, found) > 0.7
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }
}
