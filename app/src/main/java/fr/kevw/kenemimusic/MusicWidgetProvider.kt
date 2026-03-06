package fr.kevw.kenemimusic

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.RemoteViews
import android.util.Log

class MusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "fr.kevw.kenemimusic.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "fr.kevw.kenemimusic.WIDGET_NEXT"
        const val ACTION_WIDGET_PREVIOUS = "fr.kevw.kenemimusic.WIDGET_PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "fr.kevw.kenemimusic.UPDATE_WIDGET"
        const val ACTION_WIDGET_SONG_CLICKED = "fr.kevw.kenemimusic.WIDGET_SONG_CLICKED"
        const val ACTION_WIDGET_OPEN_APP = "fr.kevw.kenemimusic.WIDGET_OPEN_APP"

        fun updateWidget(
            context: Context,
            song: Song?,
            isPlaying: Boolean,
            progress: Int,
            albumArtUrl: String? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MusicWidgetProvider::class.java)
            )

            val bitmap = if (albumArtUrl != null) {
                try {
                    val uri = android.net.Uri.parse(albumArtUrl)
                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                } catch (e: Exception) {
                    Log.w("MusicWidget", "Pochette introuvable: ${e.message}")
                    null
                }
            } else null

            widgetIds.forEach { widgetId ->
                updateAppWidget(context, appWidgetManager, widgetId, song, isPlaying, progress, bitmap)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            song: Song?,
            isPlaying: Boolean,
            progress: Int,
            albumBitmap: Bitmap? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music_player)

            if (song != null) {
                views.setTextViewText(R.id.widget_song_title, song.title)
                views.setTextViewText(R.id.widget_artist_name, song.artist)
                views.setProgressBar(R.id.widget_progress, 100, progress, false)
            } else {
                views.setTextViewText(R.id.widget_song_title, "Aucune chanson")
                views.setTextViewText(R.id.widget_artist_name, "Sélectionnez une chanson")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
            }

            if (albumBitmap != null) {
                views.setImageViewBitmap(R.id.widget_album_art, albumBitmap)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_music_note_widget)
            }

            val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

            val serviceIntent = Intent(context, WidgetSongService::class.java)
            views.setRemoteAdapter(R.id.widget_song_list, serviceIntent)
            views.setEmptyView(R.id.widget_song_list, R.id.widget_empty_view)

            val clickIntentTemplate = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_SONG_CLICKED
            }
            val clickPendingIntentTemplate = PendingIntent.getBroadcast(
                context, 0, clickIntentTemplate,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_song_list, clickPendingIntentTemplate)

            setupButtonClicks(context, views)

            val openAppIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_OPEN_APP
            }
            val openAppPendingIntent = PendingIntent.getBroadcast(
                context, 200, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_song_title, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setupButtonClicks(context: Context, views: RemoteViews) {
            val playPauseIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context, 0, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

            val previousIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PREVIOUS
            }
            val previousPendingIntent = PendingIntent.getBroadcast(
                context, 1, previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_previous, previousPendingIntent)

            val nextIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, 2, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("MusicWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId, null, false, 0)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("MusicWidget", "Received action: ${intent.action}")

        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                context.startService(Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY_PAUSE
                })
            }
            ACTION_WIDGET_PREVIOUS -> {
                context.startService(Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PREVIOUS
                })
            }
            ACTION_WIDGET_NEXT -> {
                context.startService(Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_NEXT
                })
            }
            ACTION_UPDATE_WIDGET -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, MusicWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, widgetIds)
            }
            ACTION_WIDGET_SONG_CLICKED -> {
                val songId = intent.getLongExtra("song_id", -1L)
                Log.d("MusicWidget", "Song clicked: songId=$songId")
                if (songId != -1L) {
                    try {
                        val serviceIntent = Intent(context, MusicService::class.java).apply {
                            action = MusicService.ACTION_PLAY_SPECIFIC_SONG
                            putExtra("song_id", songId)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        Log.e("MusicWidget", "Error starting service for song: $songId", e)
                    }
                }
            }
            ACTION_WIDGET_OPEN_APP -> {
                try {
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(appIntent)
                } catch (e: Exception) {
                    Log.e("MusicWidget", "Error opening app", e)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        Log.d("MusicWidget", "Widget enabled")
    }

    override fun onDisabled(context: Context) {
        Log.d("MusicWidget", "Widget disabled")
    }
}