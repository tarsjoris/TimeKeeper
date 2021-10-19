package be.t_ars.timekeeper

import android.view.Menu

class PlaylistBubbleActivity : AbstractPlaylistActivity() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_actions, menu)
        menu.findItem(R.id.playlist_action_edit).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun didNext() {
        openScore()
    }
}