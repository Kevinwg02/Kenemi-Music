@file:OptIn(ExperimentalMaterial3Api::class)

package fr.kevw.kenemimusic

import androidx.compose.material.icons.filled.Analytics
import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File
import kotlin.random.Random
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.collectAsState
import android.util.Log
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.core.view.WindowCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
// ===== PERSONNALISATION DES COULEURS =====
@Composable
fun customColorScheme() = darkColorScheme(
    primary = Color(0xFF03A9F4),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF01579B),
    onPrimaryContainer = Color(0xFFE1F5FE),

    secondary = Color(0xFF00BCD4),
    onSecondary = Color(0xFF000000),

    tertiary = Color(0xFFFF5722),
    onTertiary = Color(0xFF000000),

    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFB0B0B0),

    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),

    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF2A2A2A),

    scrim = Color(0x99000000),

    surfaceTint = Color(0xFF03A9F4)
)

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri,
    val albumId: Long = 0,
    val album: String = ""
)

data class Album(
    val id: Long, val name: String, val artist: String, val songCount: Int, val artUri: Uri?
)

data class Artist(
    val id: Long, val name: String, val albumCount: Int, val songCount: Int
)

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long = System.currentTimeMillis()
)

data class MusicFolder(
    val path: String, val name: String, val songCount: Int
)

enum class RepeatMode {
    OFF, ONE, ALL
}

@Composable
fun PermissionRequiredMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permission requise",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pour afficher $title, vous devez autoriser l'accès aux fichiers audio dans les paramètres de l'application.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ouvrir les paramètres")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Paramètres → Kenemi Music → Autorisations → Fichiers et contenus multimédias",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

class MainActivity : ComponentActivity() {
    private val songs = mutableStateListOf<Song>()
    private val albums = mutableStateListOf<Album>()
    private val artists = mutableStateListOf<Artist>()
    private val playlists = mutableStateListOf<Playlist>()
    private val folders = mutableStateListOf<MusicFolder>()
    private var hasPermission by mutableStateOf(false)
    private val favorites = mutableStateListOf<Long>()
    private var musicService: MusicService? by mutableStateOf(null)
    private var serviceBound = false
    private var showQueueScreen by mutableStateOf(false)
    private var showLyricsScreen by mutableStateOf(false)

    // ✅ FIX: Initialize these BEFORE onCreate
    private lateinit var lyricsService: LyricsService
    private lateinit var playlistManager: PlaylistManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var statsManager: StatsManager

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MainActivity", "Service connected")
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true

