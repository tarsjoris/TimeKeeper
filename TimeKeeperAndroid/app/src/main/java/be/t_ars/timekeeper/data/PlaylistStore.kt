package be.t_ars.timekeeper.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.util.Xml
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import be.t_ars.timekeeper.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

class PlaylistStore(private val fContext: Context) {
    private class PlaylistComparator : Comparator<PlaylistHeader> {
        override fun compare(lhs: PlaylistHeader, rhs: PlaylistHeader): Int {
            return lhs.weight - rhs.weight
        }
    }

    fun readAllPlaylists(): List<PlaylistHeader> {
        val playlists = ArrayList<PlaylistHeader>()
        DocumentFile.fromTreeUri(fContext, getFolder())
            ?.listFiles()!!
            .forEach { file ->
                val matcher = kFILENAME_PATTERN.matcher(file.name!!)
                if (matcher.matches()) {
                    matcher.group(1)?.let { id ->
                        readPlaylistHeader(file.uri, id)
                            ?.let { playlists.add(it) }
                    }
                }
            }
        Collections.sort(playlists, kPLAYLIST_COMPARATOR)
        return playlists
    }

    fun storePlaylistHeader(playlist: PlaylistHeader) {
        readPlaylist(playlist.id)?.let { completePlaylist ->
            completePlaylist.name = playlist.name
            completePlaylist.weight = playlist.weight
            savePlaylist(completePlaylist)
        }
    }

