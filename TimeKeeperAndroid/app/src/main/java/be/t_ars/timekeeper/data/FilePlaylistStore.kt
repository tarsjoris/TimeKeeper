package be.t_ars.timekeeper.data

import android.os.Environment
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.util.*
import java.util.regex.Pattern

class FilePlaylistStore : IPlaylistStore {
    private class EntryComparator : Comparator<AbstractEntry> {
        override fun compare(lhs: AbstractEntry, rhs: AbstractEntry): Int {
            return lhs.weight - rhs.weight
        }
    }

    override fun readAllPlaylists(): List<PlaylistHeader> {
        val playlists = ArrayList<PlaylistHeader>()
        val files = kBASE_PATH.listFiles()
        files?.forEach { file ->
            val matcher = kFILENAME_PATTERN.matcher(file.name)
            if (matcher.matches()) {
                val id = matcher.group(1)
                readPlaylistHeader(file, id)?.let { playlists.add(it) }
            }
        }
        Collections.sort(playlists, kCOMPARATOR)
        return playlists
    }

    override fun storePlaylistHeader(playlist: PlaylistHeader) {
        readPlaylist(playlist.id)?.let { completePlaylist ->
            completePlaylist.name = playlist.name
            completePlaylist.weight = playlist.weight
            savePlaylist(completePlaylist)
        }
    }

    override fun readPlaylist(id: Long): Playlist? {
        try {
            var playlist: Playlist? = null
            BufferedInputStream(FileInputStream(getPlaylistFile(id))).use { inputStream ->
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, "UTF-8")
                while (true) {
                    when (parser.next()) {
                        XmlPullParser.START_TAG -> {
                            if (kTAG_PLAYLIST == parser.name) {
                                val name = parser.getAttributeValue(null, kATTR_NAME)
                                val weight = Integer.parseInt(parser.getAttributeValue(null, kATTR_WEIGHT))
                                playlist = Playlist(id, name, weight)
                            } else if (kTAG_SONG == parser.name) {
                                val name = parser.getAttributeValue(null, kATTR_NAME)
                                val tempo = parser.getAttributeValue(null, kATTR_TEMPO)
                                        ?.let(Integer::parseInt)
                                playlist?.addSong(Song(name = name, tempo = tempo))
                            }
                        }
                        XmlPullParser.END_DOCUMENT -> {
                            return playlist
                        }
                        else -> {
                        }
                    }
                }
            }
        } catch (e: XmlPullParserException) {
            Log.e("TimeKeeper", "Could not read playlist: " + e.message, e)
        } catch (e: IOException) {
            Log.e("TimeKeeper", "Could not read playlist: " + e.message, e)
        }

        return null
    }

    override fun addPlaylist(playlist: Playlist) {
        savePlaylist(playlist)
    }

    override fun deletePlaylist(playlist: PlaylistHeader) {
        getPlaylistFile(playlist.id).delete()
    }

    override val nextPlaylistWeight: Int
        get() {
            val playlists = readAllPlaylists()
            if (playlists.isEmpty()) {
                return 0
            }
            return playlists[playlists.size - 1].weight + 1
        }

    override fun storeSong(playlist: Playlist, song: Song) {
        savePlaylist(playlist)
    }

    override fun deleteSong(playlist: Playlist, song: Song) {
        savePlaylist(playlist)
    }

    override fun close() {}

    private fun readPlaylistHeader(file: File, filename: String): PlaylistHeader? {
        try {
            val id = java.lang.Long.parseLong(filename)
            BufferedInputStream(FileInputStream(file)).use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                while (true) {
                    when (parser.next()) {
                        XmlPullParser.START_TAG -> {
                            parser.require(XmlPullParser.START_TAG, null, kTAG_PLAYLIST)
                            val name = parser.getAttributeValue(null, kATTR_NAME)
                            val weight = Integer.parseInt(parser.getAttributeValue(null, kATTR_WEIGHT))
                            return PlaylistHeader(id, name, weight)
                        }
                        XmlPullParser.END_DOCUMENT -> {
                            return null
                        }
                        else -> {
                        }
                    }
                }
            }
        } catch (e: XmlPullParserException) {
            Log.e("TimeKeeper", "Could not read playlist: " + e.message, e)
        } catch (e: IOException) {
            Log.e("TimeKeeper", "Could not read playlist: " + e.message, e)
        }

        return null
    }

    private fun savePlaylist(playlist: Playlist) {
        try {
            kBASE_PATH.mkdirs()
            if (playlist.isNew()) {
                playlist.id = System.currentTimeMillis()
            }
            val serializer = Xml.newSerializer()
            BufferedOutputStream(FileOutputStream(getPlaylistFile(playlist.id))).use { outputStream ->
                serializer.setOutput(outputStream, "UTF-8")
                serializer.startDocument("UTF-8", null)
                serializer.startTag(null, kTAG_PLAYLIST)
                serializer.attribute(null, kATTR_NAME, playlist.name)
                serializer.attribute(null, kATTR_WEIGHT, playlist.weight.toString())
                for (song in playlist.songs) {
                    serializer.startTag(null, kTAG_SONG)
                    serializer.attribute(null, kATTR_NAME, song.name)
                    if (song.tempo != null)
                        serializer.attribute(null, kATTR_TEMPO, song.tempo.toString())
                    serializer.endTag(null, kTAG_SONG)
                }
                serializer.endTag(null, kTAG_PLAYLIST)
                serializer.endDocument()
            }
        } catch (e: IOException) {
            Log.e("TimeKeeper", "Could not write playlist: " + e.message, e)
        }

    }

    private fun getPlaylistFile(id: Long): File {
        return File(kBASE_PATH, "$id.xml")
    }

    companion object {
        private const val kTAG_PLAYLIST = "playlist"
        private const val kTAG_SONG = "song"
        private const val kATTR_NAME = "name"
        private const val kATTR_WEIGHT = "weight"
        private const val kATTR_TEMPO = "tempo"

        private val kBASE_PATH = File(Environment.getExternalStorageDirectory(), "TimeKeeper")
        private val kCOMPARATOR = EntryComparator()
        private val kFILENAME_PATTERN = Pattern.compile("([0-9]+)\\.xml")
    }
}
