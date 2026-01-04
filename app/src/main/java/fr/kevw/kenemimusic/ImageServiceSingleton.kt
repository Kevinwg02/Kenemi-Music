package fr.kevw.kenemimusic

import android.content.Context

object ImageServiceSingleton {
    private lateinit var artistImageService: ArtistImageService
    
    fun init(context: Context) {
        if (!::artistImageService.isInitialized) {
            artistImageService = ArtistImageService(context)
        }
    }
    
    fun getArtistImageService(): ArtistImageService = artistImageService
}