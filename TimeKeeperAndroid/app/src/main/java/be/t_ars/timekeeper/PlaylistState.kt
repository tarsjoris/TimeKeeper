package be.t_ars.timekeeper

import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.data.Song

class PlaylistState {
    companion object {
        var currentPlaylist: Playlist? = null
            set(value) {
                field = value
                currentPos = 0
            }

        var currentPos = 0

        fun withCurrentPlaylist(consumer: (Playlist, Int?) -> Unit) {
            currentPlaylist?.let { playlist ->
                val pos = currentPos
                if (pos in playlist.songs.indices) {
                    consumer(playlist, pos)
                } else {
                    consumer(playlist, null)
                }
            }
        }

        fun withCurrentSong(consumer: (Playlist, Song, Int) -> Unit) {
            currentPlaylist?.let { playlist ->
                val pos = currentPos
                if (pos in playlist.songs.indices) {
                    consumer(playlist, playlist.songs[pos], pos)
                }
            }
        }
    }
}