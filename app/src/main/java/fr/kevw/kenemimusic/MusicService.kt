package fr.kevw.kenemimusic
//commit
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

class MusicService : Service() {
    private val binder = MusicBinder()
    private lateinit var settingsManager: SettingsManager

    private fun updateWidget() {
        try {
            val progress = if (duration > 0) {
                ((currentPosition.toFloat() / duration.toFloat()) * 100).toInt()
            } else 0

            MusicWidgetProvider.updateWidget(
                context = this,
                song = currentSong,
                isPlaying = isPlaying,
                progress = progress,
                albumArtUrl = currentAlbumArtUrl
            )
            MusicWidgetSmallProvider.updateWidget(
                context = this,
                song = currentSong,
                isPlaying = isPlaying,
                progress = progress,
                albumArtUrl = currentAlbumArtUrl
            )
        } catch (e: Exception) {
            Log.e("MusicService", "Error updating widget", e)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    var currentSong: Song? = null
    var currentAlbumArtUrl: String? = null
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var repeatMode by mutableStateOf(RepeatMode.OFF)
    var isShuffleEnabled: Boolean = false
        private set

    private var playlist: List<Song> = emptyList()
    var currentIndex by mutableStateOf(-1)
    var playlistVersion by mutableStateOf(0)
        private set
    var onStateChanged: (() -> Unit)? = null

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var wasPlayingBeforeFocusLoss = false

    private var bluetoothReceiver: BroadcastReceiver? = null
    private var isBluetoothReceiverRegistered = false

    private var headsetReceiver: BroadcastReceiver? = null
    private var isHeadsetReceiverRegistered = false
    private lateinit var statsManager: StatsManager
    private var songStartTime: Long = 0

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_channel"
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PLAY_SPECIFIC_SONG = "ACTION_PLAY_SPECIFIC_SONG"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        statsManager = StatsManager(this)
        settingsManager = SettingsManager(this)

        createNotificationChannel()
        setupMediaSession()
        setupAudioManager()
        setupBluetoothReceiver()
        setupHeadsetReceiver()
    }

    private fun setupBluetoothReceiver() {
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        Log.d("MusicService", "Bluetooth déconnecté - Pause de la musique")
                        if (isPlaying) pause()
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                            Log.d("MusicService", "Bluetooth désactivé - Pause de la musique")
                            if (isPlaying) pause()
                        }
                    }
                    AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                        val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)
                        if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                            Log.d("MusicService", "Audio Bluetooth déconnecté - Pause")
                            if (isPlaying) pause()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }

        try {
            registerReceiver(bluetoothReceiver, filter)
            isBluetoothReceiverRegistered = true
            Log.d("MusicService", "Bluetooth receiver enregistré")
        } catch (e: Exception) {
            Log.e("MusicService", "Erreur lors de l'enregistrement du Bluetooth receiver", e)
        }
    }

    private fun setupHeadsetReceiver() {
        headsetReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    Log.d("MusicService", "Écouteurs débranchés - Pause de la musique")
                    if (isPlaying) pause()
                }
            }
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        try {
            registerReceiver(headsetReceiver, filter)
            isHeadsetReceiverRegistered = true
            Log.d("MusicService", "Headset receiver enregistré")
        } catch (e: Exception) {
            Log.e("MusicService", "Erreur lors de l'enregistrement du Headset receiver", e)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { resume() }
                override fun onPause() { pause() }
                override fun onSkipToNext() { playNext() }
                override fun onSkipToPrevious() { playPrevious() }
                override fun onSeekTo(pos: Long) { seekTo(pos) }
                override fun onStop() { stopPlayback() }
            })

            isActive = true
        }
    }

    private fun setupAudioManager() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeFocusLoss = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingBeforeFocusLoss = isPlaying
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun updatePlaybackState() {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, currentPosition, 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    // ✅ NOUVEAU : met à jour les métadonnées (titre, artiste, pochette) dans la MediaSession
    private fun updateMediaSessionMetadata() {
        val albumArt = try {
            val uri = Uri.parse("content://media/external/audio/albumart/${currentSong?.albumId}")
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null }

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong?.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong?.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong?.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
            .build()

        mediaSession.setMetadata(metadata)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> { if (isPlaying) pause() else resume() }
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> stopPlayback()
            ACTION_PLAY_SPECIFIC_SONG -> {
                val songId = intent.getLongExtra("song_id", -1L)
                if (songId != -1L) playSongFromId(songId)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lecture de musique",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        // ✅ Charger la pochette pour l'affichage agrandi sur l'écran de verrouillage
        val albumArt = try {
            val uri = Uri.parse("content://media/external/audio/albumart/${currentSong?.albumId}")
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null }

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val previousIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSong?.title ?: "Aucune chanson")
            .setContentText(currentSong?.artist ?: "")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(albumArt) // ✅ Pochette = lecteur agrandi sur l'écran de verrouillage
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_media_previous, "Précédent", previousIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Suivant", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Fermer", stopIntent)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setDeleteIntent(stopIntent)
            .build()
    }

    fun setPlaylist(songs: List<Song>) {
        playlist = songs
    }

    fun toggleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
    }

    fun setStatsManager(manager: StatsManager) {
        this.statsManager = manager
        Log.d("MusicService", "StatsManager configuré")
    }

    fun setSettingsManager(manager: SettingsManager) {
        this.settingsManager = manager
        Log.d("MusicService", "SettingsManager configuré")
    }

    private fun saveCurrentSongToSettings() {
        if (::settingsManager.isInitialized) {
            currentSong?.let { song ->
                settingsManager.lastPlayedSongId = song.id
                settingsManager.lastPlayedPosition = currentPosition
                Log.d("MusicService", "Sauvegarde: ${song.title} à ${currentPosition}ms")
            }
        }
    }

    fun saveLastPlayedSong(settingsManager: SettingsManager) {
        currentSong?.let { song ->
            settingsManager.lastPlayedSongId = song.id
            settingsManager.lastPlayedPosition = currentPosition
        }
    }

    fun playSong(song: Song) {
        Log.d("MusicService", "playSong called: ${song.title}")

        if (!requestAudioFocus()) return

        onStateChanged?.invoke()
        saveCurrentSongToSettings()

        try {
            currentSong?.let { previousSong ->
                val playDuration = (System.currentTimeMillis() - songStartTime) / 1000
                if (playDuration >= 30) {
                    statsManager.recordSongPlay(previousSong, playDuration)
                }
            }

            songStartTime = System.currentTimeMillis()
            currentIndex = playlist.indexOfFirst { it.id == song.id }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener { _, _, _ ->
                    handlePlaybackError(song)
                    true
                }
                setDataSource(this@MusicService, song.uri)
                prepare()
                setOnCompletionListener { handleSongCompletion() }
                start()
            }

            currentSong = song
            duration = mediaPlayer?.duration?.toLong() ?: 0L
            isPlaying = true
            currentAlbumArtUrl = "content://media/external/audio/albumart/${song.albumId}"

            updatePlaybackState()
            updateMediaSessionMetadata() // ✅ Met à jour la pochette dans la MediaSession
            startForeground(NOTIFICATION_ID, buildNotification())
            onStateChanged?.invoke()

        } catch (e: Exception) {
            e.printStackTrace()
            handlePlaybackError(song)
        }

        updateWidget()
    }

    private fun playSongFromId(songId: Long) {
        Log.d("MusicService", "playSongFromId called: songId=$songId")

        if (!isPlaying && currentSong == null) {
            try {
                startForeground(NOTIFICATION_ID, buildNotification())
                Log.d("MusicService", "Started foreground service")
            } catch (e: Exception) {
                Log.e("MusicService", "Error starting foreground service", e)
            }
        }

        val song = playlist.find { it.id == songId }
        if (song != null) {
            Log.d("MusicService", "Found song in current playlist: ${song.title}")
            playSong(song)
        } else {
            Log.d("MusicService", "Song not in playlist, loading all songs")
            loadAllSongsAndPlay(songId)
        }
    }

    private fun loadAllSongsAndPlay(songId: Long) {
        Thread {
            try {
                val allSongs = mutableListOf<Song>()
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
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

                contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Titre inconnu"
                        val artist = cursor.getString(artistColumn) ?: "Artiste inconnu"
                        val duration = cursor.getLong(durationColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        val album = cursor.getString(albumColumn) ?: "Album inconnu"

                        val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())

                        try {
                            contentResolver.openInputStream(uri)?.close()
                            allSongs.add(Song(id, title, artist, duration, uri, albumId, album))
                        } catch (e: Exception) {
                            continue
                        }
                    }
                }

                playlist = allSongs
                Log.d("MusicService", "Updated playlist with ${allSongs.size} songs")

                val targetSong = allSongs.find { it.id == songId }
                if (targetSong != null) {
                    Log.d("MusicService", "Found target song: ${targetSong.title}")
                    playSong(targetSong)
                } else {
                    Log.e("MusicService", "Target song not found in playlist: $songId")
                }
            } catch (e: Exception) {
                Log.e("MusicService", "Error loading songs for widget", e)
            }
        }.start()
    }

    private fun handlePlaybackError(failedSong: Song) {
        Log.e("MusicService", "Impossible de lire: ${failedSong.title}")

        if (hasNext) {
            playNext()
        } else if (hasPrevious) {
            playPrevious()
        } else {
            isPlaying = false
            currentSong = null
            updatePlaybackState()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlaybackState()
        startForeground(NOTIFICATION_ID, buildNotification())
        onStateChanged?.invoke()
        saveCurrentSongToSettings()
        updateWidget()
    }

    fun resume() {
        if (!requestAudioFocus()) return

        mediaPlayer?.start()
        isPlaying = true
        updatePlaybackState()
        startForeground(NOTIFICATION_ID, buildNotification())
        onStateChanged?.invoke()
        updateWidget()
    }

    fun stopPlayback() {
        pause()
        abandonAudioFocus()
        stopForeground(true)
        stopSelf()
    }

    fun playNext() {
        if (playlist.isEmpty()) return

        if (isShuffleEnabled) {
            playSong(playlist[playlist.indices.random()])
            return
        }

        val nextIndex = currentIndex + 1
        if (nextIndex < playlist.size) {
            playSong(playlist[nextIndex])
        } else if (repeatMode == RepeatMode.ALL) {
            playSong(playlist[0])
        }
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return

        if (isShuffleEnabled) {
            playSong(playlist[playlist.indices.random()])
            return
        }

        val previousIndex = currentIndex - 1
        if (previousIndex >= 0) {
            playSong(playlist[previousIndex])
        } else if (repeatMode == RepeatMode.ALL) {
            playSong(playlist.last())
        }
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        currentPosition = position
        updatePlaybackState()
        saveCurrentSongToSettings()
    }

    fun updatePosition() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                currentPosition = it.currentPosition.toLong()
                updateWidget()
            }
        }
    }

    private fun handleSongCompletion() {
        currentSong?.let { song ->
            val playDuration = (System.currentTimeMillis() - songStartTime) / 1000
            if (playDuration >= 30) {
                statsManager.recordSongPlay(song, playDuration)
                Log.d("MusicService", "Terminé: ${song.title} - ${playDuration}s")
            }
        }

        when (repeatMode) {
            RepeatMode.ONE -> currentSong?.let { playSong(it) }
            RepeatMode.ALL -> {
                if (hasNext) playNext()
                else if (playlist.isNotEmpty()) playSong(playlist[0])
            }
            RepeatMode.OFF -> {
                if (hasNext) playNext()
            }
        }
    }

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val hasNext: Boolean
        get() = currentIndex < playlist.size - 1

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        currentPosition = 0L
        abandonAudioFocus()
        updatePlaybackState()
    }

    override fun onDestroy() {
        super.onDestroy()

        currentSong?.let { song ->
            val playDuration = (System.currentTimeMillis() - songStartTime) / 1000
            if (playDuration >= 30) {
                statsManager.recordSongPlay(song, playDuration)
            }
        }

        try {
            if (isBluetoothReceiverRegistered) {
                unregisterReceiver(bluetoothReceiver)
                isBluetoothReceiverRegistered = false
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Erreur Bluetooth receiver", e)
        }

        try {
            if (isHeadsetReceiverRegistered) {
                unregisterReceiver(headsetReceiver)
                isHeadsetReceiverRegistered = false
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Erreur Headset receiver", e)
        }

        mediaSession.release()
        release()
    }

    fun getCurrentPlaylist(): List<Song> = playlist.toList()

    fun updatePlaylist(newPlaylist: List<Song>) {
        playlist = newPlaylist
        currentSong?.let { current ->
            currentIndex = playlist.indexOfFirst { it.id == current.id }
        }
        playlistVersion++
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlayback()
    }
}