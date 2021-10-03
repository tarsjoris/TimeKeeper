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

@RequiresApi(Build.VERSION_CODES.P)
class PlaylistActivity : AbstractPlaylistActivity(false) {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_actions, menu)
        return super.onCreateOptionsMenu(menu)
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
