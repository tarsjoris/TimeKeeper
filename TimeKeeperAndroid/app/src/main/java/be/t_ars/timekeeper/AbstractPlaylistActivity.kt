package be.t_ars.timekeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.SimpleAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.playlist.*
import java.util.*

abstract class AbstractPlaylistActivity : AbstractActivity() {
    private inner class TimeKeeperBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(content: Context?, intent: Intent?) {
            if (intent?.action == TimeKeeperApplication.kBROADCAST_EVENT_SONG_CHANGED) {
                songChanged()
            }
        }
    }

    private val fBroadcastReceiver = TimeKeeperBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.playlist)
        setSupportActionBar(toolbar)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val playlistView = playlist
        playlistView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> trigger(position) }
        playlistView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            PlaylistState.withCurrentSong { _, _, pos ->
                scrollToPosition(pos)
            }
        }
        button_start.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_START_METRONOME))
        }
        button_stop.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_STOP_METRONOME))
        }
        button_next.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG).also {
                it.putExtra(
                    TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG_EXTRA_OPEN_SCORE,
                    shouldOpenScoreOnNext()
                )
            })
        }

        updateView()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            fBroadcastReceiver,
            IntentFilter(TimeKeeperApplication.kBROADCAST_EVENT_SONG_CHANGED)
        )
    }

    protected abstract fun shouldOpenScoreOnNext(): Boolean

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fBroadcastReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation =
            getIntPreference(this, kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)

        updateView()

        loadPlaylist()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.playlist_open_score -> {
                openScore()
            }
            R.id.playlist_action_edit -> {
                PlaylistState.withCurrentPlaylist { playlist, _ ->
                    PlaylistEditActivity.startActivity(this, playlist.id)
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

    protected open fun updateView() {
    }

    private fun openScore() {
        PlaylistState.withCurrentSong { _, song, _ ->
            if (song.scoreLink != null) {
                willOpenScore()
                sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_OPEN_SCORE))
            }
        }
    }

    protected open fun willOpenScore() {
    }

    private fun loadPlaylist() {
        val playlistView = playlist
        PlaylistState.withCurrentPlaylist { playlist, pos ->
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
                    val tempo = if (song.tempo != null) "${song.tempo}" else "-"
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

            if (pos != null && pos in playlist.songs.indices) {
                playlistView.setItemChecked(pos, true)
                playlistView.post { scrollToPosition(pos) }
            }
        }
    }

    protected fun songChanged() {
        PlaylistState.withCurrentSong { _, _, pos ->
            playlist.setItemChecked(pos, true)
            scrollToPosition(pos)
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
        sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_SELECT_SONG)
            .also {
                it.putExtra(
                    TimeKeeperApplication.kBROADCAST_ACTION_SELECT_SONG_EXTRA_SELECTION,
                    selection
                )
            })
    }

    companion object {
        private const val kKEY_NAME = "name"
        private const val kKEY_TEMPO = "tempo"
    }
}
