@file:OptIn(ExperimentalMaterial3Api::class)
package fr.kevw.kenemimusic

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import java.io.File
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.text.style.TextAlign

// ===== PERSONNALISATION DES COULEURS =====
@Composable
fun customColorScheme() = darkColorScheme(
    primary = Color(0xFF7A7A7A),
    primaryContainer = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF020202),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xC2C2C2FF),
    onSurfaceVariant = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF000000)
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
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val artUri: Uri?
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int
)

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long = System.currentTimeMillis()
)

data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int
)

enum class RepeatMode {
    OFF, ONE, ALL
}

class MainActivity : ComponentActivity() {
    private val songs = mutableStateListOf<Song>()
    private val albums = mutableStateListOf<Album>()
    private val artists = mutableStateListOf<Artist>()
    private val playlists = mutableStateListOf<Playlist>()
    private val folders = mutableStateListOf<MusicFolder>()
    private var hasPermission by mutableStateOf(false)

    private var musicService: MusicService? by mutableStateOf(null)
    private var serviceBound = false

    // AJOUTÉ : Gestionnaire de playlists persistantes
    private lateinit var playlistManager: PlaylistManager

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            musicService = null
        }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            loadSongs()
            loadAlbums()
            loadArtists()
            loadFolders()
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission notification accordée ou refusée
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AJOUTÉ : Initialiser le gestionnaire de playlists
        playlistManager = PlaylistManager(this)

        // AJOUTÉ : Charger les playlists sauvegardées
        playlists.addAll(playlistManager.loadPlaylists())

        // Démarrer et lier le service de musique
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        checkPermission()

        // Demander la permission de notification sur Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val player = musicService
            val imageService = remember { ArtistImageService(this@MainActivity) }

            MaterialTheme(colorScheme = customColorScheme()) {
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
                        hasPermission = hasPermission,
                        imageService = imageService,
                        musicPlayer = player,
                        onRequestPermission = { requestPermission.launch(getPermissionString()) },
                        // MODIFIÉ : Sauvegarder lors de la création
                        onCreatePlaylist = { name, songIds ->
                            val newPlaylist = Playlist(
                                id = System.currentTimeMillis().toString(),
                                name = name,
                                songIds = songIds
                            )
                            playlists.add(newPlaylist)
                            playlistManager.addPlaylist(newPlaylist)
                        },
                        // MODIFIÉ : Sauvegarder lors de la suppression
                        onDeletePlaylist = { playlist ->
                            playlists.remove(playlist)
                            playlistManager.deletePlaylist(playlist.id)
                        },
                        // MODIFIÉ : Sauvegarder lors de la mise à jour
                        onUpdatePlaylist = { playlistId, newName, newSongIds ->
                            val index = playlists.indexOfFirst { it.id == playlistId }
                            if (index != -1) {
                                playlists[index] = playlists[index].copy(
                                    name = newName,
                                    songIds = newSongIds
                                )
                                playlistManager.updatePlaylist(playlistId, newName, newSongIds)
                            }
                        }
                    )
                }
            }
        }
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

        if (hasPermission) {
            loadSongs()
            loadAlbums()
            loadArtists()
            loadFolders()
        }
    }

    private fun loadSongs() {
        val songList = mutableListOf<Song>()

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
            MediaStore.Audio.Media.ALBUM
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val album = cursor.getString(albumColumn)

                // VÉRIFICATION 1 : Filtrer les fichiers avec durée invalide
                if (duration <= 0) {
                    continue // Sauter ce fichier
                }

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // VÉRIFICATION 2 : Vérifier si le fichier est accessible
                try {
                    contentResolver.openInputStream(uri)?.close()
                } catch (e: Exception) {
                    continue // Fichier inaccessible, on le saute
                }

                // Si on arrive ici, le fichier est valide, on l'ajoute
                songList.add(
                    Song(
                        id,
                        title,
                        artist ?: "Artiste inconnu",
                        duration,
                        uri,
                        albumId,
                        album ?: "Album inconnu"
                    )
                )
            }
        }

        songs.clear()
        songs.addAll(songList)
        loadFolders()
    }

    private fun loadAlbums() {
        val albumMap = mutableMapOf<Long, Album>()

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
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistColumn)
                val count = cursor.getInt(countColumn)

                val artUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    id
                )

                albumMap[id] = Album(id, album ?: "Album inconnu", artist ?: "Artiste inconnu", count, artUri)
            }
        }

        albums.clear()
        albums.addAll(albumMap.values)
    }

    private fun loadArtists() {
        val artistList = mutableListOf<Artist>()

        val collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Artists.ARTIST} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val artist = cursor.getString(artistColumn)
                val albumCount = cursor.getInt(albumCountColumn)
                val trackCount = cursor.getInt(trackCountColumn)

                artistList.add(Artist(id, artist ?: "Artiste inconnu", albumCount, trackCount))
            }
        }

        artists.clear()
        artists.addAll(artistList)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun loadFolders() {
        val folderMap = mutableMapOf<String, MutableList<Song>>()

        // Grouper les chansons par dossier
        songs.forEach { song ->
            val path = song.uri.path ?: return@forEach
            val folderPath = File(path).parent ?: return@forEach
            val folderName = File(folderPath).name

            if (!folderMap.containsKey(folderPath)) {
                folderMap[folderPath] = mutableListOf()
            }
            folderMap[folderPath]?.add(song)
        }

        // Créer la liste des dossiers
        val folderList = folderMap.map { (path, songs) ->
            MusicFolder(
                path = path,
                name = File(path).name,
                songCount = songs.size
            )
        }.sortedBy { it.name }

        folders.clear()
        folders.addAll(folderList)
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
        onRequestPermission: () -> Unit,
        onCreatePlaylist: (String, List<Long>) -> Unit,
        onDeletePlaylist: (Playlist) -> Unit,
        onUpdatePlaylist: (String, String, List<Long>) -> Unit
    ) {
        var selectedTab by remember { mutableStateOf(0) }
        var selectedArtist by remember { mutableStateOf<Artist?>(null) }
        var selectedAlbum by remember { mutableStateOf<Album?>(null) }
        var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
        var selectedFolder by remember { mutableStateOf<MusicFolder?>(null) }


        LaunchedEffect(selectedTab) {
            if (selectedTab == 2) selectedAlbum = null
            if (selectedTab == 3) selectedArtist = null
            if (selectedTab == 4) selectedPlaylist = null
            if (selectedTab == 5) selectedFolder = null
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF000000).copy(alpha = 0.85f),
                    modifier = Modifier.height(40.dp)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PlayArrow, "Lecteur", Modifier.size(26.dp)) },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MusicNote, "Chansons", Modifier.size(26.dp)) },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Album, "Albums", Modifier.size(26.dp)) },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, "Artistes", Modifier.size(26.dp)) },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.QueueMusic, "Playlists", Modifier.size(26.dp)) },
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Folder, "Dossiers", Modifier.size(26.dp)) },
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> MusicPlayerScreen(musicPlayer = musicPlayer)
                    1 -> SongListScreen(
                        songs = songs,
                        hasPermission = hasPermission,
                        onRequestPermission = onRequestPermission,
                        onSongClick = { song ->
                            musicPlayer.setPlaylist(songs)
                            musicPlayer.playSong(song)
                            selectedTab = 0
                        }
                    )
                    2 -> {
                        if (selectedAlbum != null) {
                            val albumSongs = songs.filter { it.albumId == selectedAlbum!!.id }
                            AlbumDetailScreen(
                                album = selectedAlbum!!,
                                songs = albumSongs,
                                onBack = { selectedAlbum = null },
                                imageService = imageService,
                                onSongClick = { song ->
                                    musicPlayer.setPlaylist(albumSongs)
                                    musicPlayer.playSong(song)
                                    selectedTab = 0
                                }
                            )
                        } else {
                            AlbumsScreen(
                                albums = albums,
                                hasPermission = hasPermission,
                                onRequestPermission = onRequestPermission,
                                onAlbumClick = { album -> selectedAlbum = album }
                            )
                        }
                    }
                    3 -> {


                        if (selectedArtist != null) {
                            val artistSongs = songs.filter { it.artist == selectedArtist!!.name }
                            val imageService = remember { ArtistImageService(this@MainActivity) }
                            ArtistDetailScreen(
                                artist = selectedArtist!!,
                                songs = artistSongs,
                                imageService = imageService,
                                onBack = { selectedArtist = null },
                                onSongClick = { song ->
                                    musicPlayer.setPlaylist(artistSongs)
                                    musicPlayer.playSong(song)
                                    selectedTab = 0
                                }
                            )
                        } else {
                            ArtistsScreen(
                                artists = artists,
                                hasPermission = hasPermission,
                                imageService = imageService,
                                onRequestPermission = onRequestPermission,
                                onArtistClick = { artist -> selectedArtist = artist }
                            )
                        }
                    }

                    4 -> {
                        if (selectedPlaylist != null) {
                            val playlistSongs = songs.filter { song -> selectedPlaylist!!.songIds.contains(song.id) }
                            PlaylistDetailScreen(
                                playlist = selectedPlaylist!!,
                                songs = playlistSongs,
                                allSongs = songs,
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
                                    selectedPlaylist = selectedPlaylist!!.copy(name = newName, songIds = newSongIds)
                                }
                            )
                        } else {
                            PlaylistsScreen(
                                playlists = playlists,
                                songs = songs,
                                hasPermission = hasPermission,
                                onRequestPermission = onRequestPermission,
                                onPlaylistClick = { playlist -> selectedPlaylist = playlist },
                                onCreatePlaylist = onCreatePlaylist
                            )
                        }
                    }
                    5 -> {
                        if (selectedFolder != null) {
                            val folderSongs = songs.filter { song ->
                                val path = song.uri.path ?: ""
                                File(path).parent == selectedFolder!!.path
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
                }
            }
        }
    }
    @Composable
    fun PlaylistsScreen(
        playlists: List<Playlist>,
        songs: List<Song>,
        hasPermission: Boolean,
        onRequestPermission: () -> Unit,
        onPlaylistClick: (Playlist) -> Unit,
        onCreatePlaylist: (String, List<Long>) -> Unit
    ) {
        var showCreateDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {

            }
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
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Permission nécessaire",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
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
            } else if (playlists.isEmpty()) {
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
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
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
                            onClick = { onPlaylistClick(playlist) }
                        )
                        HorizontalDivider()
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
                }
            )
        }
    }

    @Composable
    fun PlaylistItem(
        playlist: Playlist,
        songCount: Int,
        onClick: () -> Unit
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
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
        onBack: () -> Unit,
        onSongClick: (Song) -> Unit,
        onDeletePlaylist: () -> Unit,
        onUpdatePlaylist: (String, List<Long>) -> Unit
    ) {
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showEditDialog by remember { mutableStateOf(false) }

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
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucune chanson dans cette playlist",
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
                        },
                        colors = ButtonDefaults.buttonColors(
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
                }
            )
        }

        if (showEditDialog) {
            EditPlaylistDialog(
                playlist = playlist,
                allSongs = allSongs,
                onDismiss = { showEditDialog = false },
                onUpdate = { newName, newSongIds ->
                    onUpdatePlaylist(newName, newSongIds)
                    showEditDialog = false
                }
            )
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

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Modifier la playlist") },
            text = {
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
                        onClick = { showSongSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modifier les chansons (${selectedSongs.size})")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onUpdate(playlistName, selectedSongs.toList())
                        }
                    },
                    enabled = playlistName.isNotBlank()
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        )

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
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                }
            )
        }
    }

    @Composable
    fun CreatePlaylistDialog(
        songs: List<Song>,
        onDismiss: () -> Unit,
        onCreate: (String, List<Long>) -> Unit
    ) {
        var playlistName by remember { mutableStateOf("") }
        var selectedSongs by remember { mutableStateOf(setOf<Long>()) }
        var showSongSelector by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Nouvelle playlist") },
            text = {
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
                        onClick = { showSongSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sélectionner les chansons (${selectedSongs.size})")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreate(playlistName, selectedSongs.toList())
                        }
                    },
                    enabled = playlistName.isNotBlank()
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        )

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
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                }
            )
        }
    }


    @Composable
    fun MusicPlayerScreen(
        musicPlayer: MusicService
    ) {
        val currentSong = musicPlayer.currentSong
        val isPlaying = musicPlayer.isPlaying
        val repeatMode = musicPlayer.repeatMode
        val isShuffleEnabled = musicPlayer.isShuffleEnabled
        val songTitle = currentSong?.title ?: "Aucune chanson"
        val artistName = currentSong?.artist ?: "Sélectionnez une chanson"

        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                musicPlayer.updatePosition()
                delay(100)
            }
        }

        val currentPosition = musicPlayer.currentPosition
        val duration = musicPlayer.duration
        val progress = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()) * 100f
        } else 0f

        Scaffold(

        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    modifier = Modifier
                        .size(280.dp)//icon musique
                        .clip(RoundedCornerShape(36.dp)),//borderround
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)

                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Album cover",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = songTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = artistName,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
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

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // --- REPEAT (gauche) ---
                    FilledTonalIconButton(
                        onClick = { musicPlayer.toggleRepeatMode() },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (repeatMode != RepeatMode.OFF)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.OFF -> Icons.Default.Repeat
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                RepeatMode.ALL -> Icons.Default.Repeat
                            },
                            contentDescription = when (repeatMode) {
                                RepeatMode.OFF -> "Répétition désactivée"
                                RepeatMode.ONE -> "Répéter cette chanson"
                                RepeatMode.ALL -> "Répéter toute la playlist"
                            },
                            tint = if (repeatMode != RepeatMode.OFF)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }


                    // --- CONTROLS AU CENTRE ---
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { musicPlayer.playPrevious() },
                            modifier = Modifier.size(64.dp),
                            enabled = musicPlayer.hasPrevious || repeatMode == RepeatMode.ALL
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Précédent", modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        FloatingActionButton(
                            onClick = {
                                if (isPlaying) musicPlayer.pause() else musicPlayer.resume()
                            },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        FilledTonalIconButton(
                            onClick = { musicPlayer.playNext() },
                            modifier = Modifier.size(64.dp),
                            enabled = musicPlayer.hasNext || repeatMode == RepeatMode.ALL
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Suivant", modifier = Modifier.size(32.dp))
                        }
                    }


                    // --- SHUFFLE (droite) ---
                    FilledTonalIconButton(
                        onClick = { musicPlayer.toggleShuffle() },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isShuffleEnabled)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = if (isShuffleEnabled)
                                "Mode aléatoire activé"
                            else
                                "Mode aléatoire désactivé",
                            tint = if (isShuffleEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }




            }
        }
    }

    @Composable
    fun SongListScreen(
        songs: List<Song>,
        hasPermission: Boolean,
        onRequestPermission: () -> Unit,
        onSongClick: (Song) -> Unit
    ) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredSongs = remember(songs, searchQuery) {
            if (searchQuery.isBlank()) {
                songs
            } else {
                songs.filter { song ->
                    song.title.contains(searchQuery, ignoreCase = true) ||
                            song.artist.contains(searchQuery, ignoreCase = true) ||
                            song.album.contains(searchQuery, ignoreCase = true)
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
                                            Icon(Icons.Default.Close, contentDescription = "Effacer")
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
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Permission nécessaire",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
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
            } else {
                if (filteredSongs.isEmpty() && searchQuery.isNotEmpty()) {
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        items(filteredSongs) { song ->
                            SongItem(
                                song = song,
                                onClick = { onSongClick(song) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AlbumsScreen(
        albums: List<Album>,
        hasPermission: Boolean,
        onRequestPermission: () -> Unit,
        onAlbumClick: (Album) -> Unit
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
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Permission nécessaire",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(albums) { album ->
                        AlbumItem(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    @Composable
    fun SongItem(song: Song, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
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

            Text(
                text = formatDuration(song.duration),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun AlbumItem(album: Album, onClick: () -> Unit) {
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
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

            Text(
                text = "${album.songCount} chansons",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    @Composable
    fun ArtistsScreen(
        artists: List<Artist>,
        hasPermission: Boolean,
        imageService: ArtistImageService,
        onRequestPermission: () -> Unit,
        onArtistClick: (Artist) -> Unit
    ) {
        Scaffold(
            topBar = { }
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Permission nécessaire",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
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
                // GRILLE D'ARTISTES
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(artists.size) { index ->
                        ArtistGridItem(
                            artist = artists[index],
                            onClick = { onArtistClick(artists[index]) },
                            imageService = imageService
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ArtistGridItem(
        artist: Artist,
        onClick: () -> Unit,
        imageService: ArtistImageService
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Photo de ${artist.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
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
        artist: Artist,
        onClick: () -> Unit,
        imageService: ArtistImageService
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Photo de ${artist.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
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

    @Composable
    fun AlbumDetailScreen(
        album: Album,
        songs: List<Song>,
        imageService: ArtistImageService,
        onBack: () -> Unit,
        onSongClick: (Song) -> Unit
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(album.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Retour")
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
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = album.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = album.artist,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${songs.size} chansons",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucune chanson dans cet album",
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
    @Composable
    fun ArtistDetailScreen(
        artist: Artist,
        songs: List<Song>,
        imageService: ArtistImageService,
        onBack: () -> Unit,
        onSongClick: (Song) -> Unit
    ) {
        var artistImageUrl by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(artist.name) {
            scope.launch {
                artistImageUrl = imageService.getArtistImageUrl(artist.name)
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(artist.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Retour")
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
                                .clip(RoundedCornerShape(40.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
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
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
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

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = artist.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${artist.albumCount} albums",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${songs.size} chansons",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucune chanson pour cet artiste",
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
                            "Permission nécessaire",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
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
                            folder = folder,
                            onClick = { onFolderClick(folder) }
                        )
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
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
        folder: MusicFolder,
        songs: List<Song>,
        onBack: () -> Unit,
        onSongClick: (Song) -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(folder.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Retour")
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
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
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