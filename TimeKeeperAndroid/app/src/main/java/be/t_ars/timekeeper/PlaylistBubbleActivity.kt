package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.Menu
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
class PlaylistBubbleActivity : AbstractPlaylistActivity(true) {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_actions, menu)
        menu.findItem(R.id.playlist_action_edit).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }
}