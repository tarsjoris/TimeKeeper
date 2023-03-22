package be.t_ars.timekeeper

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import be.t_ars.timekeeper.data.PlaylistStore

class TimeKeeperApplication : Application() {
    private inner class TimeKeeperBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(content: Context?, intent: Intent?) {
            when (intent?.action) {
                kBROADCAST_ACTION_NEXT_SONG -> doNextSong(
                    intent.getBooleanExtra(kBROADCAST_ACTION_NEXT_SONG_EXTRA_OPEN_SCORE, false)
                )
                kBROADCAST_ACTION_SELECT_SONG -> selectSong(
                    intent.getIntExtra(
                        kBROADCAST_ACTION_SELECT_SONG_EXTRA_SELECTION,
                        0
                    )
                )
                kBROADCAST_ACTION_START_METRONOME -> startMetronome()
                kBROADCAST_ACTION_STOP_METRONOME -> stopMetronome()
                kBROADCAST_ACTION_OPEN_SCORE -> openScore()
            }
        }
    }

    private val fBroadcastReceiver = TimeKeeperBroadcastReceiver()
    private val fStore = PlaylistStore(this)

    override fun onCreate() {
        super.onCreate()
        registerReceiver(fBroadcastReceiver, IntentFilter().also {
            it.addAction(kBROADCAST_ACTION_NEXT_SONG)
            it.addAction(kBROADCAST_ACTION_SELECT_SONG)
            it.addAction(kBROADCAST_ACTION_START_METRONOME)
            it.addAction(kBROADCAST_ACTION_STOP_METRONOME)
            it.addAction(kBROADCAST_ACTION_OPEN_SCORE)
        })
    }

    override fun onTerminate() {
        unregisterReceiver(fBroadcastReceiver)
        super.onTerminate()
    }

    private fun doNextSong(openScore: Boolean) {
        fStore.withCurrentSong { playlist, _, pos ->
            if (pos < playlist.songs.size - 1) {
                val newPos = pos + 1
                fStore.setCurrentSongIndex(newPos)

                songChanged()

                if (openScore) {
                    openScore()
                }
            }
        }
    }

    private fun selectSong(selection: Int) {
        fStore.setCurrentSongIndex(selection)
        songChanged()
    }

    private fun songChanged() {
        if (getSettingAutoplay(this)) {
            startMetronome()
        } else {
            SoundService.stopSound(this)
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(kBROADCAST_EVENT_SONG_CHANGED)
        )
    }

    private fun startMetronome() {
        fStore.withCurrentSong { _, song, _ ->
            val tempo = song.tempo
            if (tempo != null) {
                SoundService.startSound(
                    this,
                    song.name,
                    tempo,
                    PlaylistActivity::class.java
                )
            } else {
                SoundService.stopSound(this)
            }
        }
    }

    private fun stopMetronome() {
        SoundService.stopSound(this)
    }

    private fun openScore() {
        fStore.withCurrentSong { _, song, _ ->
            song.scoreLink?.also(this::openLink)
        }
    }

    private fun openLink(link: String) {
        val openURL = Intent(Intent.ACTION_VIEW)
            .apply {
                data = Uri.parse(link)
                flags = Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        startActivity(openURL)
    }

    companion object {
        const val kBROADCAST_ACTION_NEXT_SONG = "next-song"
        const val kBROADCAST_ACTION_NEXT_SONG_EXTRA_OPEN_SCORE = "open-score"

        const val kBROADCAST_ACTION_SELECT_SONG = "select-song"
        const val kBROADCAST_ACTION_SELECT_SONG_EXTRA_SELECTION = "selection"

        const val kBROADCAST_ACTION_START_METRONOME = "start-metronome"

        const val kBROADCAST_ACTION_STOP_METRONOME = "stop-metronome"

        const val kBROADCAST_ACTION_OPEN_SCORE = "open-score"

        const val kBROADCAST_EVENT_SONG_CHANGED = "song-changed"
    }
}