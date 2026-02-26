package fr.kevw.kenemimusic

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayCount(
    val songId: Long,
    val songTitle: String,
    val artistName: String,
    val count: Int,
    val lastPlayed: Long,
    val totalPlayTime: Long
)

class StatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("music_stats", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PLAY_COUNTS = "play_counts"
        private const val KEY_TOTAL_PLAYS = "total_plays"
        private const val KEY_TOTAL_LISTENING_TIME = "total_listening_time"
        private const val KEY_RECENTLY_PLAYED = "recently_played"
    }

    // ✅ AJOUT : StateFlow pour notifier les changements
    private val _statsUpdated = MutableStateFlow(0L)
    val statsUpdated: StateFlow<Long> = _statsUpdated.asStateFlow()

    private var playCounts: MutableMap<Long, PlayCount> = loadPlayCounts()
    private var totalPlays: Int = prefs.getInt(KEY_TOTAL_PLAYS, 0)
    private var totalListeningTime: Long = prefs.getLong(KEY_TOTAL_LISTENING_TIME, 0)
    private var recentlyPlayed: MutableList<Long> = loadRecentlyPlayed()

    private fun loadPlayCounts(): MutableMap<Long, PlayCount> {
        val json = prefs.getString(KEY_PLAY_COUNTS, null)
        return if (json != null) {
            val type = object : TypeToken<MutableMap<Long, PlayCount>>() {}.type
            gson.fromJson(json, type) ?: mutableMapOf()
        } else {
            mutableMapOf()
        }
    }

    private fun loadRecentlyPlayed(): MutableList<Long> {
        val json = prefs.getString(KEY_RECENTLY_PLAYED, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Long>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }
    }

    private fun saveData() {
        prefs.edit()
            .putString(KEY_PLAY_COUNTS, gson.toJson(playCounts))
            .putInt(KEY_TOTAL_PLAYS, totalPlays)
            .putLong(KEY_TOTAL_LISTENING_TIME, totalListeningTime)
            .putString(KEY_RECENTLY_PLAYED, gson.toJson(recentlyPlayed))
            .apply()

        // ✅ AJOUT : Notifier que les stats ont changé
        _statsUpdated.value = System.currentTimeMillis()

        // ✅ DEBUG : Log pour vérifier
        android.util.Log.d("StatsManager", "Stats saved - Total plays: $totalPlays, Songs: ${playCounts.size}")
    }

    fun recordSongPlay(song: Song, playDuration: Long = 0) {
        val currentTime = System.currentTimeMillis()
        val existingCount = playCounts[song.id]

        val newCount = PlayCount(
            songId = song.id,
            songTitle = song.title,
            artistName = song.artist,
            count = (existingCount?.count ?: 0) + 1,
            lastPlayed = currentTime,
            totalPlayTime = (existingCount?.totalPlayTime ?: 0) + playDuration
        )

        playCounts[song.id] = newCount
        totalPlays++
        totalListeningTime += playDuration

        recentlyPlayed.remove(song.id)
        recentlyPlayed.add(0, song.id)
        if (recentlyPlayed.size > 20) {
            recentlyPlayed.removeAt(recentlyPlayed.size - 1)
        }

        // ✅ DEBUG : Log pour vérifier l'enregistrement
        android.util.Log.d("StatsManager", "Recorded: ${song.title} - Duration: ${playDuration}s - Total plays: $totalPlays")

        saveData()
    }

    fun getTopSongs(songs: List<Song>, limit: Int = 5): List<PlayCount> {
        return playCounts.values
            .sortedByDescending { it.count }
            .take(limit)
    }

    fun getRecentlyPlayed(songs: List<Song>, limit: Int = 15): List<PlayCount> {
        return recentlyPlayed
            .mapNotNull { songId -> playCounts[songId] }
            .sortedByDescending { it.lastPlayed }
            .take(limit)
    }

    fun getTopArtists(songs: List<Song>, limit: Int = 5): List<Pair<String, Int>> {
        val artistCounts = mutableMapOf<String, Int>()

        playCounts.values.forEach { playCount ->
            artistCounts[playCount.artistName] =
                (artistCounts[playCount.artistName] ?: 0) + playCount.count
        }

        return artistCounts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun getTotalStats(): Triple<Int, Int, Long> {
        val totalSongsPlayed = playCounts.size
        return Triple(totalPlays, totalSongsPlayed, totalListeningTime)
    }

    fun clearAllStats() {
        playCounts.clear()
        totalPlays = 0
        totalListeningTime = 0
        recentlyPlayed.clear()
        saveData()
    }

    fun getSongPlayCount(songId: Long): Int {
        return playCounts[songId]?.count ?: 0
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return when {
            hours > 0 -> String.format("%dh %02dm", hours, minutes)
            minutes > 0 -> String.format("%dm %02ds", minutes, remainingSeconds)
            else -> String.format("%ds", remainingSeconds)
        }
    }
}