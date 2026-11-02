package com.mixify.music.playback

import com.mixify.music.BuildConfig

/** Public song and playlist links use the same website contract as the inbound intent handler. */
object MixifyShareLinks {
    fun song(videoId: String): String? {
        if (!videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) return null
        val variant = if (BuildConfig.APPLICATION_ID == "com.mixify.app.dev") "&app=dev" else ""
        return "https://mixify.co.in/open?song=$videoId$variant"
    }

    fun playlist(playlistId: String): String? {
        // Strip the common 'VL' prefix YouTube Music uses internally if present
        val cleanedId = playlistId.removePrefix("VL")
        // Basic sanity check for playlist IDs (usually start with PL, OL, RD, etc. and contain alphanumeric chars/hyphens/underscores)
        if (!cleanedId.matches(Regex("[A-Za-z0-9_-]+"))) return null
        val encodedId = java.net.URLEncoder.encode(cleanedId, "UTF-8")
        val variant = if (BuildConfig.APPLICATION_ID == "com.mixify.app.dev") "&app=dev" else ""
        return "https://mixify.co.in/open?playlist=$encodedId$variant"
    }
}
