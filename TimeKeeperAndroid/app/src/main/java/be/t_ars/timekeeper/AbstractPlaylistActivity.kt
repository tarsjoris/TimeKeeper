package be.t_ars.timekeeper

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.SimpleAdapter
import kotlinx.android.synthetic.main.playlist.*
import java.util.*

abstract class AbstractPlaylistActivity : AbstractActivity() {
    private var fAutoPlay = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.playlist)
        setSupportActionBar(toolbar)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val playlistView = playlist
        playlistView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> trigger(position) }
        button_start.setOnClickListener {
            startMetronome()
        }
        button_stop.setOnClickListener {
            SoundService.stopSound(this)
        }
        button_next.setOnClickListener {
            doNext()
        }

        updateView()
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation =
            getIntPreference(this, kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
        fAutoPlay = getBoolPreference(this, kAUTOPLAY, true)

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

    protected fun openScore() {
        PlaylistState.withCurrentSong { _, song, _ ->
            willOpenScore()
            song.scoreLink?.also(this::openLink)
        }
    }

    protected open fun willOpenScore() {
    }

    private fun openLink(link: String) {
        val openURL = Intent(Intent.ACTION_VIEW)
            .apply {
                data = Uri.parse(link)
                flags = if (isInMultiWindowMode) {
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK
                } else {
                    Intent.FLAG_ACTIVITY_TASK_ON_HOME or Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        startActivity(openURL)
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


    protected fun doNext() {
        val playlistView = playlist
        PlaylistState.withCurrentSong { playlist, _, pos ->
            if (pos < playlist.songs.size - 1) {
                val newPos = pos + 1
                PlaylistState.currentPos = newPos

                if (fAutoPlay) {
                    startMetronome()
                } else {
                    SoundService.stopSound(this)
                }

                didNext()

                playlistView.setItemChecked(newPos, true)
                scrollToPosition(newPos)
            }
        }
    }

    protected abstract fun didNext()

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
        PlaylistState.currentPos = selection
        if (fAutoPlay) {
            startMetronome()
        } else {
            SoundService.stopSound(this)
        }
    }

    private fun startMetronome() {
        PlaylistState.withCurrentSong { _, song, _ ->
            val tempo = song.tempo
            if (tempo != null) {
                SoundService.startSound(
                    this,
                    song.name,
                    tempo,
                    AbstractPlaylistActivity::class.java
                )
            } else {
                SoundService.stopSound(this)
            }
        }
    }

    companion object {
        private const val kKEY_NAME = "name"
        private const val kKEY_TEMPO = "tempo"
    }
}
