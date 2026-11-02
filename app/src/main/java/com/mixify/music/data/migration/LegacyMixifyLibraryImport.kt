package com.mixify.music.data.migration

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

/** Copies the old online library without ever changing or deleting song.db. */
object LegacyMixifyLibraryImport {
    private const val MARKER = "mixify_v106_library_import_v1"
    private const val KEY = "playlists_json"
    private val youtubeId = Regex("[A-Za-z0-9_-]{11}")

    // A LEFT JOIN retains songs whose cached artist information is incomplete.
    internal const val SONGS_SQL = """
        SELECT s.id, s.title, s.duration, s.thumbnailUrl, s.albumId, s.albumName,
               s.isVideo, s.liked, s.inLibrary,
               (SELECT a.name FROM song_artist_map sam JOIN artist a ON a.id = sam.artistId
                WHERE sam.songId = s.id ORDER BY sam.position LIMIT 1) AS artistName,
               (SELECT sam.artistId FROM song_artist_map sam
                WHERE sam.songId = s.id ORDER BY sam.position LIMIT 1) AS artistId
        FROM song s WHERE s.isLocal = 0 AND s.isEpisode = 0
    """
    internal const val PLAYLISTS_SQL = "SELECT id, name FROM playlist ORDER BY createdAt, id"
    internal const val MEMBERS_SQL = "SELECT songId FROM playlist_song_map WHERE playlistId = ? ORDER BY position, id"

    /** Called on the startup worker. Failure leaves the original and destination intact. */
    fun run(context: Context) {
        val source = context.getDatabasePath("song.db")
        if (!source.isFile) return
        val prefs = context.getSharedPreferences("bitchord_local_playlists", Context.MODE_PRIVATE)
        if (prefs.getBoolean(MARKER, false)) return
        runCatching {
            SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                check(db.version == 39) { "Unsupported legacy database version: ${db.version}" }
                // No Room schema upgrade is attempted on this database.
                val existing = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
                val ids = (0 until existing.length()).map { existing.getJSONObject(it).getString("id") }.toMutableSet()
                val songs = linkedMapOf<String, JSONObject>()
                val liked = JSONArray()
                val library = JSONArray()
                db.rawQuery(SONGS_SQL, null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.text("id") ?: continue
                        // Local paths and non-YouTube identifiers cannot be played by this engine.
                        if (!youtubeId.matches(id)) continue
                        val duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration")).coerceAtLeast(0)
                        val song = JSONObject().apply {
                            put("videoId", id)
                            put("title", cursor.text("title") ?: "Untitled")
                            put("artist", cursor.text("artistName") ?: "Unknown artist")
                            put("durationText", "%d:%02d".format(java.util.Locale.ROOT, duration / 60, duration % 60))
                            put("isVideo", cursor.getInt(cursor.getColumnIndexOrThrow("isVideo")) != 0)
                            for (key in listOf("thumbnailUrl", "artistId", "albumId", "albumName")) {
                                cursor.text(key)?.let { put(key, it) }
                            }
                        }
                        songs[id] = song
                        if (cursor.getInt(cursor.getColumnIndexOrThrow("liked")) != 0) liked.put(song)
                        if (!cursor.isNull(cursor.getColumnIndexOrThrow("inLibrary"))) library.put(song)
                    }
                }
                fun add(id: String, title: String, members: JSONArray) {
                    if (ids.add(id)) existing.put(JSONObject().put("id", id).put("title", title).put("songs", members))
                }
                if (liked.length() > 0) add("mixify106:liked", "Liked songs · Mixify 1.0.6", liked)
                if (library.length() > 0) add("mixify106:library", "Library · Mixify 1.0.6", library)
                db.rawQuery(PLAYLISTS_SQL, null).use { playlists ->
                    while (playlists.moveToNext()) {
                        val id = playlists.text("id") ?: continue
                        val members = JSONArray()
                        db.rawQuery(MEMBERS_SQL, arrayOf(id)).use { entries ->
                            while (entries.moveToNext()) entries.text("songId")?.let { songs[it] }?.let { members.put(it) }
                        }
                        // Retain empty cached playlists too; their remote contents are not fetched.
                        add("mixify106:playlist:$id", playlists.text("name") ?: "Imported playlist", members)
                    }
                }
                check(prefs.edit().putString(KEY, existing.toString()).putBoolean(MARKER, true).commit()) {
                    "Could not persist legacy library import"
                }
            }
        }.onFailure { android.util.Log.w("MixifyMigration", "Legacy library import deferred; source retained", it) }
    }

    private fun Cursor.text(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }
}