    fun readPlaylist(id: Long): Playlist? {
        try {
            var playlist: Playlist? = null
            fContext.contentResolver.openInputStream(getPlaylistUri(id))?.use { inputStream ->
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, "UTF-8")
                while (true) {
                    when (parser.next()) {
                        XmlPullParser.START_TAG -> {
                            if (kTAG_PLAYLIST == parser.name) {
                                val name = parser.getAttributeValue(null, kATTR_NAME)
                                val weight =
                                    Integer.parseInt(parser.getAttributeValue(null, kATTR_WEIGHT))
                                playlist = Playlist(id, name, weight)
                            } else if (kTAG_SONG == parser.name) {
                                val name = parser.getAttributeValue(null, kATTR_NAME)
                                val tempo = parser.getAttributeValue(null, kATTR_TEMPO)?.toInt()
                                    ?: ClickDescription.DEFAULT_TEMPO
                                val clickType = parser.getAttributeValue(null, kATTR_CLICK_TYPE)
                                    ?.let(Integer::parseInt)
                                    ?.let(EClickType::of)
                                    ?: EClickType.DEFAULT
                                val divisionCount =
                                    parser.getAttributeValue(null, kATTR_DIVISION_COUNT)?.toInt()
                                        ?: ClickDescription.DEFAULT_DIVISION_COUNT
                                val beatCount =
                                    parser.getAttributeValue(null, kATTR_BEAT_COUNT)?.toInt()
                                        ?: ClickDescription.DEFAULT_BEAT_COUNT
                                val countOff = parser.getAttributeValue(null, kATTR_COUNT_OFF)
                                    ?.let { it.toBoolean() }
                                    ?: false
                                val trackPath = parser.getAttributeValue(null, kATTR_TRACK_PATH)
                                val scoreLink = parser.getAttributeValue(null, kATTR_SCORE_LINK)
                                playlist?.addSong(
                                    Song(
                                        name,
                                        ClickDescription(
                                            tempo,
                                            clickType,
                                            divisionCount,
                                            beatCount,
                                            countOff,
                                            trackPath
                                        ),
                                        scoreLink
                                    )
                                )
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

    fun addPlaylist(playlist: Playlist) {
        savePlaylist(playlist)
    }

    fun deletePlaylist(playlist: PlaylistHeader) {
        deleteFile(getPlaylistUri(playlist.id))
    }

    private fun deleteFile(uri: Uri) {
        DocumentsContract.deleteDocument(fContext.contentResolver, uri)
    }

    val nextPlaylistWeight: Int
        get() {
            val playlists = readAllPlaylists()
            if (playlists.isEmpty()) {
                return 0
            }
            return playlists[playlists.size - 1].weight + 1
        }

    fun createDocumentRequest() =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/xml"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, getFolder().toString())
            val id = System.currentTimeMillis().toString()
            putExtra(Intent.EXTRA_TITLE, "$id.xml")
        }


    fun acceptRequestedDocument(data: Intent?, documentIdConsumer: (Long) -> Unit) {
        data?.data?.let { uri ->
            val path = uri.path
            if (path != null && path.endsWith(".xml")) {
                if (isInCorrectFolder(uri)) {
                    val lastSlash = path.lastIndexOf('/')
                    if (lastSlash != -1) {
                        val filename = path.substring(lastSlash + 1, path.length - 4)
                        try {
                            documentIdConsumer(filename.toLong())
                        } catch (e: NumberFormatException) {
                            deleteFile(uri)
                            Toast.makeText(fContext, "Invalid id '$filename'", Toast.LENGTH_LONG)
                                .show()
                        }
                    } else {
                        deleteFile(uri)
                    }
                } else {
                    deleteFile(uri)
                    Toast.makeText(fContext, "Place file in store folder", Toast.LENGTH_LONG).show()
                }
            } else {
                deleteFile(uri)
                Toast.makeText(fContext, "File should have extension 'xml'", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun isInCorrectFolder(uri: Uri): Boolean {
        val tree = getFolder().toString().substringAfter("/tree/")
        val document = uri.toString().substringAfter("/document/")
        return document.startsWith("$tree%2F")
    }

    private fun readPlaylistHeader(uri: Uri, filename: String): PlaylistHeader? {
        try {
            val id = filename.toLong()
            fContext.contentResolver.openInputStream(uri).use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                while (true) {
                    when (parser.next()) {
                        XmlPullParser.START_TAG -> {
                            parser.require(XmlPullParser.START_TAG, null, kTAG_PLAYLIST)
                            val name = parser.getAttributeValue(null, kATTR_NAME)
                            val weight =
                                Integer.parseInt(parser.getAttributeValue(null, kATTR_WEIGHT))
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

    fun savePlaylist(playlist: Playlist) {
        try {
            val uri = getPlaylistUri(playlist.id)
            val serializer = Xml.newSerializer()
            fContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { outputStream ->
                    // WTF: otherwise old bytes are left at the end of the file
                    outputStream.channel.truncate(0)

                    serializer.setOutput(outputStream, "UTF-8")
                    serializer.startDocument("UTF-8", null)
                    serializer.startTag(null, kTAG_PLAYLIST)
                    serializer.attribute(null, kATTR_NAME, playlist.name)
                    serializer.attribute(null, kATTR_WEIGHT, playlist.weight.toString())
                    for (song in playlist.songs) {
                        serializer.startTag(null, kTAG_SONG)
                        serializer.attribute(null, kATTR_NAME, song.name)
                        serializer.attribute(null, kATTR_TEMPO, song.click.bpm.toString())
                        if (song.click.type != EClickType.DEFAULT)
                            serializer.attribute(
                                null,
                                kATTR_CLICK_TYPE,
                                song.click.type.value.toString()
                            )
                        if (song.click.divisionCount != ClickDescription.DEFAULT_DIVISION_COUNT)
                            serializer.attribute(
                                null,
                                kATTR_DIVISION_COUNT,
                                song.click.divisionCount.toString()
                            )
                        if (song.click.beatCount != ClickDescription.DEFAULT_BEAT_COUNT)
                            serializer.attribute(
                                null,
                                kATTR_BEAT_COUNT,
                                song.click.beatCount.toString()
                            )
                        serializer.attribute(null, kATTR_COUNT_OFF, song.click.countOff.toString())
                        if (song.click.trackPath != null)
                            serializer.attribute(null, kATTR_TRACK_PATH, song.click.trackPath)
                        if (song.scoreLink != null)
                            serializer.attribute(null, kATTR_SCORE_LINK, song.scoreLink)
                        serializer.endTag(null, kTAG_SONG)
                    }
                    serializer.endTag(null, kTAG_PLAYLIST)
                    serializer.endDocument()
                }
            }
        } catch (e: IOException) {
            Log.e("TimeKeeper", "Could not write playlist: " + e.message, e)
        }
    }

    private fun getPlaylistUri(id: Long) =
        resolveFileUri(fContext, "$id.xml")

    private fun getFolder() =
        getStorageFolder(fContext)

    fun setCurrentPlaylistID(playlistID: Long) {
        val oldPlaylistID = getSettingCurrentPlaylistID(fContext)
        setSettingCurrentPlaylistID(fContext, playlistID)
        if (oldPlaylistID != playlistID) {
            setSettingCurrentSongIndex(fContext, 0)
        }
    }

    fun setCurrentSongIndex(index: Int) {
        setSettingCurrentSongIndex(fContext, index)
    }

    fun withCurrentPlaylist(consumer: (Playlist, Int?) -> Unit) {
        getSettingCurrentPlaylistID(fContext)
            ?.let { readPlaylist(it) }
            ?.let { playlist ->
                val pos = getSettingCurrentSongIndex(fContext)
                if (pos in playlist.songs.indices) {
                    consumer(playlist, pos)
                } else {
                    consumer(playlist, null)
                }
            }
    }

    fun withCurrentSong(consumer: (Playlist, Song, Int) -> Unit) {
        withCurrentPlaylist { playlist, pos ->
            if (pos != null) {
                consumer(playlist, playlist.songs[pos], pos)
            }
        }
    }

    companion object {
        private const val kTAG_PLAYLIST = "playlist"
        private const val kTAG_SONG = "song"
        private const val kATTR_NAME = "name"
        private const val kATTR_WEIGHT = "weight"
        private const val kATTR_TEMPO = "tempo"
        private const val kATTR_CLICK_TYPE = "click_type"
        private const val kATTR_DIVISION_COUNT = "division_count"
        private const val kATTR_BEAT_COUNT = "beat_count"
        private const val kATTR_COUNT_OFF = "count_off"
        private const val kATTR_TRACK_PATH = "track_path"
        private const val kATTR_SCORE_LINK = "score_link"

        private val kPLAYLIST_COMPARATOR = PlaylistComparator()
        private val kFILENAME_PATTERN = Pattern.compile("([0-9]+)\\.xml")
    }
}
