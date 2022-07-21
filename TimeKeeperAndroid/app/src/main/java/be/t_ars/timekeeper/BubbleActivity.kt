package be.t_ars.timekeeper

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import be.t_ars.timekeeper.databinding.BubbleBinding

@RequiresApi(Build.VERSION_CODES.P)
class BubbleActivity : AbstractActivity() {
    private lateinit var fBinding: BubbleBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = BubbleBinding.inflate(layoutInflater)
        setContentView(fBinding.root)

        fBinding.bubbleStart.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_START_METRONOME))
        }
        fBinding.bubbleStop.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_STOP_METRONOME))
        }
        fBinding.bubbleNext.setOnClickListener {
            sendBroadcast(Intent(TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG).also {
                it.putExtra(
                    TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG_EXTRA_OPEN_SCORE,
                    true
                )
            })
        }
        fBinding.bubblePlaylist.setOnClickListener {
            PlaylistActivity.startActivity(this)
        }
    }

    override fun onResume() {
        super.onResume()

        PlaylistState.withCurrentSong { _, song, _ ->
            val name = if (song.scoreLink != null) "${song.name}*" else song.name
            val tempo = if (song.tempo != null) "${song.tempo}" else "-"
            fBinding.included.playlistEntryName.text = name
            fBinding.included.playlistEntryTempo.text = tempo
        }
    }
}