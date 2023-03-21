package be.t_ars.timekeeper.components

import android.content.Context
import android.widget.ListView
import android.widget.SimpleAdapter
import be.t_ars.timekeeper.R
import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.data.PlaylistHeader
import be.t_ars.timekeeper.data.Song

private const val kKEY_NAME = "name"

class SongList {
    private val fData: MutableList<Map<String, String>> = ArrayList()
    private var fAdapter: SimpleAdapter? = null

    fun bindToView(context: Context, view: ListView) {
        fAdapter = SimpleAdapter(
            context,
            fData,
            R.layout.playlist_entry_name,
            arrayOf(kKEY_NAME),
            intArrayOf(R.id.playlist_entry_name_name)
        )
        view.adapter = fAdapter
    }

    fun setSongs(songs: List<Song>) {
        fData.clear()
        for (song in songs) {
            val map = HashMap<String, String>()
            map[kKEY_NAME] = song.name
            fData.add(map)
        }
        fAdapter?.notifyDataSetChanged()
    }
}