package be.t_ars.timekeeper.data

import java.util.*

class Playlist(id: Long, name: String, weight: Int) : PlaylistHeader(id, name, weight) {
    val songs: MutableList<Song> = ArrayList()

    constructor(name: String, weight: Int) : this(kID_NEW, name, weight)

    constructor(other: Playlist, name: String, weight: Int) : this(name, weight) {
        songs.addAll(other.songs.map { Song(it) })
    }

    fun addSong(store: IPlaylistStore, song: Song) {
        addSong(song)
        store.storeSong(this, song)
    }

    fun addSong(song: Song) {
        song.weight = if (songs.isNotEmpty()) {
            songs[songs.size - 1].weight + 1
        } else {
            0
        }
        songs.add(song)
    }

    fun removeSong(store: IPlaylistStore, position: Int) {
        if (position in 0 until songs.size) {
            val song = songs.removeAt(position)
            store.deleteSong(this, song)
            for (i in position until songs.size) {
                val after = songs[i]
                after.weight = after.weight - 1
                store.storeSong(this, after)
            }
        }
    }

    fun move(store: IPlaylistStore, position: Int, up: Boolean) {
        val otherPos = position + if (up) -1 else 1
        if (position in 0 until songs.size && otherPos in 0 until songs.size) {
            val song = songs[position]
            val other = songs[otherPos]
            songs[position] = other
            songs[otherPos] = song
            val tmpWeight = other.weight
            other.weight = song.weight
            song.weight = tmpWeight
            store.storeSong(this, other)
            store.storeSong(this, song)
        }
    }
}
