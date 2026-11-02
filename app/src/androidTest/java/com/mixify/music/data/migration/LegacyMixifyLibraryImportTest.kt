package com.mixify.music.data.migration

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LegacyMixifyLibraryImportTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val token = "legacy-import-test-${UUID.randomUUID()}"
    private val folder = File(base.cacheDir, token)
    private val context = object : ContextWrapper(base) {
        override fun getDatabasePath(name: String) = File(folder, name)
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            base.getSharedPreferences("$token-$name", mode)
    }
    private val source get() = context.getDatabasePath("song.db")
    private val prefs get() = context.getSharedPreferences("bitchord_local_playlists", Context.MODE_PRIVATE)

    @Before fun fixture() {
        folder.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(source, null).use { db ->
            db.execSQL("CREATE TABLE song(id TEXT PRIMARY KEY,title TEXT,duration INTEGER,thumbnailUrl TEXT,albumId TEXT,albumName TEXT,isVideo INTEGER,liked INTEGER,inLibrary INTEGER,isLocal INTEGER,isEpisode INTEGER)")
            db.execSQL("CREATE TABLE artist(id TEXT PRIMARY KEY,name TEXT)")
            db.execSQL("CREATE TABLE song_artist_map(songId TEXT,artistId TEXT,position INTEGER)")
            db.execSQL("CREATE TABLE playlist(id TEXT PRIMARY KEY,name TEXT,createdAt INTEGER)")
            db.execSQL("CREATE TABLE playlist_song_map(id INTEGER,playlistId TEXT,songId TEXT,position INTEGER)")
            db.execSQL("INSERT INTO song VALUES('aaaaaaaaaaa','First',61,NULL,NULL,NULL,0,1,1,0,0)")
            db.execSQL("INSERT INTO song VALUES('bbbbbbbbbbb','No artist',120,NULL,NULL,NULL,1,0,1,0,0)")
            db.execSQL("INSERT INTO song VALUES('ccccccccccc','Local',90,NULL,NULL,NULL,0,1,1,1,0)")
            db.execSQL("INSERT INTO song VALUES('ddddddddddd','Podcast',90,NULL,NULL,NULL,0,1,1,0,1)")
            db.execSQL("INSERT INTO artist VALUES('artist','Original artist')")
            db.execSQL("INSERT INTO song_artist_map VALUES('aaaaaaaaaaa','artist',0)")
            db.execSQL("INSERT INTO playlist VALUES('p','My playlist',0)")
            db.execSQL("INSERT INTO playlist_song_map VALUES(1,'p','aaaaaaaaaaa',1)")
            db.execSQL("INSERT INTO playlist_song_map VALUES(2,'p','bbbbbbbbbbb',0)")
            db.version = 39
        }
    }
    @After fun cleanup() { folder.deleteRecursively(); base.deleteSharedPreferences("$token-bitchord_local_playlists") }
    private fun playlists() = JSONArray(prefs.getString("playlists_json", "[]"))

    @Test fun importsOnlineCollectionsInOrderAndLeavesSourceUntouched() {
        val before = source.readBytes()
        LegacyMixifyLibraryImport.run(context)
        val list = playlists()
        assertEquals(3, list.length())
        assertEquals(1, list.getJSONObject(0).getJSONArray("songs").length())
        assertEquals(2, list.getJSONObject(1).getJSONArray("songs").length())
        val entries = list.getJSONObject(2).getJSONArray("songs")
        assertEquals("bbbbbbbbbbb", entries.getJSONObject(0).getString("videoId"))
        assertEquals("Unknown artist", entries.getJSONObject(0).getString("artist"))
        assertEquals("1:01", entries.getJSONObject(1).getString("durationText"))
        assertArrayEquals(before, source.readBytes())
        val firstImport = list.toString()
        LegacyMixifyLibraryImport.run(context)
        assertEquals(firstImport, playlists().toString())
    }
    @Test fun malformedDestinationDoesNotOverwriteAndCanRetry() {
        prefs.edit().putString("playlists_json", "invalid-json").commit()
        LegacyMixifyLibraryImport.run(context)
        assertEquals("invalid-json", prefs.getString("playlists_json", null))
        assertFalse(prefs.getBoolean("mixify_v106_library_import_v1", false))
        prefs.edit().remove("playlists_json").commit()
        LegacyMixifyLibraryImport.run(context)
        assertEquals(3, playlists().length())
    }
    @Test fun unknownSchemaRemainsUntouched() {
        SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 40 }
        val before = source.readBytes()
        LegacyMixifyLibraryImport.run(context)
        assertEquals(0, playlists().length())
        assertArrayEquals(before, source.readBytes())
    }
    @Test fun existingDestinationPlaylistWinsOnIdCollision() {
        prefs.edit().putString("playlists_json", """[{"id":"mixify106:playlist:p","title":"Edited","songs":[]}]""").commit()
        LegacyMixifyLibraryImport.run(context)
        assertEquals(3, playlists().length())
        assertEquals("Edited", playlists().getJSONObject(0).getString("title"))
    }
}
