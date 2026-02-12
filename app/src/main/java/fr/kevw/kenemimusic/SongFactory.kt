package fr.kevw.kenemimusic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class SongFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val songs = mutableListOf<Song>()

    override fun onCreate() {
        loadSongs()
    }

    private fun loadSongs() {
        songs.clear()

        try {
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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

            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
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

                    val uri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        songs.add(Song(id, title, artist, duration, uri, albumId, album))
                    } catch (e: Exception) {
                        continue
                    }
                }
            }

//            Log.d("SongFactory", "Loaded ${songs.size} songs for widget")
        } catch (e: Exception) {
//            Log.e("SongFactory", "Error loading songs for widget", e)
        }
    }

    override fun onDataSetChanged() {
        loadSongs()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = songs.size

    override fun getViewAt(position: Int): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_song_item)

        val song = songs[position]
        view.setTextViewText(R.id.widget_item_text, "${song.title} - ${song.artist}")

        // ✅ fillInIntent simple avec juste les extras
        val fillInIntent = Intent().apply {
            putExtra("song_id", song.id)

        }

//        Log.d("SongFactory", "Setting click for song: ${song.title}, id: ${song.id}")
        view.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return view
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = songs[position].id

    override fun hasStableIds(): Boolean = true
}