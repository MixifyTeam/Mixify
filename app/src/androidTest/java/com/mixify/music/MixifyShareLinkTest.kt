package com.mixify.music

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mixify.music.playback.LinkRequest
import com.mixify.music.playback.MusicLink
import com.mixify.music.playback.MixifyShareLinks
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MixifyShareLinkTest {
    @Test fun songShareOpensTheSameTrack() {
        val link = requireNotNull(MixifyShareLinks.song("dQw4w9WgXcQ"))
        assertEquals(LinkRequest.Track("dQw4w9WgXcQ"), MusicLink.parse(Uri.parse(link)))
    }
    @Test fun duplicatesAndInvalidSongIdsAreRejected() {
        assertNull(MusicLink.parse(Uri.parse("https://mixify.co.in/open?song=short")))
        assertNull(MusicLink.parse(Uri.parse("https://mixify.co.in/open?song=dQw4w9WgXcQ&song=abcdefghijk")))
        assertNull(MixifyShareLinks.song("local:audio:123"))
        assertNull(MusicLink.parse(Uri.parse("https://example.com/open?song=dQw4w9WgXcQ")))
    @Test fun playlistShareOpensTheSamePlaylist() {
        val link = requireNotNull(MixifyShareLinks.playlist("VLRD12345_67890"))
        val parsed = MusicLink.parse(Uri.parse(link))
        assertTrue(parsed is LinkRequest.Page)
        assertEquals("VLRD12345_67890", (parsed as LinkRequest.Page).browseId)
    }

    @Test fun duplicatesAndInvalidPlaylistIdsAreRejected() {
        assertNull(MusicLink.parse(Uri.parse("https://mixify.co.in/open?playlist=123&playlist=456")))
        assertNull(MusicLink.parse(Uri.parse("https://mixify.co.in/open?playlist=invalid*chars")))
    }
}
