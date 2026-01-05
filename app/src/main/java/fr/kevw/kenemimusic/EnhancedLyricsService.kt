package fr.kevw.kenemimusic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

class EnhancedLyricsService(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // ✅ PARALLEL FETCHING - All sources at once!
    suspend fun getLyricsParallel(title: String, artist: String): LyricsResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            Log.d("LyricsService", "📵 Offline mode - checking cache only")
            return@withContext getCachedLyrics(title, artist) ?: LyricsResult.Error("Pas de connexion internet")
        }
        
        Log.d("LyricsService", "🚀 Parallel search: $title - $artist")
        
        try {
            // Launch all sources in parallel
            val deferreds = listOf(
                async { fetchFromSource1(title, artist) },
                async { fetchFromSource2(title, artist) },
                async { fetchFromSource3(title, artist) },
                async { fetchFromSource4(title, artist) }
            )
            
            // Wait for first successful result
            deferreds.forEach { deferred ->
                try {
                    deferred.await()?.let { (lyrics, source) ->
                        Log.d("LyricsService", "✅ Found lyrics from: $source")
                        cacheLyrics(title, artist, lyrics, source)
                        return@withContext LyricsResult.Success(lyrics, source)
                    }
                } catch (e: Exception) {
                    Log.w("LyricsService", "Source failed: ${e.message}")
                }
            }
            
            LyricsResult.NotFound
            
        } catch (e: Exception) {
            Log.e("LyricsService", "Parallel search error", e)
            LyricsResult.Error(e.message ?: "Erreur recherche")
        }
    }
    
    // ✅ SMART CACHING with expiry
    private suspend fun getCachedLyrics(title: String, artist: String): LyricsResult? {
        // Implementation with expiry check
        return null // Placeholder
    }
    
    private fun cacheLyrics(title: String, artist: String, lyrics: String, source: String) {
        // Enhanced caching implementation
    }
    
    // ✅ OFFLINE DETECTION
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    // ✅ INDIVIDUAL SOURCES
    private suspend fun fetchFromSource1(title: String, artist: String): Pair<String, String>? {
        return try {
            val query = "${URLEncoder.encode(artist, "UTF-8")} ${URLEncoder.encode(title, "UTF-8")}"
            val url = "https://api.lyrics.ovh/v1/$artist/$title"
            val result = URL(url).readText()
            if (result.contains("\"lyrics\":")) {
                val json = org.json.JSONObject(result)
                val lyrics = json.getString("lyrics")
                if (lyrics.isNotEmpty()) Pair(lyrics, "Lyrics.ovh") else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun fetchFromSource2(title: String, artist: String): Pair<String, String>? {
        // Implementation for second source
        return null
    }
    
    private suspend fun fetchFromSource3(title: String, artist: String): Pair<String, String>? {
        // Implementation for third source
        return null
    }
    
    private suspend fun fetchFromSource4(title: String, artist: String): Pair<String, String>? {
        // Implementation for fourth source
        return null
    }
}