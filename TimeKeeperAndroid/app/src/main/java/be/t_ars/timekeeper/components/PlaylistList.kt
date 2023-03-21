package be.t_ars.timekeeper.components

import android.content.Context
import android.widget.ListView
import android.widget.SimpleAdapter
import be.t_ars.timekeeper.R
import be.t_ars.timekeeper.data.PlaylistHeader

private const val kKEY_NAME = "name"

class PlaylistList() {
    private val fData: MutableList<Map<String, String>> = ArrayList()
    private var fAdapter: SimpleAdapter? = null

    fun bindToView(context: Context, view: ListView) {
        fAdapter = SimpleAdapter(
            context,
            fData,
            R.layout.overview_entry,
            arrayOf(kKEY_NAME),
            intArrayOf(R.id.overview_entry_name)
        )
        view.adapter = fAdapter
    }

    fun setPlaylists(playlists: List<PlaylistHeader>) {
        fData.clear()
        for (element in playlists) {
            val map = HashMap<String, String>()
            map[kKEY_NAME] = element.name
            fData.add(map)
        }
        fAdapter?.notifyDataSetChanged()
    }
}