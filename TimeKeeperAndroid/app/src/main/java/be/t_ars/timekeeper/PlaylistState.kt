package be.t_ars.timekeeper

import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.data.Song

class PlaylistState {
    companion object {
        var currentPlaylist: Playlist? = null
            set(value) {
                if (value == null || field?.id != value.id || currentPos !in value.songs.indices) {
                    currentPos = 0
                }
                field = value
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