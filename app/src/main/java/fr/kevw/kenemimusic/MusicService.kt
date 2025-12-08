package fr.kevw.kenemimusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

class MusicService : Service() {
    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    var currentSong: Song? = null
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var repeatMode by mutableStateOf(RepeatMode.OFF)
    var isShuffleEnabled: Boolean = false
        private set

    private var playlist: List<Song> = emptyList()
    var currentIndex by mutableStateOf(-1)
    var onStateChanged: (() -> Unit)? = null

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_channel"
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        setupAudioManager()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resume()
                }

                override fun onPause() {
                    pause()
                }

                override fun onSkipToNext() {
                    playNext()
                }

                override fun onSkipToPrevious() {
                    playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                }

                override fun onStop() {
                    pause()
                }
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
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                resume()
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
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, currentPosition, 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (isPlaying) pause() else resume()
            }
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSong?.title ?: "Aucune chanson")
            .setContentText(currentSong?.artist ?: "")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_media_previous, "Précédent", previousIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Suivant", nextIntent)
            .setStyle(MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSession.sessionToken))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
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

    fun playSong(song: Song) {
        if (!requestAudioFocus()) {
            return
        }

        try {
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

            updatePlaybackState()
            startForeground(NOTIFICATION_ID, buildNotification())
            onStateChanged?.invoke()

        } catch (e: Exception) {
            e.printStackTrace()
            handlePlaybackError(song)
        }
    }

    private fun handlePlaybackError(failedSong: Song) {
        android.util.Log.e("MusicService", "Impossible de lire: ${failedSong.title}")

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
    }

    fun resume() {
        if (!requestAudioFocus()) return

        mediaPlayer?.start()
        isPlaying = true
        updatePlaybackState()
        startForeground(NOTIFICATION_ID, buildNotification())
        onStateChanged?.invoke()
    }

    fun playNext() {
        if (playlist.isEmpty()) return

        if (isShuffleEnabled) {
            val randomIndex = playlist.indices.random()
            playSong(playlist[randomIndex])
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
            val randomIndex = playlist.indices.random()
            playSong(playlist[randomIndex])
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
    }

    fun updatePosition() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                currentPosition = it.currentPosition.toLong()
            }
        }
    }

    private fun handleSongCompletion() {
        when (repeatMode) {
            RepeatMode.ONE -> currentSong?.let { playSong(it) }
            RepeatMode.ALL -> {
                if (hasNext) {
                    playNext()
                } else if (playlist.isNotEmpty()) {
                    playSong(playlist[0])
                }
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
        mediaSession.release()
        release()
    }
}