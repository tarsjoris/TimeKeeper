package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.SimpleAdapter
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.playlist.*
import java.util.*

class PlaylistActivity : AbstractActivity() {
    private lateinit var fBubbleManager: BubbleManager
    private var fAutoPlay = true

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fBubbleManager = BubbleManager(this)

        setContentView(R.layout.playlist)
        setSupportActionBar(toolbar)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val playlistView = playlist
        playlistView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> trigger(position) }
        button_start.setOnClickListener {
            startMetronome()
        }
        button_stop.setOnClickListener {
            SoundService.stopSound(this)
        }
        button_next.setOnClickListener {
            doNext()
        }
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getIntPreference(this, kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
        fAutoPlay = getBoolPreference(this, kAUTOPLAY, true)
        loadPlaylist()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.P)
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun openScore() {
        PlaylistState.withCurrentSong { _, song, _ ->
            fBubbleManager.showBubble()
            song.scoreLink?.also(this::openLink)
        }
    }

    private fun openLink(link: String) {
        val openURL = Intent(Intent.ACTION_VIEW)
        openURL.data = Uri.parse(link)
        startActivity(openURL)
    }

    private fun loadPlaylist() {
        val playlistView = playlist
        PlaylistState.withCurrentPlaylist { playlist, pos ->
            supportActionBar?.title = playlist.name
            val data = ArrayList<Map<String, String>>()
            playlist.songs.forEach { song ->
                val name = if (song.scoreLink != null) "${song.name}*" else song.name
                val tempo = if (song.tempo != null) "${song.tempo}" else "-"
                data.add(mapOf(kKEY_NAME to name, kKEY_TEMPO to tempo))
            }

            playlistView.adapter = SimpleAdapter(this,
                    data,
                    R.layout.playlist_entry,
                    arrayOf(kKEY_NAME, kKEY_TEMPO),
                    intArrayOf(R.id.playlist_entry_name, R.id.playlist_entry_tempo))

            if (pos != null && pos in playlist.songs.indices) {
                playlistView.setItemChecked(pos, true)
                playlistView.post { scrollToPosition(pos) }
            }
        }
    }


    private fun doNext() {
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

                playlistView.setItemChecked(newPos, true)
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
                SoundService.startSound(this, song.name, tempo, PlaylistActivity::class.java)
            } else {
                SoundService.stopSound(this)
            }
        }
    }

    companion object {
        private const val kKEY_NAME = "name"
        private const val kKEY_TEMPO = "tempo"

        fun startActivity(context: Context) =
                Intent(context, PlaylistActivity::class.java)
                        .let(context::startActivity)
    }
}
