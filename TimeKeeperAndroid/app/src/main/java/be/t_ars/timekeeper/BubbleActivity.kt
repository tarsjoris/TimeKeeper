package be.t_ars.timekeeper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import kotlinx.android.synthetic.main.bubble.*
import kotlinx.android.synthetic.main.playlist_entry.*

class BubbleActivity : AbstractActivity() {
    private var fAutoPlay = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.bubble)

        bubble_start.setOnClickListener {
            startMetronome()
        }
        bubble_stop.setOnClickListener {
            SoundService.stopSound(this)
        }
        bubble_next.setOnClickListener {
            doNext()
        }
        bubble_playlist.setOnClickListener {
            showPlaylist()
        }
    }

    override fun onResume() {
        super.onResume()
        fAutoPlay = getBoolPreference(this, kAUTOPLAY, true)
        loadIntent()
    }

    private fun loadIntent() {
        PlaylistState.withCurrentSong { _, song, _ ->
            val name = if (song.scoreLink != null) "${song.name}*" else song.name
            val tempo = if (song.tempo != null) "${song.tempo}" else "-"
            playlist_entry_name.text = name
            playlist_entry_tempo.text = tempo
        }
    }

    private fun doNext() {
        PlaylistState.withCurrentSong { playlist, _, pos ->
            if (pos < playlist.songs.size - 1) {
                val newPos = pos + 1
                PlaylistState.currentPos = newPos
                if (fAutoPlay) {
                    startMetronome()
                } else {
                    SoundService.stopSound(this)
                }
                val scoreLink = playlist.songs[newPos].scoreLink
                if (scoreLink != null) {
                    openLink(scoreLink)
                } else {
                    showPlaylist()
                }
            }
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

    private fun openLink(link: String) {
        val openURL = Intent(Intent.ACTION_VIEW)
            .also {
                it.data = Uri.parse(link)
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        startActivity(openURL)
    }

    private fun showPlaylist() {
        val playlistIntent = Intent(this, PlaylistActivity::class.java)
                .also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
        startActivity(playlistIntent)
    }
}