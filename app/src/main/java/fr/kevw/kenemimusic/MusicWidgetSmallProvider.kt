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
import android.util.Log
import android.widget.RemoteViews

class MusicWidgetSmallProvider : AppWidgetProvider() {

    companion object {
        fun updateWidget(
            context: Context,
            song: Song?,
            isPlaying: Boolean,
            progress: Int,
            albumArtUrl: String? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MusicWidgetSmallProvider::class.java)
            )

            val bitmap = if (albumArtUrl != null) {
                try {
                    val uri = android.net.Uri.parse(albumArtUrl)
                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                } catch (e: Exception) {
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
            val views = RemoteViews(context.packageName, R.layout.widget_music_player_small)

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

            // Bouton Play/Pause
            val playPauseIntent = PendingIntent.getBroadcast(
                context, 10,
                Intent(context, MusicWidgetSmallProvider::class.java).apply {
                    action = MusicWidgetProvider.ACTION_WIDGET_PLAY_PAUSE
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPauseIntent)

            // Bouton Previous
            val previousIntent = PendingIntent.getBroadcast(
                context, 11,
                Intent(context, MusicWidgetSmallProvider::class.java).apply {
                    action = MusicWidgetProvider.ACTION_WIDGET_PREVIOUS
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_previous, previousIntent)

            // Bouton Next
            val nextIntent = PendingIntent.getBroadcast(
                context, 12,
                Intent(context, MusicWidgetSmallProvider::class.java).apply {
                    action = MusicWidgetProvider.ACTION_WIDGET_NEXT
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextIntent)

            // Clic sur le titre → ouvrir l'app
            val openAppIntent = PendingIntent.getBroadcast(
                context, 13,
                Intent(context, MusicWidgetSmallProvider::class.java).apply {
                    action = MusicWidgetProvider.ACTION_WIDGET_OPEN_APP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_song_title, openAppIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateAppWidget(context, appWidgetManager, widgetId, null, false, 0)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            MusicWidgetProvider.ACTION_WIDGET_PLAY_PAUSE -> {
                context.startService(Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY_PAUSE
                })
            }
            MusicWidgetProvider.ACTION_WIDGET_PREVIOUS -> {
                context.startService(Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PREVIOUS
                })
            }
            MusicWidgetProvider.ACTION_WIDGET_NEXT -> {
                context.startService(Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_NEXT
                })
            }
            MusicWidgetProvider.ACTION_WIDGET_OPEN_APP -> {
                try {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MusicWidgetSmall", "Error opening app", e)
                }
            }
        }
    }
}