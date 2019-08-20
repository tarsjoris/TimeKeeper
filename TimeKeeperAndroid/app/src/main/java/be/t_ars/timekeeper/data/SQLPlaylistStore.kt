package be.t_ars.timekeeper.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class SQLPlaylistStore internal constructor(context: Context) : SQLiteOpenHelper(context, kDATABASE_NAME, null, kDATABASE_VERSION), IPlaylistStore {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(kPLAYLIST_TABLE_CREATE)
        db.execSQL(kSONG_TABLE_CREATE)

        val playlist = Playlist("Solid Rock", 0)
        playlist.addSong(Song("Weak", 95))
        playlist.addSong(Song("It's So Hard", 128))
        playlist.addSong(Song("So Lonely", 160))
        playlist.addSong(Song("Message In a Bottle", 152))
        playlist.addSong(Song("You Oughta Know", 105))
        playlist.addSong(Song("Hard To Handle", 104))
        playlist.addSong(Song("Personal Jesus", 119))
        playlist.addSong(Song("Run to you", 126))
        playlist.addSong(Song("Don't Stop Me Now (1/2)", 101))
        playlist.addSong(Song("Don't Stop Me Now (2/2)", 158))
        playlist.addSong(Song("Welcome To The Jungle (1/2)", 100))
        playlist.addSong(Song("Welcome To The Jungle (2/2)", 120))
        playlist.addSong(Song("Jerusalem", 132))
        playlist.addSong(Song("Girl", 136))
        playlist.addSong(Song("Are You Gonna Go My Way", 130))
        playlist.addSong(Song("Always On The Run", 88))
        playlist.addSong(Song("Heavy Cross", 118))
        playlist.addSong(Song("Dirty Diana", 131))
        playlist.addSong(Song("Sweet Child O' Mine", 126))
        playlist.addSong(Song("Paradise City (1/2)", 100))
        playlist.addSong(Song("Paradise City (2/2)", 216))
        playlist.addSong(Song("The Pretender", 87))
        playlist.addSong(Song("Still Lovin' You", 106))
        playlist.addSong(Song("Pride", 105))
        playlist.addSong(Song("R U Kiddin' Me", 178))
        playlist.addSong(Song("Black Is Black", 96))
        playlist.addSong(Song("Thunder", 132))
        playlist.addSong(Song("All Night Long", 138))
        playlist.addSong(Song("Highway To Hell", 120))
        addPlaylist(db, playlist)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    override fun readAllPlaylists(): List<PlaylistHeader> {
        val playlists = ArrayList<PlaylistHeader>()
        readableDatabase.query(kPLAYLIST_TABLE_NAME, arrayOf(kPLAYLIST_ID, kPLAYLIST_NAME, kPLAYLIST_WEIGHT), null, null, null, null, kPLAYLIST_WEIGHT).use { playlistResult ->
            while (playlistResult.moveToNext()) {
                playlists.add(PlaylistHeader(playlistResult.getLong(0), playlistResult.getString(1), playlistResult.getInt(2)))
            }
        }
        return playlists
    }

    override fun readPlaylist(id: Long): Playlist? = readableDatabase.query(kPLAYLIST_TABLE_NAME, arrayOf(kPLAYLIST_NAME, kPLAYLIST_WEIGHT), "$kPLAYLIST_ID = ?", arrayOf(id.toString()), null, null, kPLAYLIST_WEIGHT).use { playlistResult ->
            if (playlistResult.moveToNext()) {
                val playlist = Playlist(id, playlistResult.getString(0), playlistResult.getInt(1))
                readableDatabase.query(kSONG_TABLE_NAME, arrayOf(kSONG_ID, kSONG_NAME, kSONG_WEIGHT, kSONG_TEMPO), "$kSONG_PLAYLIST = ?", arrayOf(id.toString()), null, null, kSONG_WEIGHT).use { songResult ->
                    while (songResult.moveToNext()) {
                        playlist.songs.add(Song(songResult.getInt(0).toLong(), songResult.getString(1), songResult.getInt(2), songResult.getInt(3)))
                    }
                }
                return playlist
            }
            return null
        }

    override fun storePlaylistHeader(playlist: PlaylistHeader) {
        storePlaylistHeader(writableDatabase, playlist)
    }

    override val nextPlaylistWeight: Int
        get() = readableDatabase.query(kPLAYLIST_TABLE_NAME, arrayOf("MAX($kPLAYLIST_WEIGHT)"), null, null, null, null, null).use { result ->
                if (result.moveToNext()) {
                    return result.getInt(0)
                }
                return 0
            }

    override fun addPlaylist(playlist: Playlist) {
        addPlaylist(writableDatabase, playlist)
    }

    override fun deletePlaylist(playlist: PlaylistHeader) {
        val idObject = playlist.id.toString()
        writableDatabase.delete(kSONG_TABLE_NAME, "$kSONG_PLAYLIST = ?", arrayOf(idObject))
        writableDatabase.delete(kPLAYLIST_TABLE_NAME, "$kPLAYLIST_ID = ?", arrayOf(idObject))
    }

    private fun addPlaylist(db: SQLiteDatabase, playlist: Playlist) {
        storePlaylistHeader(db, playlist)
        for (song in playlist.songs) {
            storeSong(db, playlist, song)
        }
    }

    private fun storePlaylistHeader(db: SQLiteDatabase, playlist: PlaylistHeader) {
        val values = ContentValues(1)
        values.put(kPLAYLIST_NAME, playlist.name)
        values.put(kPLAYLIST_WEIGHT, Integer.valueOf(playlist.weight))
        if (playlist.isNew()) {
            val result = db.insert(kPLAYLIST_TABLE_NAME, null, values)
            playlist.id = result
        } else {
            db.update(kPLAYLIST_TABLE_NAME, values, "$kPLAYLIST_ID = ?", arrayOf(playlist.id.toString()))
        }
    }

    override fun storeSong(playlist: Playlist, song: Song) {
        storeSong(writableDatabase, playlist, song)
    }

    private fun storeSong(db: SQLiteDatabase, playlist: PlaylistHeader, song: Song) {
        val values = ContentValues(3)
        values.put(kSONG_NAME, song.name)
        values.put(kSONG_WEIGHT, Integer.valueOf(song.weight))
        values.put(kSONG_TEMPO, Integer.valueOf(song.tempo))
        if (song.isNew()) {
            values.put(kSONG_PLAYLIST, java.lang.Long.valueOf(playlist.id))
            val result = db.insert(kSONG_TABLE_NAME, null, values)
            song.id = result
        } else {
            db.update(kSONG_TABLE_NAME, values, "$kSONG_ID = ?", arrayOf(song.id.toString()))
        }
    }

    override fun deleteSong(playlist: Playlist, song: Song) {
        writableDatabase.delete(kSONG_TABLE_NAME, "$kSONG_ID = ?", arrayOf(song.id.toString()))
    }

    companion object {
        private const val kDATABASE_NAME = "timekeeper"
        private const val kDATABASE_VERSION = 1

        private const val kPLAYLIST_TABLE_NAME = "playlist"
        private const val kPLAYLIST_ID = "id"
        private const val kPLAYLIST_NAME = "name"
        private const val kPLAYLIST_WEIGHT = "weight"
        private const val kPLAYLIST_TABLE_CREATE = "CREATE TABLE $kPLAYLIST_TABLE_NAME ($kPLAYLIST_ID INTEGER PRIMARY KEY, $kPLAYLIST_NAME TEXT, $kPLAYLIST_WEIGHT INTEGER);"

        private const val kSONG_TABLE_NAME = "song"
        private const val kSONG_ID = "id"
        private const val kSONG_PLAYLIST = "playlist"
        private const val kSONG_NAME = "name"
        private const val kSONG_TEMPO = "tempo"
        private const val kSONG_WEIGHT = "weight"
        private const val kSONG_TABLE_CREATE = "CREATE TABLE $kSONG_TABLE_NAME ($kSONG_ID INTEGER PRIMARY KEY, $kSONG_PLAYLIST INTEGER, $kSONG_NAME TEXT, $kSONG_TEMPO INTEGER, $kSONG_WEIGHT INTEGER, FOREIGN KEY ($kSONG_PLAYLIST) REFERENCES $kPLAYLIST_TABLE_NAME ($kPLAYLIST_ID));"
    }
}
