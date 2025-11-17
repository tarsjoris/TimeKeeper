package be.t_ars.timekeeper.data

class Playlist(id: Long, name: String, weight: Int) : PlaylistHeader(id, name, weight) {
    val songs: MutableList<Song> = ArrayList()

    constructor(other: Playlist, id: Long, name: String, weight: Int) : this(id, name, weight) {
        songs.addAll(other.songs.map { Song(it) })
    }

    fun addSong(store: PlaylistStore, song: Song) {
        addSong(song)
        store.savePlaylist(this)
    }

    fun addSong(song: Song) {
        songs.add(song)
    }

    fun removeSong(store: PlaylistStore, position: Int) {
        if (position in 0 until songs.size) {
            songs.removeAt(position)
        }
        store.savePlaylist(this)
    }

    fun removeSongAndAbove(store: PlaylistStore, position: Int) {
        for (i in 0..position) {
            songs.removeAt(0)
        }
        store.savePlaylist(this)
    }

    fun move(store: PlaylistStore, position: Int, up: Boolean) {
        val otherPos = position + if (up) -1 else 1
        if (position in 0 until songs.size && otherPos in 0 until songs.size) {
            val song = songs[position]
            val other = songs[otherPos]
            songs[position] = other
            songs[otherPos] = song
        }
        store.savePlaylist(this)
    }

    fun sendToTop(store: PlaylistStore, position: Int) {
        if (position in 1 until songs.size) {
            val song = songs.removeAt(position)
            songs.add(0, song)
        }
        store.savePlaylist(this)
    }

    fun sendToBottom(store: PlaylistStore, position: Int) {
        if (position in 0 until songs.size - 1) {
            val song = songs.removeAt(position)
            songs.add(song);
        }
        store.savePlaylist(this)
    }

    fun sort(store: PlaylistStore) {
        songs.sortBy { it.name }
        store.savePlaylist(this)
    }

    override fun hashCode() =
        id.toInt()

    override fun equals(other: Any?) =
        if (other is Playlist) other.id == id && other.songs == songs else false
}
