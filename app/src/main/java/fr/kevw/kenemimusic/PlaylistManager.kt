package fr.kevw.kenemimusic

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlaylistManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("playlists_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_FAVORITES = "favorites"
        const val FAVORITES_ID = "favorites_playlist"
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val json = gson.toJson(playlists)
        prefs.edit().putString(KEY_PLAYLISTS, json).apply()
    }

    fun loadPlaylists(): List<Playlist> {
        val json = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Playlist>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun addPlaylist(playlist: Playlist) {
        val currentPlaylists = loadPlaylists().toMutableList()
        currentPlaylists.add(playlist)
        savePlaylists(currentPlaylists)
    }

    fun deletePlaylist(playlistId: String) {
        val currentPlaylists = loadPlaylists().toMutableList()
        currentPlaylists.removeAll { it.id == playlistId }
        savePlaylists(currentPlaylists)
    }

    fun updatePlaylist(playlistId: String, newName: String, newSongIds: List<Long>) {
        val currentPlaylists = loadPlaylists().toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            currentPlaylists[index] = currentPlaylists[index].copy(
                name = newName,
                songIds = newSongIds
            )
            savePlaylists(currentPlaylists)
        }
    }

    fun clearAllPlaylists() {
        prefs.edit().remove(KEY_PLAYLISTS).apply()
    }

    // ===== FAVORIS =====

    fun loadFavorites(): Set<Long> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptySet()
        return try {
            val type = object : TypeToken<Set<Long>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    private fun saveFavorites(favorites: Set<Long>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString(KEY_FAVORITES, json).apply()
    }

    fun addToFavorites(songId: Long) {
        val favorites = loadFavorites().toMutableSet()
        favorites.add(songId)
        saveFavorites(favorites)
    }

    fun removeFromFavorites(songId: Long) {
        val favorites = loadFavorites().toMutableSet()
        favorites.remove(songId)
        saveFavorites(favorites)
    }

    fun isFavorite(songId: Long): Boolean {
        return loadFavorites().contains(songId)
    }

    fun toggleFavorite(songId: Long): Boolean {
        return if (isFavorite(songId)) {
            removeFromFavorites(songId)
            false
        } else {
            addToFavorites(songId)
            true
        }
    }
}