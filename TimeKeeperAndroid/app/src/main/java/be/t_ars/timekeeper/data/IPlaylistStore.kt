package be.t_ars.timekeeper.data

interface IPlaylistStore {
    fun readAllPlaylists(): List<PlaylistHeader>
    fun storePlaylistHeader(playlist: PlaylistHeader)
    fun readPlaylist(id: Long): Playlist?
    fun addPlaylist(playlist: Playlist)
    fun deletePlaylist(playlist: PlaylistHeader)
    val nextPlaylistWeight: Int
    fun storeSong(playlist: Playlist, song: Song)
    fun deleteSong(playlist: Playlist, song: Song)
    fun close()
}
