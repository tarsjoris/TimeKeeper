package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.SimpleAdapter
import be.t_ars.timekeeper.data.IPlaylistStore
import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.data.PlaylistStoreFactory
import kotlinx.android.synthetic.main.playlist.*
import java.io.Serializable
import java.util.*

class PlaylistActivity : AbstractActivity() {
    private val fStore: IPlaylistStore = PlaylistStoreFactory.createStore(this)
    private var fPlaylist: Playlist? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.playlist)
        setSupportActionBar(toolbar)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val playlistView = playlist
        playlistView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> trigger(position) }
        button_start.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                trigger(playlistView.checkedItemPosition)
            }
            false
        }
        button_stop.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                SoundService.stopSound(this)
            }
            false
        }
        button_next.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                doNext()
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getIntPreference(this, kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
        loadIntent()
    }

    override fun onDestroy() {
        fStore.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.playlist_action_edit -> {
                fPlaylist?.let { p ->
                    PlaylistEditActivity.startActivity(this, p.id)
                }
            }
            android.R.id.home -> {
                OverviewActivity.startActivity(this)
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun loadIntent() {
        val playlistView = playlist
        val extras = intent.extras
        val oldPlaylist = fPlaylist
        if (extras != null) {
            val playlistID = extras.getLong(kINTENT_DATA_PLAYLIST_ID)
            val newPlaylist = fStore.readPlaylist(playlistID)
            val defaultPosition = if (oldPlaylist?.id == playlistID) playlistView.checkedItemPosition else 0
            val newPosition = extras.getInt(kINTENT_DATA_POSITION, defaultPosition)
            loadPlaylist(newPlaylist, newPosition)
        } else if (oldPlaylist != null) {
            val newPlaylist = fStore.readPlaylist(oldPlaylist.id)
            val newPosition = playlistView.checkedItemPosition
            loadPlaylist(newPlaylist, newPosition)
        } else {
            loadPlaylist(null, 0)
        }
    }

    private fun loadPlaylist(pl: Playlist?, position: Int) {
        fPlaylist = pl
        supportActionBar?.title = fPlaylist?.name ?: "<no playlist>"
        val data = ArrayList<Map<String, String>>()
        fPlaylist?.let { p ->
            p.songs.forEach { song -> data.add(mapOf(kKEY_NAME to song.name, kKEY_TEMPO to "${song.tempo}")) }
        }

        val playlistView = playlist
        playlistView.adapter = SimpleAdapter(this,
                data,
                R.layout.playlist_entry,
                arrayOf(kKEY_NAME, kKEY_TEMPO),
                intArrayOf(R.id.playlist_entry_name, R.id.playlist_entry_tempo))

        fPlaylist?.let { p ->
            if (p.songs.isNotEmpty()) {
                playlistView.setItemChecked(position, true)
                playlistView.post { scrollToPosition(position) }
            }
        }
    }


    private fun doNext() {
        val playlistView = playlist
        val pos = playlistView.checkedItemPosition
        fPlaylist?.let { p ->
            if (pos < p.songs.size - 1) {
                val newPos = pos + 1
                trigger(newPos)
                scrollToPosition(newPos)
            }
        }
    }

    private fun scrollToPosition(position: Int) {
        val playlistView = playlist
        val first = playlistView.firstVisiblePosition
        val last = playlistView.lastVisiblePosition
        val middle = (last - first) / 2 + first
        if (position <= first) {
            playlistView.smoothScrollToPosition(position)
        } else if (position > middle) {
            playlistView.smoothScrollToPosition(last + position - middle)
        }
    }

    private fun trigger(selection: Int) {
        fPlaylist?.let { p ->
            val pos = if (selection in 0 until p.songs.size) selection else 0
            if (p.songs.isNotEmpty()) {
                val song = p.songs[pos]
                val extras = HashMap<String, Serializable>().also {
                    it[kINTENT_DATA_PLAYLIST_ID] = fPlaylist?.id ?: 0
                    it[kINTENT_DATA_POSITION] = pos
                }
                SoundService.startSound(this, song.name, song.tempo, PlaylistActivity::class.java, extras)
                playlist.setItemChecked(pos, true)
            }
        }
    }

    companion object {
        private const val kINTENT_DATA_PLAYLIST_ID = "playlist-id"
        private const val kINTENT_DATA_POSITION = "position"

        private const val kKEY_NAME = "name"
        private const val kKEY_TEMPO = "tempo"

        fun startActivity(context: Context, playlistID: Long) =
                Intent(context, PlaylistActivity::class.java)
                        .also { intent ->
                            intent.putExtra(kINTENT_DATA_PLAYLIST_ID, playlistID)
                        }
                        .let(context::startActivity)
    }
}
