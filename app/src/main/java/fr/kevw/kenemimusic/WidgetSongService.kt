package fr.kevw.kenemimusic

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetSongService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent)
            : RemoteViewsFactory {

        return SongFactory(applicationContext)
    }
}