            // ✅ FIX: Restore last song AFTER service is connected AND songs are loaded
            restoreLastPlayedSong()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("MainActivity", "Service disconnected")
            serviceBound = false
            musicService = null
        }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "Permission result: $isGranted")
        hasPermission = isGranted
        if (isGranted) {
            loadAppDataAsync()
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "Notification permission: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")

        // ✅ FIX: Initialize services FIRST
        try {
            ImageServiceSingleton.init(this)
            settingsManager = SettingsManager(this)
            playlistManager = PlaylistManager(this)
            statsManager = StatsManager(this)
            lyricsService = LyricsService(this)

            Log.d("MainActivity", "All services initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing services", e)
        }

        // Load saved data
        playlists.addAll(playlistManager.loadPlaylists())
        favorites.addAll(playlistManager.loadFavorites())

        // Start and bind music service
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Check permissions
        checkPermission()

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val player = musicService
            val imageService = ImageServiceSingleton.getArtistImageService()
            val isDarkThemeState = remember { mutableStateOf(settingsManager.isDarkTheme) }

            // ✅ FIX: Set managers when service becomes available
            LaunchedEffect(player) {
                player?.let {
                    it.setStatsManager(statsManager)
                    it.setSettingsManager(settingsManager)
                    Log.d("MainActivity", "Managers set on service")
                }
            }

            MaterialTheme(
                colorScheme = if (isDarkThemeState.value) customColorScheme()
                else lightColorScheme(
                    primary = Color(0xFF03A9F4),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE1F5FE),
                    onPrimaryContainer = Color(0xFF01579B),
                    secondary = Color(0xFF00BCD4),
                    onSecondary = Color(0xFF000000),
                    tertiary = Color(0xFFFF5722),
                    onTertiary = Color(0xFFFFFFFF),
                    surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF000000),
                    surfaceVariant = Color(0xFFF5F5F5),
                    onSurfaceVariant = Color(0xFF757575),
                    background = Color(0xFFFFFFFF),
                    onBackground = Color(0xFF000000),
                    outline = Color(0xFFE0E0E0),
                    outlineVariant = Color(0xFFEEEEEE),
                    surfaceTint = Color(0xFF03A9F4)
                )
            ) {
                if (player == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    MusicApp(
                        songs = songs,
                        albums = albums,
                        artists = artists,
                        playlists = playlists,
                        folders = folders,
                        favorites = favorites,
                        playlistManager = playlistManager,
                        statsManager = statsManager,
                        hasPermission = hasPermission,
                        imageService = imageService,
                        musicPlayer = player,
                        isDarkThemeState = isDarkThemeState,
                        lyricsService = lyricsService,
                        onRequestPermission = { requestPermission.launch(getPermissionString()) },
                        onCreatePlaylist = { name, songIds ->
                            val newPlaylist = Playlist(
                                id = System.currentTimeMillis().toString(),
                                name = name,
                                songIds = songIds
                            )
                            playlists.add(newPlaylist)
                            playlistManager.addPlaylist(newPlaylist)
                        },
                        onDeletePlaylist = { playlist ->
                            playlists.remove(playlist)
                            playlistManager.deletePlaylist(playlist.id)
                        },
                        onUpdatePlaylist = { playlistId, newName, newSongIds ->
                            val index = playlists.indexOfFirst { it.id == playlistId }
                            if (index != -1) {
                                playlists[index] = playlists[index].copy(
                                    name = newName,
                                    songIds = newSongIds
                                )
                                playlistManager.updatePlaylist(playlistId, newName, newSongIds)
                            }
                        },
                        onToggleFavorite = { songId ->
                            val isFavorite = playlistManager.toggleFavorite(songId)
                            if (isFavorite) {
                                favorites.add(songId)
                            } else {
                                favorites.remove(songId)
                            }
                        }
                    )
                }
            }
        }
    }

    // ✅ FIX: New function to restore last played song
    private fun restoreLastPlayedSong() {
        val service = musicService
        if (service == null) {
            Log.w("MainActivity", "Cannot restore: service is null")
            return
        }

        if (songs.isEmpty()) {
            Log.w("MainActivity", "Cannot restore: songs list is empty")
            return
        }

        try {
            val lastSongId = settingsManager.lastPlayedSongId
            Log.d("MainActivity", "Attempting to restore song ID: $lastSongId")

            if (lastSongId != -1L) {
                val lastSong = songs.firstOrNull { it.id == lastSongId }
                if (lastSong != null) {
                    Log.d("MainActivity", "Restoring: ${lastSong.title}")
                    service.setPlaylist(songs)
                    service.playSong(lastSong)
                    service.pause() // Start paused

                    val savedPosition = settingsManager.lastPlayedPosition
                    if (savedPosition > 0) {
                        service.seekTo(savedPosition)
                        Log.d("MainActivity", "Restored position: ${savedPosition}ms")
                    }
                } else {
                    Log.w("MainActivity", "Last song not found in current library")
                }
            } else {
                Log.d("MainActivity", "No last played song saved")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error restoring last song", e)
        }
    }

    private fun forceScanMusic() {
        Log.d("MainActivity", "Force scan requested")
        loadAppDataAsync()
    }

    private fun getPermissionString(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun checkPermission() {
        hasPermission = ContextCompat.checkSelfPermission(
            this,
            getPermissionString()
        ) == PackageManager.PERMISSION_GRANTED

        Log.d("MainActivity", "Has permission: $hasPermission")

        if (hasPermission) {
            loadAppDataAsync()
        }
    }

    private fun loadAppDataAsync() {
        Log.d("MainActivity", "Loading app data...")
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val songsDeferred = async { loadSongs() }
                val albumsDeferred = async { loadAlbums() }
                val artistsDeferred = async { loadArtists() }
                val foldersDeferred = async { loadFolders() }

                songsDeferred.await()
                albumsDeferred.await()
                artistsDeferred.await()
                foldersDeferred.await()

                injectSecondaryArtists()
                Log.d("MainActivity", "Data loaded: ${songs.size} songs")

                // ✅ FIX: Try to restore after songs are loaded
                launch(Dispatchers.Main) {
                    restoreLastPlayedSong()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading data", e)
            }
        }
    }
    private fun injectSecondaryArtists() {
        val existingNames = artists.map { it.name.lowercase() }.toHashSet()
        val toAdd = mutableMapOf<String, Int>() // name → nb de chansons

        songs.forEach { song ->
            val allArtists = ArtistUtils.parseArtists(song.artist)

            allArtists.forEach { name ->
                val key = name.lowercase()
                if (key !in existingNames) {
                    // Artiste pas encore connu → l'ajouter comme virtuel
                    toAdd[name] = (toAdd[name] ?: 0) + 1
                }
                // Si déjà connu → rien à faire, loadArtists() l'a déjà compté
            }
        }

        toAdd.forEach { (name, count) ->
            artists.add(Artist(id = -1L, name = name, albumCount = 0, songCount = count))
            Log.d("MainActivity", "Artiste secondaire ajouté: $name ($count chansons)")
        }

        // Retrier après ajout
        val sorted = artists.sortedBy { it.name }
        artists.clear()
        artists.addAll(sorted)

        Log.d("MainActivity", "Artistes secondaires ajoutés: ${toAdd.size}")
    }
    private fun loadSongs() {
        val songList = mutableListOf<Song>()

        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("%${settingsManager.musicFolderName}%")

            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: continue
                    val artist = cursor.getString(artistColumn) ?: "Artiste inconnu"
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val album = cursor.getString(albumColumn) ?: "Album inconnu"
                    val dataPath = cursor.getString(dataColumn) ?: continue

                    if (duration <= 0) continue

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    try {
                        contentResolver.openInputStream(uri)?.close()
                    } catch (e: Exception) {
                        continue
                    }

                    songList.add(
                        Song(id, title, artist, duration, uri, albumId, album)
                    )
                }
            }

            songs.clear()
            songs.addAll(songList)
            Log.d("MainActivity", "Loaded ${songs.size} songs")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading songs", e)
        }
    }

    private fun loadAlbums() {
        val albumMap = mutableMapOf<Long, Album>()

        try {
            val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS
            )

            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Albums.ALBUM} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val countColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val album = cursor.getString(albumColumn) ?: "Album inconnu"
                    val artist = cursor.getString(artistColumn) ?: "Artiste inconnu"
                    val count = cursor.getInt(countColumn)

                    val artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        id
                    )

                    albumMap[id] = Album(id, album, artist, count, artUri)
                }
            }

            albums.clear()
            albums.addAll(albumMap.values)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading albums", e)
        }
    }

    private fun loadArtists() {
        val artistMap = mutableMapOf<String, Artist>() // clé = nom lowercase

        try {
            val collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS
            )

            contentResolver.query(
                collection, projection, null, null,
                "${MediaStore.Audio.Artists.ARTIST} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val artistField = cursor.getString(artistColumn) ?: "Artiste inconnu"
                    val albumCount = cursor.getInt(albumCountColumn)
                    val trackCount = cursor.getInt(trackCountColumn)

                    // Éclater les artistes composés ("A / B", "A feat. B", etc.)
                    val parsedNames = ArtistUtils.parseArtists(artistField)

                    parsedNames.forEachIndexed { index, name ->
                        val key = name.lowercase()
                        if (artistMap.containsKey(key)) {
                            // L'artiste existe déjà, incrémenter le songCount
                            val existing = artistMap[key]!!
                            artistMap[key] = existing.copy(songCount = existing.songCount + trackCount)
                        } else {
                            // Premier artiste = id réel de MediaStore, les suivants = -1 (virtuels)
                            artistMap[key] = Artist(
                                id = if (index == 0) id else -1L,
                                name = name,
                                albumCount = if (index == 0) albumCount else 0,
                                songCount = trackCount
                            )
                        }
                    }
                }
            }

            artists.clear()
            artists.addAll(artistMap.values.sortedBy { it.name })
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading artists", e)
        }
    }

    private fun loadFolders() {
        val folderMap = mutableMapOf<String, MutableList<Song>>()

        try {
            contentResolver.query(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                },
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DATA
                ),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?",
                arrayOf("%${settingsManager.musicFolderName}%"),
                "${MediaStore.Audio.Media.DATA} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: continue
                    val artist = cursor.getString(artistColumn) ?: "Artiste inconnu"
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val album = cursor.getString(albumColumn) ?: "Album inconnu"
                    val dataPath = cursor.getString(dataColumn) ?: continue

                    if (duration <= 0 || !dataPath.contains("/Music/", ignoreCase = true)) {
                        continue
                    }

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val song = Song(id, title, artist, duration, uri, albumId, album)

                    val file = File(dataPath)
                    val folderFile = file.parentFile ?: continue

                    if (!folderFile.absolutePath.contains("/Music/", ignoreCase = true)) {
                        continue
                    }

                    val folderPath = folderFile.absolutePath
                    if (!folderMap.containsKey(folderPath)) {
                        folderMap[folderPath] = mutableListOf()
                    }
                    folderMap[folderPath]?.add(song)
                }
            }

            val folderList = folderMap.map { (path, folderSongs) ->
                val folderFile = File(path)
                MusicFolder(path, folderFile.name, folderSongs.size)
            }.sortedBy { it.name }

            folders.clear()
            folders.addAll(folderList)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading folders", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")

        musicService?.let { service ->
            service.currentSong?.let { song ->
                settingsManager.lastPlayedSongId = song.id
                settingsManager.lastPlayedPosition = service.currentPosition
                Log.d("MainActivity", "Saved state: ${song.title} at ${service.currentPosition}ms")
            }
        }

        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onPause() {
        super.onPause()
        musicService?.let { service ->
            service.currentSong?.let { song ->
                settingsManager.lastPlayedSongId = song.id
                settingsManager.lastPlayedPosition = service.currentPosition
            }
        }
    }

    @Composable
    fun MusicApp(
        songs: List<Song>,
        albums: List<Album>,
        artists: List<Artist>,
        playlists: List<Playlist>,
        folders: List<MusicFolder>,
        hasPermission: Boolean,
        musicPlayer: MusicService,
        imageService: ArtistImageService,
        isDarkThemeState: MutableState<Boolean>,
        favorites: List<Long>,
        playlistManager: PlaylistManager,
        statsManager: StatsManager,
        lyricsService: LyricsService,
        onRequestPermission: () -> Unit,
        onCreatePlaylist: (String, List<Long>) -> Unit,
        onDeletePlaylist: (Playlist) -> Unit,
        onUpdatePlaylist: (String, String, List<Long>) -> Unit,
        onToggleFavorite: (Long) -> Unit
    ) {
        var selectedTab by remember { mutableStateOf(0) }
        var selectedArtist by remember { mutableStateOf<Artist?>(null) }
        var selectedAlbum by remember { mutableStateOf<Album?>(null) }
        var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
        var selectedFolder by remember { mutableStateOf<MusicFolder?>(null) }

        var showQueueScreen by remember { mutableStateOf(false) }
        var showLyricsScreen by remember { mutableStateOf(false) }

        LaunchedEffect(selectedTab) {
            if (selectedTab != 2) selectedAlbum = null
            if (selectedTab != 3) selectedArtist = null
            if (selectedTab != 4) selectedPlaylist = null
            if (selectedTab != 5) selectedFolder = null

            showLyricsScreen = false
            showQueueScreen = false
        }
        BackHandler(enabled = true) {
            when {
                // Si on est dans l'écran de la queue
                showQueueScreen -> {
                    showQueueScreen = false
                }
                // Si on est dans les paramètres
                selectedTab == 6 -> {
                    selectedTab = 0
                }
                // Si on visualise un album
                selectedAlbum != null -> {
                    selectedAlbum = null
                }
                // Si on visualise un artiste
                selectedArtist != null -> {
                    selectedArtist = null
                }
                // Si on visualise une playlist
                selectedPlaylist != null -> {
                    selectedPlaylist = null
                }
                // Si on visualise un dossier
                selectedFolder != null -> {
                    selectedFolder = null
                }
                // Si on est sur l'onglet principal (lecteur), quitter l'app
                selectedTab == 0 -> {
                    // Optionnel : afficher un Toast "Appuyez encore pour quitter"
                    // ou simplement ne rien faire (comportement par défaut Android)
                }
                // Sinon, retour à l'onglet principal
                else -> {
                    selectedTab = 0
                }
            }
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface, bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF000000).copy(alpha = 0.85f),
                    windowInsets = WindowInsets.navigationBars
//                    modifier = Modifier.height(92.dp)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PlayArrow, "Lecteur", Modifier.size(24.dp)) },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MusicNote, "Chansons", Modifier.size(24.dp)) },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Album, "Albums", Modifier.size(24.dp)) },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, "Artistes", Modifier.size(24.dp)) },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.QueueMusic, "Playlists", Modifier.size(24.dp)) },
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Folder, "Dossiers", Modifier.size(24.dp)) },
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, "Paramètres", Modifier.size(24.dp)) },
                        selected = selectedTab == 6,
                        onClick = { selectedTab = 6 }
                    )
                }
            }) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> MusicPlayerScreen(
                        musicPlayer = musicPlayer,
                        imageService = imageService,
                        favorites = favorites,
                        onToggleFavorite = onToggleFavorite,
                        onShowLyrics = { showLyricsScreen = true },
                        onShowQueue = { showQueueScreen = true },
                        onNavigateToArtist = { artistName ->
                            val artist = artists.firstOrNull { it.name == artistName }
                            if (artist != null) {
                                selectedArtist = artist
                                selectedTab = 3  // Navigate to artists tab
                            }
                        }
                        )

                    1 -> SongListScreen(
                        songs = songs,
                        hasPermission = hasPermission,
                        onRequestPermission = onRequestPermission,
                        musicPlayer = musicPlayer,
                        onSongClick = { song ->
                            musicPlayer.setPlaylist(songs)
                            musicPlayer.playSong(song)
                            selectedTab = 0
                        },
                        selectedTab = selectedTab
                    )

                    2 -> {

                        if (selectedAlbum != null) {
                            val albumSongs = songs.filter { it.albumId == selectedAlbum!!.id }
                            BackHandler { selectedAlbum = null }
                            AlbumDetailScreen(
                                album = selectedAlbum!!,
                                songs = albumSongs,
                                onBack = { selectedAlbum = null },
                                imageService = imageService,
                                musicPlayer = musicPlayer,
                                onSongClick = { song ->
                                    musicPlayer.setPlaylist(albumSongs)
                                    musicPlayer.playSong(song)
                                    selectedTab = 0
                                })
                        } else {
                            AlbumsScreen(
                                albums = albums,
                                hasPermission = hasPermission,
                                imageService = imageService,
                                onRequestPermission = onRequestPermission,
                                onAlbumClick = { album -> selectedAlbum = album })
                        }
                    }

                    3 -> {
                        if (selectedArtist != null) {
                            val artistSongs = songs.filter { song ->
                                ArtistUtils.songBelongsToArtist(song, selectedArtist!!.name)
                            }
                            val imageService = ImageServiceSingleton.getArtistImageService()
                            BackHandler { selectedArtist = null }
                            ArtistDetailScreen(
                                artist = selectedArtist!!,
                                songs = artistSongs,
                                imageService = imageService,
                                musicPlayer = musicPlayer,
                                onBack = { selectedArtist = null },
                                onSongClick = { song ->
                                    musicPlayer.setPlaylist(artistSongs)
                                    musicPlayer.playSong(song)
                                    selectedTab = 0
                                })
                        } else {
                            ArtistsScreen(
                                artists = artists,
                                hasPermission = hasPermission,
                                imageService = imageService,
                                onRequestPermission = onRequestPermission,
                                onArtistClick = { artist -> selectedArtist = artist })
                        }
                    }

                    4 -> {
                        if (selectedPlaylist != null) {
                            val playlistSongs =
                                songs.filter { song -> selectedPlaylist!!.songIds.contains(song.id) }

                            BackHandler { selectedArtist = null }
                            PlaylistDetailScreen(
                                playlist = selectedPlaylist!!,
                                songs = playlistSongs,
                                allSongs = songs,
                                musicPlayer = musicPlayer,
                                onBack = { selectedPlaylist = null },
                                onSongClick = { song ->
                                    musicPlayer.setPlaylist(playlistSongs)
                                    musicPlayer.playSong(song)
                                    selectedTab = 0
                                },
                                onDeletePlaylist = {
                                    onDeletePlaylist(selectedPlaylist!!)
                                    selectedPlaylist = null
                                },
                                onUpdatePlaylist = { newName, newSongIds ->
                                    onUpdatePlaylist(selectedPlaylist!!.id, newName, newSongIds)
                                    selectedPlaylist = selectedPlaylist!!.copy(
                                        name = newName, songIds = newSongIds
                                    )
                                })
                        } else {
                            PlaylistsScreen(
                                playlists = playlists,
                                songs = songs,
                                hasPermission = hasPermission,
                                favorites = favorites,
                                playlistManager = playlistManager,
                                statsManager = statsManager,
                                onRequestPermission = onRequestPermission,
                                onPlaylistClick = { selectedPlaylist = it },
                                onCreatePlaylist = onCreatePlaylist,
                                onToggleFavorite = onToggleFavorite
                            )
                        }
                    }
