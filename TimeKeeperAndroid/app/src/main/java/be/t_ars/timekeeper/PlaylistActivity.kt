package be.t_ars.timekeeper

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.view.Menu
import android.view.View
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
class PlaylistActivity : AbstractPlaylistActivity() {
    private val fBubbleManager: BubbleManager by lazy { BubbleManager(this) }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        updateView()
    }

    override fun updateView() {
        if (isInPictureInPictureMode) {
            fBinding.appbar.visibility = View.GONE
            fBinding.buttons.visibility = View.GONE
        } else {
            fBinding.appbar.visibility = View.VISIBLE
            fBinding.buttons.visibility = View.VISIBLE
        }
    }

    override fun willOpenScore() {
        if (!isInMultiWindowMode && !isInPictureInPictureMode) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                switchToPictureInPictureMode()
            } else {
                fBubbleManager.showBubble()
            }
        }
    }

    override fun shouldOpenScoreOnNext() =
        isInMultiWindowMode || isInPictureInPictureMode

    private fun switchToPictureInPictureMode() {
        val startMetronomeIntent = Intent(TimeKeeperApplication.kBROADCAST_ACTION_START_METRONOME)
        val startMetronomePendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                startMetronomeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        val stopMetronomeIntent = Intent(TimeKeeperApplication.kBROADCAST_ACTION_STOP_METRONOME)
        val stopMetronomePendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                stopMetronomeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        val nextSongIntent = Intent(TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG).also {
            it.putExtra(TimeKeeperApplication.kBROADCAST_ACTION_NEXT_SONG_EXTRA_OPEN_SCORE, true)
        }
        val nextSongPendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                nextSongIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setActions(
                    listOf(
                        RemoteAction(
                            Icon.createWithResource(this, R.drawable.start),
                            "Start",
                            "Start metronome",
                            startMetronomePendingIntent
                        ),
                        RemoteAction(
                            Icon.createWithResource(this, R.drawable.stop),
                            "Stop",
                            "Stop metronome",
                            stopMetronomePendingIntent
                        ),
                        RemoteAction(
                            Icon.createWithResource(this, R.drawable.skip_next),
                            "Next",
                            "Skip to next song",
                            nextSongPendingIntent
                        )
                    )
                )
                .build()
        )
    }

    companion object {
        fun startActivity(context: Context) =
            Intent(context, PlaylistActivity::class.java)
                .also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                .let(context::startActivity)
    }
}
