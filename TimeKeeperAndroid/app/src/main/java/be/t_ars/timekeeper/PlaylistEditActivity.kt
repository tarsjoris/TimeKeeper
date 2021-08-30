package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.EditText
import android.widget.SimpleAdapter
import be.t_ars.timekeeper.data.IPlaylistStore
import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.data.PlaylistStoreFactory
import be.t_ars.timekeeper.data.Song
import kotlinx.android.synthetic.main.playlistedit.*
import java.util.*
import kotlin.math.max
import kotlin.math.min


private const val kKEY_NAME = "name"
private const val kKEY_TEMPO = "tempo"

private fun parseTempo(tempo: CharSequence): Int? {
    if (tempo.isBlank()) {
        return null
    }
    try {
        return min(300, max(30, Integer.parseInt(tempo.toString())))
    } catch (e: NumberFormatException) {
        Log.e("TimeKeeper", "Invalid tempo: " + e.message, e)
    }
    return null
}


class PlaylistEditActivity : AbstractActivity() {

    private val fData: MutableList<Map<String, String>> = ArrayList()
    private val fAddSongDialog: InputDialog = InputDialog()
    private val fRenamePlaylistDialog: InputDialog = InputDialog()
    private val fCopyDialog: InputDialog = InputDialog()
    private val fDeleteDialog: ConfirmationDialog = ConfirmationDialog()
    private val fStore: IPlaylistStore = PlaylistStoreFactory.createStore(this)
    private var fPlaylist: Playlist? = null
    private var fActionMode: ActionMode? = null
    private var fPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fAddSongDialog.setOptions(null,
                { view ->
                    val name = view.findViewById<EditText>(R.id.playlistedit_song_name).text
                    val tempoS = view.findViewById<EditText>(R.id.playlistedit_song_tempo).text
                    val tempo = parseTempo(tempoS)
                    val song = Song(name = name.toString(), tempo = tempo)
                    fPlaylist?.addSong(fStore, song)
                    reloadSongs()
                },
                layoutInflater,
                R.string.playlistedit_action_addsong,
                R.drawable.ic_plus_dark,
                R.layout.playlistedit_song,
                R.string.playlistedit_addsong_add,
                R.string.playlistedit_addsong_cancel
        )
        fRenamePlaylistDialog.setOptions(
                { view ->
                    view.findViewById<EditText>(R.id.playlistedit_renameplaylist_name)
                            .setText(fPlaylist?.name ?: "")
                },
                { view ->
                    val name = view.findViewById<EditText>(R.id.playlistedit_renameplaylist_name).text
                    fPlaylist?.let {
                        it.name = name.toString()
                        fStore.storePlaylistHeader(it)
                        reloadName()
                    }
                },
                layoutInflater,
                R.string.playlistedit_action_renameplaylist,
                R.drawable.ic_pencil,
                R.layout.playlistedit_renameplaylist,
                R.string.playlistedit_renameplaylist_save,
                R.string.playlistedit_renameplaylist_cancel
        )
        fCopyDialog.setOptions(
                { view ->
                    view.findViewById<EditText>(R.id.playlistedit_copy_name).setText(fPlaylist?.name
                            ?: "")
                },
                { view ->
                    fPlaylist?.let { p ->
                        val name = view.findViewById<EditText>(R.id.playlistedit_copy_name).text
                        val newPlaylist = Playlist(p, name.toString(), fStore.nextPlaylistWeight)
                        fStore.addPlaylist(newPlaylist)

                        PlaylistActivity.startActivity(this, newPlaylist.id)
                    }
                },
                layoutInflater,
                R.string.playlistedit_action_copy,
                R.drawable.ic_content_copy,
                R.layout.playlistedit_copy,
                R.string.playlistedit_copy_copy,
                R.string.playlistedit_copy_cancel
        )
        fDeleteDialog.setOptions(
                this::deleteSelectedItems,
                R.string.playlistedit_delete_message,
                R.string.playlistedit_delete_yes,
                R.string.playlistedit_delete_no
        )

        setContentView(R.layout.playlistedit)
        setSupportActionBar(toolbar)

        fab_add_song.setOnClickListener {
            fAddSongDialog.show(supportFragmentManager, "addsong")
        }