//                    5 -> {
//                        if (selectedFolder != null) {
//                            val folderSongs = songs.filter { song ->
//                                val path = song.uri.path ?: ""
//                                File(path).parent == selectedFolder!!.path
//                            }
//                            FolderDetailScreen(
//                                folder = selectedFolder!!,
//                                songs = folderSongs,
//                                onBack = { selectedFolder = null },
//                                onSongClick = { song ->
//                                    musicPlayer.setPlaylist(folderSongs)
//                                    musicPlayer.playSong(song)
//                                    selectedTab = 0
//                                }
//                            )
//                        } else {
//                            FoldersScreen(
//                                folders = folders,
//                                hasPermission = hasPermission,
//                                onRequestPermission = onRequestPermission,
//                                onFolderClick = { folder -> selectedFolder = folder }
//                            )
//                        }
//                    }
                    5 -> {
                        if (selectedFolder != null) {
                            val folderSongs = songs.filter { song ->
                                // Get actual file path from content resolver
                                var isInFolder = false
                                contentResolver.query(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    arrayOf(MediaStore.Audio.Media.DATA),
                                    "${MediaStore.Audio.Media._ID} = ?",
                                    arrayOf(song.id.toString()),
                                    null
                                )?.use { cursor ->
                                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                                    if (cursor.moveToFirst()) {
                                        val dataPath = cursor.getString(dataColumn)
                                        val file = File(dataPath)
                                        isInFolder = file.parentFile?.absolutePath == selectedFolder!!.path
                                    }
                                }
                                isInFolder
                            }
                            FolderDetailScreen(
                                folder = selectedFolder!!,
                                songs = folderSongs,
                                onBack = { selectedFolder = null },
                                onSongClick = { song ->
                                    musicPlayer.setPlaylist(folderSongs)
                                    musicPlayer.playSong(song)
                                    selectedTab = 0
                                }
                            )
                        } else {
                            FoldersScreen(
                                folders = folders,
                                hasPermission = hasPermission,
                                onRequestPermission = onRequestPermission,
                                onFolderClick = { folder -> selectedFolder = folder }
                            )
                        }
                    }
                    6 -> {
                        // ✅ AJOUTEZ BackHandler ICI AUSSI
                        BackHandler { selectedTab = 0 }

                        SettingsScreen(
                            settingsManager = settingsManager,
                            onBack = { selectedTab = 0 },
                            onForceScan = { forceScanMusic() },
                            onThemeChanged = { newTheme ->
                                isDarkThemeState.value = newTheme
                                settingsManager.isDarkTheme = newTheme  // ✅ Save theme permanently
                            },
                            onRequestPermission = { requestPermission.launch(getPermissionString()) }
                        )
                    }

                }
                // ✅ ÉCRAN DES PAROLES (avec fermeture automatique)
                if (showLyricsScreen && musicPlayer.currentSong != null) {
                    LyricsScreen(
                        song = musicPlayer.currentSong!!,
                        lyricsService = lyricsService,
                        imageService = imageService,
                        onBack = { showLyricsScreen = false }
                    )
                }

                // ÉCRAN DE LA QUEUE
                if (showQueueScreen) {
                    CurrentQueueScreen(
                        musicPlayer = musicPlayer,
                        onBack = { showQueueScreen = false },
                        onReorder = { fromIndex, toIndex ->
                            val currentPlaylist = musicPlayer.getCurrentPlaylist().toMutableList()
                            val item = currentPlaylist.removeAt(fromIndex)
                            currentPlaylist.add(toIndex, item)
                            musicPlayer.updatePlaylist(currentPlaylist)
                        }
                    )
                }


                if (showQueueScreen) {
                    CurrentQueueScreen(
                        musicPlayer = musicPlayer,
                        onBack = { showQueueScreen = false },
                        onReorder = { fromIndex, toIndex ->
                            val currentPlaylist = musicPlayer.getCurrentPlaylist().toMutableList()
                            val item = currentPlaylist.removeAt(fromIndex)
                            currentPlaylist.add(toIndex, item)
                            musicPlayer.updatePlaylist(currentPlaylist)
                        }
                    )
                }

                if (showLyricsScreen && musicPlayer.currentSong != null) {
                    LyricsScreen(
                        song = musicPlayer.currentSong!!,
                        lyricsService = lyricsService,
                        imageService = imageService,
                        onBack = { showLyricsScreen = false }
                    )
                }

                if (showQueueScreen) {
                    CurrentQueueScreen(
                        musicPlayer = musicPlayer,
                        onBack = { showQueueScreen = false },
                        onReorder = { fromIndex, toIndex ->
                            val currentPlaylist = musicPlayer.getCurrentPlaylist().toMutableList()
                            val item = currentPlaylist.removeAt(fromIndex)
                            currentPlaylist.add(toIndex, item)
                            musicPlayer.updatePlaylist(currentPlaylist)
                        }
                    )
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun CurrentQueueScreen(
        musicPlayer: MusicService,
        onBack: () -> Unit,
        onReorder: (Int, Int) -> Unit
    ) {
        val currentSong = musicPlayer.currentSong

        // ✅ SOLUTION : Forcer la recomposition avec playlistVersion
        val playlist = remember(musicPlayer.playlistVersion) {
            musicPlayer.getCurrentPlaylist()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Lecture en cours")
                            Text(
                                "${playlist.size} chanson${if (playlist.size > 1) "s" else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Retour")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            musicPlayer.updatePlaylist(emptyList())
                            onBack()
                        }) {
                            Icon(Icons.Default.Clear, "Vider la file")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            if (playlist.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "File d'attente vide",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Lancez une chanson pour commencer",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    itemsIndexed(
                        items = playlist,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        QueueSongItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = musicPlayer.isPlaying && currentSong?.id == song.id,
                            queuePosition = index + 1,
                            onClick = {
                                musicPlayer.playSong(song)
                                onBack()
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    onReorder(index, index - 1)
                                }
                            },
                            onMoveDown = {
                                if (index < playlist.size - 1) {
                                    onReorder(index, index + 1)
                                }
                            },
                            canMoveUp = index > 0,
                            canMoveDown = index < playlist.size - 1,
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(durationMillis = 300)
                            )
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    @Composable
    fun QueueSongItem(
        song: Song,
        isCurrentSong: Boolean,
        isPlaying: Boolean,
        queuePosition: Int,
        onClick: () -> Unit,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(
                    if (isCurrentSong)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        Color.Transparent
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Numéro de position
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrentSong && isPlaying) {
                        PlayingIndicator()
                    } else {
                        Text(
                            text = queuePosition.toString(),
                            fontSize = 16.sp,
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentSong)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Infos de la chanson
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrentSong)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isCurrentSong) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = if (isPlaying) "En lecture" else "En pause",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = song.artist,
                            fontSize = 14.sp,
                            color = if (isCurrentSong)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Boutons de réorganisation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(40.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Monter",
                            modifier = Modifier.size(28.dp),
                            tint = if (canMoveUp)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Descendre",
                            modifier = Modifier.size(28.dp),
                            tint = if (canMoveDown)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun PlaylistsScreen(
        playlists: List<Playlist>,
        songs: List<Song>,
        hasPermission: Boolean,
        favorites: List<Long>,
        playlistManager: PlaylistManager,
        statsManager: StatsManager,
        onRequestPermission: () -> Unit,
        onPlaylistClick: (Playlist) -> Unit,
        onCreatePlaylist: (String, List<Long>) -> Unit,
        onToggleFavorite: (Long) -> Unit
    ) {
        var showCreateDialog by remember { mutableStateOf(false) }
        var selectedTabIndex by remember { mutableStateOf(0) }
        val favoriteSongs = remember(songs, favorites) {
            songs.filter { favorites.contains(it.id) }
        }

        Scaffold(topBar = {
            Column {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Favoris") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favoris"
                            )
                        })
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Playlists") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Playlists"
                            )
                        })
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Stats") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Statistiques"
                            )
                        })
                }
            }
        }, floatingActionButton = {
            // Bouton FAB uniquement sur l'onglet Playlists
            if (selectedTabIndex == 1 && hasPermission) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Créer une playlist")
                }
            }
        }) { padding ->
            if (!hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTabIndex == 0) Icons.Default.Favorite else Icons.Default.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Permission nécessaire", fontSize = 20.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Nous avons besoin d'accéder à vos fichiers audio",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onRequestPermission) {
                            Text("Autoriser l'accès")
                        }
                    }
                }
            } else {
                when (selectedTabIndex) {
                    0 -> {
                        // ONGLET FAVORIS
                        if (favoriteSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Aucun favori")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Ajoutez des chansons à vos favoris depuis le lecteur",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                            ) {
                                item {
                                    // En-tête avec le nombre de favoris
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                "Mes favoris",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "${favoriteSongs.size} chanson${if (favoriteSongs.size > 1) "s" else ""}",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }

                                items(favoriteSongs) { song ->
                                    FavoriteSongItem(song = song, onSongClick = { song ->
                                        // Jouer directement la chanson
                                    }, onRemoveFavorite = {
                                        onToggleFavorite(song.id)
                                    })
                                    HorizontalDivider()
                                }
                            }
                        }
                    }

                    1 -> {
                        // ONGLET PLAYLISTS (code existant)
                        if (playlists.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Aucune playlist")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { showCreateDialog = true }) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Créer une playlist")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                            ) {
                                items(playlists) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        songCount = playlist.songIds.size,
                                        onClick = { onPlaylistClick(playlist) })
                                    HorizontalDivider()
                                }
                            }
                        }
                    }

                    2 -> {
                        // ONGLET STATISTIQUES - Remplacer "À venir" par le vrai écran
                        StatsScreen(
                            songs = songs,
                            artists = artists,
                            albums = albums,
                            favorites = favorites,
                            hasPermission = hasPermission,
                            statsManager = statsManager,
                            onRequestPermission = onRequestPermission
                        )
                    }
                }
            }
        }


        if (showCreateDialog) {
            CreatePlaylistDialog(
                songs = songs,
                onDismiss = { showCreateDialog = false },
                onCreate = { name, songIds ->
                    onCreatePlaylist(name, songIds)
                    showCreateDialog = false
                })
        }
    }

    @Composable
    fun StatsScreen(
        songs: List<Song>,
        artists: SnapshotStateList<Artist>,
        albums: SnapshotStateList<Album>,
        favorites: List<Long>,
        hasPermission: Boolean,
        statsManager: StatsManager,
        onRequestPermission: () -> Unit
    ) {
        val context = LocalContext.current


        // Get stats data
        var refreshTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)  // Rafraîchir chaque seconde
                refreshTrigger++
            }
        }
        val statsUpdateTrigger by statsManager.statsUpdated.collectAsState()

        val (totalPlays, totalSongsPlayed, totalListeningTime) = remember(refreshTrigger) {
            statsManager.getTotalStats()
        }
        val topSongs = remember(songs, refreshTrigger) {
            statsManager.getTopSongs(songs, 10)
        }
        val recentlyPlayed = remember(songs, refreshTrigger) {
            statsManager.getRecentlyPlayed(songs, 10)
        }
        val topArtists = remember(songs, refreshTrigger) {
            statsManager.getTopArtists(songs, 10)
        }

        var showClearDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Statistiques") },
                    actions = {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, "Effacer les statistiques")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            if (!hasPermission) {
                PermissionRequiredMessage(
                    icon = Icons.Default.Analytics,
                    title = "vos statistiques",
                    onOpenSettings = { openAppSettings(context) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Global stats card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Vue d'ensemble",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatItem(
                                        icon = Icons.Default.PlayArrow,
                                        value = totalPlays.toString(),
                                        label = "Lectures"
                                    )
                                    StatItem(
                                        icon = Icons.Default.MusicNote,
                                        value = totalSongsPlayed.toString(),
                                        label = "Chansons"
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    StatItem(
                                        icon = Icons.Default.Analytics,
                                        value = statsManager.formatDuration(totalListeningTime),
                                        label = "Temps d'écoute"
                                    )
                                }
                            }
                        }
                    }

                    // Top songs section
                    if (topSongs.isNotEmpty()) {
                        item {
                            Text(
                                "Chansons les plus écoutées",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(topSongs) { playCount ->
                            TopSongItem(playCount = playCount)
                        }
                    }

                    // Recently played section
                    if (recentlyPlayed.isNotEmpty()) {
                        item {
                            Text(
                                "Récemment écoutées",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(recentlyPlayed) { playCount ->
                            RecentSongItem(playCount = playCount)
                        }
                    }

                    // Top artists section
                    if (topArtists.isNotEmpty()) {
                        item {
                            Text(
                                "Artistes les plus écoutés",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(topArtists) { (artist, count) ->
                            TopArtistItem(artistName = artist, playCount = count)
                        }
                    }

                    // Empty state
                    if (totalPlays == 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Aucune statistique",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Écoutez de la musique pour voir vos statistiques",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Clear stats confirmation dialog
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Effacer les statistiques ?") },
                    text = { Text("Cette action supprimera toutes vos statistiques d'écoute. Cette action est irréversible.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                statsManager.clearAllStats()
                                showClearDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Effacer")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun StatItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        value: String,
        label: String
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun TopSongItem(playCount: PlayCount) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playCount.songTitle,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playCount.artistName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${playCount.count}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "lectures",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    fun RecentSongItem(playCount: PlayCount) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playCount.songTitle,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playCount.artistName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatTimeAgo(playCount.lastPlayed),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }


    @Composable
    fun TopArtistItem(artistName: String, playCount: Int) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = artistName,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$playCount",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "lectures",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}j"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}min"
            else -> "maintenant"
        }
    }

    @Composable
    fun FavoriteSongItem(
        song: Song, onSongClick: (Song) -> Unit, onRemoveFavorite: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onSongClick(song) })
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRemoveFavorite
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Retirer des favoris",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    @Composable
    fun PlaylistItem(
        playlist: Playlist, songCount: Int, onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$songCount chansons",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }


    @Composable
    fun PlaylistDetailScreen(
        playlist: Playlist,
        songs: List<Song>,
        allSongs: List<Song>,
        musicPlayer: MusicService,
        onBack: () -> Unit,
        onSongClick: (Song) -> Unit,
        onDeletePlaylist: () -> Unit,
        onUpdatePlaylist: (String, List<Long>) -> Unit
    ) {
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showEditDialog by remember { mutableStateOf(false) }

        val currentSong = musicPlayer.currentSong
        val isPlaying = musicPlayer.isPlaying
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        // Auto-scroll to current song in playlist
        LaunchedEffect(currentSong?.id) {
            currentSong?.let { song ->
                val songIndex = songs.indexOfFirst { it.id == song.id }
                if (songIndex != -1) {
                    scope.launch {
                        listState.animateScrollToItem(songIndex)
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(playlist.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Retour")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, "Modifier la playlist")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Supprimer la playlist")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // En-tête de la playlist
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = playlist.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${songs.size} chansons",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Liste des chansons
                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucune chanson dans cette playlist",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                        items(songs) { song ->
                            SongItem(
                                song = song,
                                onClick = { onSongClick(song) },
                                isPlaying = isPlaying,
                                isCurrentSong = currentSong?.id == song.id
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Supprimer la playlist ?") },
                    text = { Text("Cette action est irréversible.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeletePlaylist()
                                showDeleteDialog = false
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Supprimer")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Annuler")
                        }
                    })
            }

            if (showEditDialog) {
                EditPlaylistDialog(
                    playlist = playlist,
                    allSongs = allSongs,
                    onDismiss = { showEditDialog = false },
                    onUpdate = { newName, newSongIds ->
                        onUpdatePlaylist(newName, newSongIds)
                        showEditDialog = false
                    })
            }
        }
    }

    @Composable
    fun EditPlaylistDialog(
        playlist: Playlist,
        allSongs: List<Song>,
        onDismiss: () -> Unit,
        onUpdate: (String, List<Long>) -> Unit
    ) {
        var playlistName by remember { mutableStateOf(playlist.name) }
        var selectedSongs by remember { mutableStateOf(playlist.songIds.toSet()) }
        var showSongSelector by remember { mutableStateOf(false) }

        AlertDialog(onDismissRequest = onDismiss, title = { Text("Modifier la playlist") }, text = {
            Column {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nom de la playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showSongSelector = true }, modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modifier les chansons (${selectedSongs.size})")
                }
            }
        }, confirmButton = {
            Button(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onUpdate(playlistName, selectedSongs.toList())
                    }
                }, enabled = playlistName.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        }, dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        })

        if (showSongSelector) {
            AlertDialog(
                onDismissRequest = { showSongSelector = false },
                title = { Text("Modifier les chansons") },
                text = {
                    LazyColumn(modifier = Modifier.height(400.dp)) {
                        items(allSongs) { song ->
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSongs = if (selectedSongs.contains(song.id)) {
                                        selectedSongs - song.id
                                    } else {
                                        selectedSongs + song.id
                                    }
                                }
                                .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedSongs.contains(song.id),
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showSongSelector = false }) {
                        Text("OK")
                    }
                })
        }
    }

    @Composable
    fun CreatePlaylistDialog(
        songs: List<Song>, onDismiss: () -> Unit, onCreate: (String, List<Long>) -> Unit
    ) {
        var playlistName by remember { mutableStateOf("") }
        var selectedSongs by remember { mutableStateOf(setOf<Long>()) }
        var showSongSelector by remember { mutableStateOf(false) }

        AlertDialog(onDismissRequest = onDismiss, title = { Text("Nouvelle playlist") }, text = {
            Column {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nom de la playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showSongSelector = true }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sélectionner les chansons (${selectedSongs.size})")
                }
            }
        }, confirmButton = {
            Button(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onCreate(playlistName, selectedSongs.toList())
                    }
                }, enabled = playlistName.isNotBlank()
            ) {
                Text("Créer")
            }
        }, dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        })

        if (showSongSelector) {
            AlertDialog(
                onDismissRequest = { showSongSelector = false },
                title = { Text("Sélectionner des chansons") },
                text = {
                    LazyColumn(modifier = Modifier.height(400.dp)) {
                        items(songs) { song ->
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSongs = if (selectedSongs.contains(song.id)) {
                                        selectedSongs - song.id
                                    } else {
                                        selectedSongs + song.id
                                    }
                                }
                                .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedSongs.contains(song.id),
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showSongSelector = false }) {
                        Text("OK")
                    }
                })
        }
    }


    @Composable
    fun MusicPlayerScreen(
        musicPlayer: MusicService,
        imageService: ArtistImageService,
        favorites: List<Long>,
        onToggleFavorite: (Long) -> Unit,
        onShowLyrics: () -> Unit,
        onShowQueue: () -> Unit,
        onNavigateToArtist: ((String) -> Unit)? = null
    ) {
        val currentSong = musicPlayer.currentSong
        val isPlaying = musicPlayer.isPlaying
        val repeatMode = musicPlayer.repeatMode
        val isShuffleEnabled = musicPlayer.isShuffleEnabled
        val songTitle = currentSong?.title ?: "Aucune chanson"
        val artistName = currentSong?.artist ?: "Sélectionnez une chanson"


        var albumCoverUrl by remember { mutableStateOf<String?>(null) }
        var isLoadingCover by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val isFavorite = currentSong?.let { favorites.contains(it.id) } ?: false
        LaunchedEffect(currentSong?.albumId) {
            if (currentSong != null) {
                isLoadingCover = true
                scope.launch {
                    albumCoverUrl = imageService.getAlbumCoverUrl(
                        currentSong.album, currentSong.artist
                    )
                    isLoadingCover = false
                }
            } else {
                albumCoverUrl = null
            }
        }

        // OPTIMIZED: Update position less frequently and only when needed
        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                musicPlayer.updatePosition()
                delay(1000) // Further reduced to 1s for better performance
            }
        }

        val currentPosition = musicPlayer.currentPosition
        val duration = musicPlayer.duration
        val progress = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()) * 100f
        } else 0f

        Scaffold (contentWindowInsets = WindowInsets(0)
        ){ padding ->
            Box(modifier = Modifier.fillMaxSize()
               ) {
                // Background album art with reduced opacity
//                if (albumCoverUrl != null) {
//                    AsyncImage(
//                        model = ImageRequest.Builder(context).data(albumCoverUrl)
//                            .crossfade(true).build(),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .alpha(0.3f),
//                        contentScale = ContentScale.Crop
//                    )
//                }

                if (albumCoverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(albumCoverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.6f)
                            .blur(radiusX = 20.dp, radiusY = 20.dp),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(36.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (albumCoverUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(albumCoverUrl)
                                        .crossfade(true).build(),
                                    contentDescription = "Pochette de ${currentSong?.album}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (isLoadingCover) {
                                CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Album cover",
                                    modifier = Modifier.size(120.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = songTitle,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = artistName,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(
                            enabled = currentSong != null && artistName != "Sélectionnez une chanson"
                        ) {
                            currentSong?.let { onNavigateToArtist?.invoke(it.artist) }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // ← Ce Spacer doit être un enfant DIRECT de la Column principale
                    Spacer(modifier = Modifier.weight(1f))

                    // Boutons queue / paroles / favori + slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = onShowQueue,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF2A2A2A),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = "Liste de lecture", tint = Color.White)
                            }
                            FilledTonalIconButton(
                                onClick = onShowLyrics,
                                modifier = Modifier.size(40.dp),
                                enabled = currentSong != null,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF2A2A2A),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = "Voir les paroles", tint = Color.White)
                            }
                            FilledTonalIconButton(
                                onClick = { currentSong?.let { onToggleFavorite(it.id) } },
                                modifier = Modifier.size(40.dp),
                                enabled = currentSong != null,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isFavorite) Color(0xFF03A9F4) else Color(0xFF2A2A2A),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                                    tint = Color.White
                                )
                            }
                        }

                        Slider(
                            value = progress,
                            onValueChange = { newProgress ->
                                val newPosition = ((newProgress / 100f) * duration).toLong()
                                musicPlayer.seekTo(newPosition)
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = formatDuration(currentPosition), fontSize = 12.sp)
                            Text(text = formatDuration(duration), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Boutons Repeat / Play / Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { musicPlayer.toggleRepeatMode() },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (repeatMode != RepeatMode.OFF) Color(0xFF03A9F4) else Color(0xFF2A2A2A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    RepeatMode.OFF -> Icons.Default.Repeat
                                    RepeatMode.ONE -> Icons.Default.RepeatOne
                                    RepeatMode.ALL -> Icons.Default.Repeat
                                },
                                contentDescription = "Répétition",
                                tint = Color.White
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = { musicPlayer.playPrevious() },
                                modifier = Modifier.size(64.dp),
                                enabled = musicPlayer.hasPrevious || repeatMode == RepeatMode.ALL,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF2A2A2A),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Précédent", modifier = Modifier.size(32.dp), tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            FloatingActionButton(
                                onClick = { if (isPlaying) musicPlayer.pause() else musicPlayer.resume() },
                                modifier = Modifier.size(72.dp),
                                containerColor = Color(0xFF03A9F4)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            FilledTonalIconButton(
                                onClick = { musicPlayer.playNext() },
                                modifier = Modifier.size(64.dp),
                                enabled = musicPlayer.hasNext || repeatMode == RepeatMode.ALL,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF2A2A2A),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Suivant", modifier = Modifier.size(32.dp), tint = Color.White)
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { musicPlayer.toggleShuffle() },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isShuffleEnabled) Color(0xFF03A9F4) else Color(0xFF2A2A2A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Mode aléatoire",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }


    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }


    @Composable
    fun SongListScreen(
        songs: List<Song>,
        hasPermission: Boolean,
        onRequestPermission: () -> Unit,
        musicPlayer: MusicService,
        onSongClick: (Song) -> Unit,
        selectedTab: Int = 1
    ) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredSongs = remember(songs, searchQuery) {
            if (searchQuery.isBlank()) songs
            else songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val alphabet = remember { generateAlphabet() }

        // Group songs by first letter
        val groupedSongs = remember(filteredSongs) {
            filteredSongs.groupByFirstLetter { it.title }
        }

        // Pre-calculate letter indices for fast scrolling
        val letterIndexMap = remember(groupedSongs) {
            val map = mutableMapOf<String, Int>()
            var currentIndex = 0
            groupedSongs.forEach { (letter, songsInGroup) ->
                map[letter] = currentIndex
                currentIndex += songsInGroup.size + 1 // +1 for the header
            }
            map
        }

        val currentSong = musicPlayer.currentSong
        val isPlaying = musicPlayer.isPlaying
        val context = LocalContext.current

        // Auto-scroll to current song when entering songs tab
        LaunchedEffect(currentSong?.id, selectedTab) {
            if (currentSong != null && selectedTab == 1) { // Only when songs tab is active
                val songIndex = filteredSongs.indexOfFirst { it.id == currentSong.id }
                if (songIndex != -1) {
                    // Calculate actual index in the LazyColumn (including headers)
                    var actualIndex = 0
                    var songsProcessed = 0

                    for ((letter, songsInGroup) in groupedSongs) {
                        // Ajouter 1 pour l'en-tête de section
                        actualIndex++

                        // Vérifier si la chanson est dans ce groupe
                        if (songIndex < songsProcessed + songsInGroup.size) {
                            // La chanson est dans ce groupe
                            val positionInGroup = songIndex - songsProcessed
                            actualIndex += positionInGroup
                            break
                        }

                        // Ajouter toutes les chansons de ce groupe
                        actualIndex += songsInGroup.size
                        songsProcessed += songsInGroup.size
                    }

                    scope.launch {
                        listState.animateScrollToItem(actualIndex)
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                if (hasPermission && songs.isNotEmpty()) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Rechercher une chanson...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Effacer"
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        ) { padding ->
            if (!hasPermission) {
                PermissionRequiredMessage(
                    icon = Icons.Default.MusicNote,
                    title = "vos chansons",
                    onOpenSettings = { openAppSettings(context) }
                )
            } else if (songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aucune chanson trouvée")
                    }
                }
            } else if (filteredSongs.isEmpty() && searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aucun résultat pour \"$searchQuery\"")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groupedSongs.forEach { (letter, songsInGroup) ->
                            item(key = "header_$letter") {
                                Text(
                                    text = letter,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            items(
                                count = songsInGroup.size,
                                key = { index -> songsInGroup[index].id }
                            ) { index ->
                                val song = songsInGroup[index]
                                SongItem(
                                    song = song,
                                    onClick = { onSongClick(song) },
                                    isPlaying = isPlaying,
                                    isCurrentSong = currentSong?.id == song.id
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    // ✅ FASTSCROLLER OPTIMISÉ AVEC SCROLL INSTANTANÉ
                    FastScroller(
                        alphabet = alphabet,
                        onLetterSelected = { letter ->
                            scope.launch {
                                // Utiliser la map pré-calculée pour un scroll instantané
                                val targetIndex = letterIndexMap[letter]
                                if (targetIndex != null) {
                                    // scrollToItem au lieu de animateScrollToItem = BEAUCOUP plus rapide
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 16.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun AlbumsScreen(
        albums: List<Album>,
        hasPermission: Boolean,
        imageService: ArtistImageService,
        onRequestPermission: () -> Unit,
        onAlbumClick: (Album) -> Unit
    ) {
        val context = LocalContext.current
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val alphabet = remember { generateAlphabet() }

        // Group albums by first letter
        val groupedAlbums = remember(albums) {
            albums.groupByFirstLetter { it.name }
        }

        // Pre-calculate letter indices for fast scrolling
        val letterIndexMap = remember(groupedAlbums) {
            val map = mutableMapOf<String, Int>()
            var currentIndex = 0
            groupedAlbums.forEach { (letter, albumsInGroup) ->
                map[letter] = currentIndex
                currentIndex += albumsInGroup.size + 1
            }
            map
        }



        Scaffold { padding ->
            if (!hasPermission) {
                PermissionRequiredMessage(
                    icon = Icons.Default.Album,
                    title = "vos albums",
                    onOpenSettings = { openAppSettings(context) }
                )
            } else if (albums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aucun album trouvé")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groupedAlbums.forEach { (letter, albumsInGroup) ->
                            item(key = "header_$letter") {
                                Text(
                                    text = letter,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 8.dp)
                                )
                            }

                            items(
                                count = albumsInGroup.size,
                                key = { index -> albumsInGroup[index].id }
                            ) { index ->
                                AlbumItem(
                                    album = albumsInGroup[index],
                                    onClick = { onAlbumClick(albumsInGroup[index]) },
                                    imageService = imageService
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    // ✅ SCROLL INSTANTANÉ
                    FastScroller(
                        alphabet = alphabet,
                        onLetterSelected = { letter ->
                            scope.launch {
                                letterIndexMap[letter]?.let { listState.scrollToItem(it) }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 21.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun SongItem(
        song: Song,
        onClick: () -> Unit,
        isPlaying: Boolean = false,
        isCurrentSong: Boolean = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(
                    if (isCurrentSong)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        Color.Transparent
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône ou animation de lecture
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentSong && isPlaying) {
                    // Animation de barres audio pour la chanson en cours
                    PlayingIndicator()
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrentSong)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentSong)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge "En lecture" si c'est la chanson actuelle
                    if (isCurrentSong) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                        ) {
                            Text(
                                text = if (isPlaying) "En lecture" else "En pause",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = song.artist,
                        fontSize = 14.sp,
                        color = if (isCurrentSong)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

//        Text(
//            text = formatDuration(song.duration),
//            fontSize = 14.sp,
//            color = if (isCurrentSong)
//                MaterialTheme.colorScheme.primary
//            else
//                MaterialTheme.colorScheme.onSurfaceVariant
//        )
        }
    }

    @Composable
    fun PlayingIndicator() {
        // Animation de 3 barres audio
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(24.dp)
        ) {
            repeat(3) { index ->
                var height by remember { mutableStateOf(0.3f) }

                LaunchedEffect(Unit) {
                    while (true) {
                        // Animation aléatoire pour chaque barre
                        animate(
                            initialValue = height,
                            targetValue = Random.nextFloat() * 0.7f + 0.3f,
                            animationSpec = tween(
                                durationMillis = Random.nextInt(300, 600),
                                easing = LinearEasing
                            )
                        ) { value, _ ->
                            height = value
                        }
                        delay(Random.nextLong(100, 200))
                    }
                }

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp * height)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }

    @Composable
    fun AlbumItem(
        album: Album, onClick: () -> Unit, imageService: ArtistImageService
    ) {
        var albumCoverUrl by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(album.id) {
            scope.launch {
                albumCoverUrl = imageService.getAlbumCoverUrl(album.name, album.artist)
                isLoading = false
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    if (albumCoverUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(albumCoverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Pochette de ${album.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

//        Text(
//           text = "${album.songCount} chansons",
//            fontSize = 14.sp,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
        }
    }

    @Composable
    fun ArtistsScreen(
        artists: List<Artist>,
        hasPermission: Boolean,
        imageService: ArtistImageService,
        onRequestPermission: () -> Unit,
        onArtistClick: (Artist) -> Unit
    ) {
        val context = LocalContext.current
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val alphabet = remember { generateAlphabet() }

        val groupedArtists = remember(artists) {
            artists.groupByFirstLetter { it.name }
        }

        // ✅ PRÉ-CALCUL DES INDEX
        val letterIndexMap = remember(groupedArtists) {
            val map = mutableMapOf<String, Int>()
            var currentIndex = 0
            groupedArtists.forEach { (letter, _) ->
                map[letter] = currentIndex
                currentIndex += 2 // header + grid
            }
            map
        }

        Scaffold(topBar = {}) { padding ->
            if (!hasPermission) {
                PermissionRequiredMessage(
                    icon = Icons.Default.Person,
                    title = "vos artistes",
                    onOpenSettings = { openAppSettings(context) }
                )
            } else if (artists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aucun artiste trouvé")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groupedArtists.forEach { (letter, artistsInGroup) ->
                            item(key = "header_$letter") {
                                Text(
                                    text = letter,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }

                            item(key = "grid_$letter") {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    artistsInGroup.chunked(3).forEach { rowArtists ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            rowArtists.forEach { artist ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    ArtistGridItem(
                                                        artist = artist,
                                                        onClick = { onArtistClick(artist) },
                                                        imageService = imageService
                                                    )
                                                }
                                            }
                                            repeat(3 - rowArtists.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ✅ SCROLL INSTANTANÉ
                    FastScroller(
                        alphabet = alphabet,
                        onLetterSelected = { letter ->
                            scope.launch {
                                letterIndexMap[letter]?.let { listState.scrollToItem(it) }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 16.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun ArtistGridItem(
        artist: Artist, onClick: () -> Unit, imageService: ArtistImageService
    ) {
        var imageUrl by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(artist.name) {
            scope.launch {
                imageUrl = imageService.getArtistImageUrl(artist.name)
                isLoading = false
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Card(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(55.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(imageUrl).crossfade(true)
                                .build(),
                            contentDescription = "Photo de ${artist.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp), strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = artist.name,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun ArtistItem(
        artist: Artist, onClick: () -> Unit, imageService: ArtistImageService
    ) {
        var imageUrl by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(artist.name) {
            scope.launch {
                imageUrl = imageService.getArtistImageUrl(artist.name)
                isLoading = false
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(imageUrl).crossfade(true)
                                .build(),
                            contentDescription = "Photo de ${artist.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${artist.albumCount} albums • ${artist.songCount} chansons",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
// =====================================================================
// REMPLACEZ la fonction AlbumDetailScreen dans MainActivity.kt
// par ce code complet. ArtistSongRow étant déjà défini, on réutilise
// un AlbumSongRow similaire.
// =====================================================================

    @Composable
    fun AlbumDetailScreen(
        album: Album,
        songs: List<Song>,
        imageService: ArtistImageService,
        musicPlayer: MusicService,
        onBack: () -> Unit,
        onSongClick: (Song) -> Unit
    ) {
        val currentSong = musicPlayer.currentSong
        val isPlaying = musicPlayer.isPlaying
        var albumCoverUrl by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val listState = rememberLazyListState()

        val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
        val isHeaderVisible = firstVisibleItemIndex == 0

        LaunchedEffect(album.id) {
            scope.launch {
                albumCoverUrl = imageService.getAlbumCoverUrl(album.name, album.artist)
                isLoading = false
            }
        }

        // Auto-scroll to current song
        LaunchedEffect(currentSong?.id) {
            currentSong?.let { song ->
                val songIndex = songs.indexOfFirst { it.id == song.id }
                if (songIndex != -1) {
                    scope.launch {
                        listState.animateScrollToItem(songIndex + 2)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── HERO avec pochette en CERCLE ─────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                    ) {
                        // Fond flouté / ambiance derrière le cercle
                        if (albumCoverUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(albumCoverUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.15f),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Dégradé de bas vers surface (fondu)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.6f to Color.Transparent,
                                            1.0f to MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        )

                        // Dégradé haut pour bouton retour
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.25f),
                                            0.2f to Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Contenu centré : cercle + infos
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 64.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // ── POCHETTE EN CERCLE ──
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (albumCoverUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(albumCoverUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Pochette de ${album.name}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Nom de l'album
                            Text(
                                text = album.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.3).sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Artiste + nb de chansons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = album.artist,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                )

                                Text(
                                    text = "${songs.size} chanson${if (songs.size > 1) "s" else ""}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── BOUTONS LECTURE / ALÉATOIRE ──────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    musicPlayer.setPlaylist(songs)
                                    musicPlayer.playSong(songs.first())
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Lecture",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    musicPlayer.setPlaylist(songs)
                                    musicPlayer.toggleShuffle()
                                    musicPlayer.playSong(songs.random())
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Aléatoire",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }

                // ── LISTE DES CHANSONS ───────────────────────────────────
                if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Aucune chanson dans cet album",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        AlbumSongRow(
                            song = song,
                            index = index + 1,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            onClick = { onSongClick(song) }
                        )
                        if (index < songs.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // ── TOP BAR FLOTTANTE ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isHeaderVisible) 56.dp else 64.dp)
                    .background(
                        if (isHeaderVisible) Color.Transparent
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
            ) {
                if (!isHeaderVisible) {
                    Text(
                        text = album.name,
                        modifier = Modifier.align(Alignment.Center),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isHeaderVisible)
                            Color.Black.copy(alpha = 0.35f)
                        else
                            Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = if (isHeaderVisible) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Ligne de chanson pour la page album ────────────────────────────
    @Composable
    private fun AlbumSongRow(
        song: Song,
        index: Int,
        isCurrentSong: Boolean,
        isPlaying: Boolean,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isCurrentSong)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else Color.Transparent
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numéro ou indicateur de lecture
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentSong && isPlaying) {
                    PlayingIndicator()
                } else {
                    Text(
                        text = index.toString(),
                        fontSize = 15.sp,
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentSong)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Titre
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = if (isCurrentSong) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrentSong)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Badge lecture
            if (isCurrentSong) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isPlaying) "▶" else "⏸",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun ArtistDetailScreen(
        artist: Artist,
        songs: List<Song>,
        imageService: ArtistImageService,
        musicPlayer: MusicService,
        onBack: () -> Unit,
        onSongClick: (Song) -> Unit
    ) {
        val currentSong = musicPlayer.currentSong
        val isPlaying = musicPlayer.isPlaying
        var artistImageUrl by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val listState = rememberLazyListState()

        // Détecter si le header est visible pour l'effet de la TopBar
        val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
        val isHeaderVisible = firstVisibleItemIndex == 0

        LaunchedEffect(artist.name) {
            scope.launch {
                artistImageUrl = imageService.getArtistImageUrl(artist.name)
                isLoading = false
            }
        }

        // Auto-scroll to current song
        LaunchedEffect(currentSong?.id) {
            currentSong?.let { song ->
                val songIndex = songs.indexOfFirst { it.id == song.id }
                if (songIndex != -1) {
                    scope.launch {
                        // +2 pour compter le header hero dans la LazyColumn
                        listState.animateScrollToItem(songIndex + 2)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── HERO IMAGE avec dégradé ──────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        // Image de fond
                        if (artistImageUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(artistImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Photo de ${artist.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            // Fallback : dégradé avec initiale
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artist.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    fontSize = 96.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }

                        // Dégradé du bas (noir vers transparent) — effet Apple Music
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.45f to Color.Transparent,
                                            1.0f to MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        )

                        // Dégradé du haut (pour la lisibilité du bouton retour)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.35f),
                                            0.25f to Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Nom + stats en bas de l'image
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                        ) {
                            Text(
                                text = artist.name,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-0.5).sp,
                                lineHeight = 38.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Badge Albums
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Album,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color.White.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = "${artist.albumCount} album${if (artist.albumCount > 1) "s" else ""}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                // Badge Chansons
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color.White.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = "${songs.size} chanson${if (songs.size > 1) "s" else ""}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── BOUTON "TOUT JOUER" ──────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bouton Lecture
                        Button(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    musicPlayer.setPlaylist(songs)
                                    musicPlayer.playSong(songs.first())
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF03A9F4)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Lecture",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        // Bouton Aléatoire
                        OutlinedButton(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    musicPlayer.setPlaylist(songs)
                                    musicPlayer.toggleShuffle()
                                    musicPlayer.playSong(songs.random())
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Color(0xFF03A9F4)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF03A9F4)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Aléatoire",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF03A9F4)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }

                // ── LISTE DES CHANSONS ───────────────────────────────────
                if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Aucune chanson pour cet artiste",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        ArtistSongRow(
                            song = song,
                            index = index + 1,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            onClick = { onSongClick(song) }
                        )
                        if (index < songs.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }

                    // Espace en bas
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // ── TOP BAR FLOTTANTE ────────────────────────────────────────
            // Transparente sur le hero, opaque en scrollant
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isHeaderVisible) 56.dp else 64.dp)
                    .background(
                        if (isHeaderVisible) Color.Transparent
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
            ) {
                // Titre centré (visible uniquement quand le hero est hors écran)
                if (!isHeaderVisible) {
                    Text(
                        text = artist.name,
                        modifier = Modifier.align(Alignment.Center),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bouton retour toujours visible
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isHeaderVisible)
                            Color.Black.copy(alpha = 0.35f)
                        else
                            Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = if (isHeaderVisible) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Ligne de chanson spécifique à la page artiste ──────────────────
    @Composable
    private fun ArtistSongRow(
        song: Song,
        index: Int,
        isCurrentSong: Boolean,
        isPlaying: Boolean,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isCurrentSong)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else Color.Transparent
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numéro de piste ou indicateur de lecture
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentSong && isPlaying) {
                    PlayingIndicator()
                } else {
                    Text(
                        text = index.toString(),
                        fontSize = 15.sp,
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentSong)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Titre + artiste
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = if (isCurrentSong) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrentSong)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.album.isNotBlank() && song.album != "Album inconnu") {
                    Text(
                        text = song.album,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Badge "En lecture"
            if (isCurrentSong) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isPlaying) "▶" else "⏸",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun FoldersScreen(
        folders: List<MusicFolder>,
        hasPermission: Boolean,
        onRequestPermission: () -> Unit,
        onFolderClick: (MusicFolder) -> Unit
    ) {
        Scaffold(

        ) { padding ->
            if (!hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Permission nécessaire", fontSize = 20.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Nous avons besoin d'accéder à vos fichiers audio",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onRequestPermission) {
                            Text("Autoriser l'accès")
                        }
                    }
                }
            } else if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aucun dossier trouvé")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(folders) { folder ->
                        FolderItem(
                            folder = folder, onClick = { onFolderClick(folder) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    @Composable
    fun FolderItem(folder: MusicFolder, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.songCount} chansons",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    fun FolderDetailScreen(
        folder: MusicFolder, songs: List<Song>, onBack: () -> Unit, onSongClick: (Song) -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(folder.name) }, navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Retour")
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // En-tête du dossier
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = folder.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${songs.size} chansons",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Liste des chansons
                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucune chanson dans ce dossier",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(songs) { song ->
                            SongItem(song = song, onClick = { onSongClick(song) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}