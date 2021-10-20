package be.t_ars.timekeeper

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.bubble.*
import kotlinx.android.synthetic.main.playlist_entry.*

class BubbleActivity : AbstractActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.bubble)

        bubble_start.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_START_METRONOME))
        }
        bubble_stop.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_STOP_METRONOME))
        }
        bubble_next.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG).also {
                it.putExtra(
                    TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG_EXTRA_OPEN_SCORE,
                    true
                )
            })
        }
        bubble_playlist.setOnClickListener {
            PlaylistActivity.startActivity(this)
        }
    }

    override fun onResume() {
        super.onResume()

        PlaylistState.withCurrentSong { _, song, _ ->
            val name = if (song.scoreLink != null) "${song.name}*" else song.name
            val tempo = if (song.tempo != null) "${song.tempo}" else "-"
            playlist_entry_name.text = name
            playlist_entry_tempo.text = tempo
        }
    }
}