        playlistedit.adapter = SimpleAdapter(this, fData, R.layout.playlist_entry, arrayOf(kKEY_NAME, kKEY_TEMPO), intArrayOf(R.id.playlist_entry_name, R.id.playlist_entry_tempo))
        playlistedit.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (fPlaylist != null && position >= 0 && position < fPlaylist?.songs?.size ?: 0) {
                fPosition = position
                editEntry()
            }
        }

        playlistedit.setMultiChoiceModeListener(object : AbsListView.MultiChoiceModeListener {
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                fActionMode = null
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                fActionMode = mode
                mode.menuInflater.inflate(R.menu.playlistedit_contextactions, menu)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.playlistedit_action_up -> {
                        move(true)
                    }
                    R.id.playlistedit_action_down -> {
                        move(false)
                    }
                    R.id.playlistedit_action_delete -> {
                        fDeleteDialog.show(supportFragmentManager, "delete_songs")
                    }
                    else -> {
                        return false
                    }
                }
                return true
            }

            override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {}
        })
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getIntPreference(this, kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
        loadIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            kREQUEST_TEMPO -> {
                if (resultCode != RESULT_OK) {
                    return
                }
                data?.let { d ->
                    fPlaylist?.let { playlist ->
                        if (fPosition in 0 until playlist.songs.size) {
                            val newName = d.getStringExtra(TapSongActivity.kINTENT_DATA_NAME)
                            val newTempo = d.getIntExtra(TapSongActivity.kINTENT_DATA_TEMPO, -1)
                                    .let { if (it == -1) null else it }
                            val newScoreLink = d.getStringExtra(TapSongActivity.kINTENT_DATA_SCORE_LINK)
                                    ?.let { if (it.isBlank()) null else it }

                            val song = playlist.songs[fPosition]
                            val replaceName = newName != null && newName != song.name
                            val replaceTempo = newTempo != song.tempo
                            val replaceScoreLink = newScoreLink != song.scoreLink
                            if (replaceName || replaceTempo || replaceScoreLink) {
                                if (replaceName && newName != null)
                                    song.name = newName
                                if (replaceTempo)
                                    song.tempo = newTempo
                                if (replaceScoreLink)
                                    song.scoreLink = newScoreLink
                                fStore.storeSong(playlist, song)
                                reloadSongs()
                            }
                        }
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        fStore.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlistedit_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        when (item.itemId) {
            R.id.playlistedit_action_renameplaylist -> {
                fRenamePlaylistDialog.show(supportFragmentManager, "renameplaylist")
            }
            R.id.playlistedit_action_copy -> {
                fCopyDialog.show(supportFragmentManager, "copyplaylist")
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun loadIntent() {
        val bundle = intent.extras
        if (bundle != null) {
            val playlist = bundle.getLong(kINTENT_DATA_PLAYLIST_ID)
            val p = fPlaylist
            if (p == null || p.id != playlist) {
                fPlaylist = fStore.readPlaylist(playlist)
                loadPlaylist()
            }
        } else {
            fPlaylist = null
            loadPlaylist()
        }
    }

    private fun loadPlaylist() {
        reloadName()
        reloadSongs()
    }

    private fun editEntry() {
        fPlaylist?.let { playlist ->
            val song = playlist.songs[fPosition]
            TapSongActivity.startActivityForResult(this, song.tempo, song.name, song.scoreLink, kREQUEST_TEMPO)
        }
    }

    private fun reloadName() {
        supportActionBar?.title = fPlaylist?.name ?: "<no playlist>"
    }

    private fun reloadSongs() {
        fData.clear()
        fPlaylist?.let { playlist ->
            fData.addAll(
                    playlist.songs.map { song ->
                        val tempo = if (song.tempo != null) "${song.tempo}" else "-"
                        mapOf(kKEY_NAME to song.name, kKEY_TEMPO to tempo)
                    }
            )
        }
        (playlistedit.adapter as SimpleAdapter).notifyDataSetChanged()
    }

    private fun deleteSelectedItems() {
        val selection = playlistedit.checkedItemPositions
        fData.indices.reversed().forEach { i ->
            if (selection.get(i)) {
                fPlaylist?.removeSong(fStore, i)
            }
        }
        fActionMode?.finish()
        reloadSongs()
    }

    private fun move(up: Boolean) {
        val selection = playlistedit.checkedItemPositions
        var ignore = false
        var i = if (up) 0 else fData.size - 1
        while (if (up) i < fData.size else i >= 0) {
            if (selection.get(i)) {
                if (if (up) i <= 0 else i >= fData.size - 1) {
                    // leave the first selection group alone
                    ignore = true
                } else if (!ignore) {
                    val otherIndex = if (up) i - 1 else i + 1
                    if (!selection.get(otherIndex)) {
                        playlistedit.setItemChecked(i, false)
                        playlistedit.setItemChecked(otherIndex, true)
                    }
                    fPlaylist!!.move(fStore, i, up)
                }
            } else {
                ignore = false
            }
            i += if (up) 1 else -1
        }
        reloadSongs()
    }

    companion object {
        private const val kINTENT_DATA_PLAYLIST_ID = "playlist-id"

        private const val kREQUEST_TEMPO = 1

        fun startActivity(context: Context, playlistID: Long) =
                Intent(context, PlaylistEditActivity::class.java)
                        .also { intent ->
                            intent.putExtra(kINTENT_DATA_PLAYLIST_ID, playlistID)
                        }
                        .let(context::startActivity)
    }
}
