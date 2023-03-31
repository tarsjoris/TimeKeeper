package be.t_ars.timekeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.SimpleAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.data.PlaylistStore
import be.t_ars.timekeeper.databinding.PlaylistBinding

class PlaylistActivity : AbstractActivity() {
    private inner class TimeKeeperBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimeKeeperApplication.kBROADCAST_EVENT_SONG_CHANGED) {
                songChanged()
            }
        }
    }

    private val fStore = PlaylistStore(this)
    private lateinit var fBinding: PlaylistBinding
    private val fBroadcastReceiver = TimeKeeperBroadcastReceiver()
    private var fCurrentPlaylist: Playlist? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fBinding = PlaylistBinding.inflate(layoutInflater)

        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val playlistView = fBinding.playlist
        playlistView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> trigger(position) }
        playlistView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            fCurrentPlaylist?.let { playlist ->
                val pos = fBinding.playlist.checkedItemPosition
                if (pos in playlist.songs.indices) {
                    scrollToPosition(pos)
                }
            }
        }
        fBinding.buttonStart.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_START_METRONOME))
        }
        fBinding.buttonStop.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_STOP_METRONOME))
        }
        fBinding.buttonNext.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG).also {
                it.putExtra(
                    TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG_EXTRA_OPEN_SCORE,
                    shouldOpenScoreOnNext()
                )
            })
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            fBroadcastReceiver,
            IntentFilter(TimeKeeperApplication.kBROADCAST_EVENT_SONG_CHANGED)
        )
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fBroadcastReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getSettingScreenOrientation(this)

        fStore.withCurrentPlaylist { playlist, pos ->
            if (fCurrentPlaylist != playlist) {
                fCurrentPlaylist = playlist
                loadPlaylist(playlist)
            }

            if (pos != null && pos in playlist.songs.indices) {
                val playlistView = fBinding.playlist
                playlistView.setItemChecked(pos, true)
                playlistView.post { scrollToPosition(pos) }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.playlist_open_score -> {
                openScore()
            }
            R.id.playlist_action_edit -> {
                fCurrentPlaylist?.let { PlaylistEditActivity.startActivity(this, it.id) }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun openScore() {
        fCurrentPlaylist?.let { playlist ->
            val pos = fBinding.playlist.checkedItemPosition
            if (pos in playlist.songs.indices) {
                val song = playlist.songs[pos]
                if (song.scoreLink != null) {
                    sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_OPEN_SCORE))
                }
            }
        }
    }

    private fun loadPlaylist(playlist: Playlist) {
        val playlistView = fBinding.playlist
        supportActionBar?.title = playlist.name
        val data = ArrayList<Map<String, String>>()
        if (isInPictureInPictureMode) {
            playlist.songs.forEach { song ->
                val name = if (song.scoreLink != null) "${song.name}*" else song.name
                data.add(mapOf(kKEY_NAME to name))
            }

            playlistView.adapter = SimpleAdapter(
                this,
                data,
                R.layout.playlist_entry_name,
                arrayOf(kKEY_NAME),
                intArrayOf(R.id.playlist_entry_name_name)
            )
        } else {
            playlist.songs.forEach { song ->
                val name = if (song.scoreLink != null) "${song.name}*" else song.name
                val tempo = "${song.click.bpm}"
                data.add(mapOf(kKEY_NAME to name, kKEY_TEMPO to tempo))
            }

            playlistView.adapter = SimpleAdapter(
                this,
                data,
                R.layout.playlist_entry,
                arrayOf(kKEY_NAME, kKEY_TEMPO),
                intArrayOf(R.id.playlist_entry_name, R.id.playlist_entry_tempo)
            )
        }
    }

    private fun songChanged() {
        fStore.withCurrentSong { _, _, pos ->
            fBinding.playlist.setItemChecked(pos, true)
            scrollToPosition(pos)
        }
    }

    private fun scrollToPosition(position: Int) {
        val playlistView = fBinding.playlist
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
        sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_SELECT_SONG)
            .also {
                it.putExtra(
                    TimeKeeperApplication.kBROADCAST_ACTION_SELECT_SONG_EXTRA_SELECTION,
                    selection
                )
            })
    }

    private fun shouldOpenScoreOnNext() =
        isInMultiWindowMode

    companion object {
        private const val kKEY_NAME = "name"
        private const val kKEY_TEMPO = "tempo"

        fun startActivity(context: Context) =
            Intent(context, PlaylistActivity::class.java)
                .also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                .let(context::startActivity)
    }
}
