package be.t_ars.timekeeper

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.playlist.*

@RequiresApi(Build.VERSION_CODES.P)
class PlaylistActivity : AbstractPlaylistActivity() {
    private inner class PIPBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(content: Context?, intent: Intent?) {
            if (intent?.action == kBROADCAST_ACTION_NEXT && isInPictureInPictureMode) {
                doNext()
            }
        }
    }

    private val fBubbleManager: BubbleManager by lazy { BubbleManager(this) }
    private val fBroadcastReceiver = PIPBroadcastReceiver()


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(fBroadcastReceiver, IntentFilter(kBROADCAST_ACTION_NEXT))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(fBroadcastReceiver)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        updateView()
    }

    override fun updateView() {
        if (isInPictureInPictureMode) {
            appbar.visibility = View.GONE
            buttons.visibility = View.GONE
        } else {
            appbar.visibility = View.VISIBLE
            buttons.visibility = View.VISIBLE
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

    override fun didNext() {
        if (isInMultiWindowMode || isInPictureInPictureMode) {
            openScore()
        }
    }

    private fun switchToPictureInPictureMode() {
        val nextIntent = Intent(kBROADCAST_ACTION_NEXT)
        val nextPendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                nextIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setActions(
                    listOf(
                        RemoteAction(
                            Icon.createWithResource(this, R.drawable.skip_next),
                            "Next",
                            "Skip to next song",
                            nextPendingIntent
                        )
                    )
                )
                .build()
        )
    }

    companion object {
        private const val kBROADCAST_ACTION_NEXT = "next"

        fun startActivity(context: Context) =
            Intent(context, PlaylistActivity::class.java)
                .also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                .let(context::startActivity)
    }
}
