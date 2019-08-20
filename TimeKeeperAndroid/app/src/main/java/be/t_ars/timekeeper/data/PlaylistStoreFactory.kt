package be.t_ars.timekeeper.data

import android.content.Context

object PlaylistStoreFactory {
    fun createStore(context: Context): IPlaylistStore {
        return FilePlaylistStore()
    }
}
