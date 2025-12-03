package fr.kevw.kenemimusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
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

    private var playlist: List<Song> = emptyList()
    var currentIndex by mutableStateOf(-1)

    var onStateChanged: (() -> Unit)? = null

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

            .addAction(
                android.R.drawable.ic_media_previous,
                "Précédent",
                previousIntent
            )
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Suivant",
                nextIntent
            )

            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
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

    fun playSong(song: Song) {
        try {
            currentIndex = playlist.indexOfFirst { it.id == song.id }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MusicService, song.uri)
                prepare()
                setOnCompletionListener { handleSongCompletion() }
                start()
            }

            currentSong = song
            duration = mediaPlayer?.duration?.toLong() ?: 0L
            isPlaying = true

            startForeground(NOTIFICATION_ID, buildNotification())
            onStateChanged?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
        startForeground(NOTIFICATION_ID, buildNotification())
        onStateChanged?.invoke()
    }

    fun resume() {
        mediaPlayer?.start()
        isPlaying = true
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
    var isShuffleEnabled: Boolean = false
        private set

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
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
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }
}