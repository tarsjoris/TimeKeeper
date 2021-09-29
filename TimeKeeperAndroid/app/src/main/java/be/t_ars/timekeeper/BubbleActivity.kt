package be.t_ars.timekeeper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.data.PlaylistStore
import kotlinx.android.synthetic.main.bubble.*
import java.io.Serializable
import java.util.*

class BubbleActivity : AbstractActivity() {
    private lateinit var fStore: PlaylistStore
    private var fAutoPlay = true
    private var fPlaylist: Playlist? = null
    private var fPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fStore = PlaylistStore(this)

        setContentView(R.layout.bubble)

        bubble_start.setOnClickListener {
            fPlaylist?.let { startMetronome(it, fPos) }
        }
        bubble_stop.setOnClickListener {
            SoundService.stopSound(this)
        }
        bubble_next.setOnClickListener {
            doNext()
        }
        bubble_playlist.setOnClickListener {
            fPlaylist?.let { p ->
                showPlaylist(p)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fAutoPlay = getBoolPreference(this, kAUTOPLAY, true)
        loadIntent()
    }

    private fun loadIntent() {
        val extras = intent.extras
        val oldPlaylist = fPlaylist
        when {
            extras != null -> {
                val playlistID = extras.getLong(kINTENT_DATA_PLAYLIST_ID)
                fPlaylist = fStore.readPlaylist(playlistID)
                fPos = extras.getInt(kINTENT_DATA_POSITION, fPos)
            }
            oldPlaylist != null -> {
                fPlaylist = fStore.readPlaylist(oldPlaylist.id)
            }
            else -> {
                fPlaylist = null
            }
        }
    }

    private fun doNext() {
        fPlaylist?.let { p ->
            if (fPos < p.songs.size - 1) {
                ++fPos
                if (fAutoPlay) {
                    startMetronome(p, fPos)
                } else {
                    SoundService.stopSound(this)
                }
                val scoreLink = p.songs[fPos].scoreLink
                if (scoreLink != null) {
                    openLink(scoreLink)
                } else {
                    showPlaylist(p)
                }
            }
        }
    }

    private fun startMetronome(p: Playlist, pos: Int) {
        val song = p.songs[pos]
        val tempo = song.tempo
        if (tempo != null) {
            val extras = HashMap<String, Serializable>().also {
                it[PlaylistActivity.kINTENT_DATA_PLAYLIST_ID] = p.id
                it[PlaylistActivity.kINTENT_DATA_POSITION] = pos
            }
            SoundService.startSound(this, song.name, tempo, PlaylistActivity::class.java, extras)
        } else {
            SoundService.stopSound(this)
        }
    }

    private fun openLink(link: String) {
        val openURL = Intent(Intent.ACTION_VIEW)
        openURL.data = Uri.parse(link)
        startActivity(openURL)
    }

    private fun showPlaylist(playlist: Playlist) {
        val playlistIntent = Intent(this, PlaylistActivity::class.java)
                .also {
                    it.putExtra(PlaylistActivity.kINTENT_DATA_PLAYLIST_ID, playlist.id)
                    it.putExtra(PlaylistActivity.kINTENT_DATA_POSITION, fPos)
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
        startActivity(playlistIntent)
    }

    companion object {
        const val kINTENT_DATA_PLAYLIST_ID = "playlist-id"
        const val kINTENT_DATA_POSITION = "position"
    }
}