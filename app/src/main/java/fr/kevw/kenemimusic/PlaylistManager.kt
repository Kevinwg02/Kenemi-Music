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
    }

    /**
     * Sauvegarde toutes les playlists
     */
    fun savePlaylists(playlists: List<Playlist>) {
        val json = gson.toJson(playlists)
        prefs.edit().putString(KEY_PLAYLISTS, json).apply()
    }

    /**
     * Charge toutes les playlists sauvegardées
     */
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

    /**
     * Ajoute une nouvelle playlist
     */
    fun addPlaylist(playlist: Playlist) {
        val currentPlaylists = loadPlaylists().toMutableList()
        currentPlaylists.add(playlist)
        savePlaylists(currentPlaylists)
    }

    /**
     * Supprime une playlist
     */
    fun deletePlaylist(playlistId: String) {
        val currentPlaylists = loadPlaylists().toMutableList()
        currentPlaylists.removeAll { it.id == playlistId }
        savePlaylists(currentPlaylists)
    }

    /**
     * Met à jour une playlist existante
     */
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

    /**
     * Efface toutes les playlists (utile pour debug/reset)
     */
    fun clearAllPlaylists() {
        prefs.edit().remove(KEY_PLAYLISTS).apply()
    }
}