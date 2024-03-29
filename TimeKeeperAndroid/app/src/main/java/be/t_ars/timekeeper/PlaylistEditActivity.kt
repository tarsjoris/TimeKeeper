package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.EditText
import android.widget.SimpleAdapter
import be.t_ars.timekeeper.components.PlaylistList
import be.t_ars.timekeeper.components.SongList
import be.t_ars.timekeeper.data.*
import be.t_ars.timekeeper.databinding.PlaylisteditBinding
import kotlin.math.max
import kotlin.math.min


private const val kKEY_NAME = "name"
private const val kKEY_TEMPO = "tempo"

private fun parseTempo(tempo: CharSequence): Int {
    if (tempo.isBlank()) {
        return ClickDescription.DEFAULT_TEMPO
    }
    try {
        return min(300, max(30, Integer.parseInt(tempo.toString())))
    } catch (e: NumberFormatException) {
        Log.e("TimeKeeper", "Invalid tempo: " + e.message, e)
    }
    return ClickDescription.DEFAULT_TEMPO
}

private enum class EditMode {
    NORMAL, SEND_TOP, SEND_BOTTOM
}

class PlaylistEditActivity : AbstractActivity() {
    private lateinit var fBinding: PlaylisteditBinding
    private val fData: MutableList<Map<String, String>> = ArrayList()
    private val fAddSongDialog: InputDialog = InputDialog()
    private val fRenamePlaylistDialog: InputDialog = InputDialog()
    private val fCopyDialog: InputDialog = InputDialog()
    private val fDeleteDialog: ConfirmationDialog = ConfirmationDialog()
    private val fDeleteAndAboveDialog: ConfirmationDialog = ConfirmationDialog()
    private val fInsertfromPlaylistList = PlaylistList()
    private val fInsertfromSongList = SongList()

    private lateinit var fStore: PlaylistStore
    private var fPlaylist: Playlist? = null
    private var fActionMode: ActionMode? = null
    private var fPosition: Int = 0
    private var fNewPlaylistId: Long? = null
    private var fEditMode = EditMode.NORMAL
    private val fInsertFromPlaylists: MutableList<PlaylistHeader> = ArrayList()
    private val fInsertFromSongs: MutableList<Song> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fStore = PlaylistStore(this)

        fAddSongDialog.setOptions(
            null,
            { view ->
                val name = view.findViewById<EditText>(R.id.playlistedit_song_name).text
                val tempoS = view.findViewById<EditText>(R.id.playlistedit_song_tempo).text
                val tempo = parseTempo(tempoS)
                val song =
                    Song(
                        name.toString(),
                        ClickDescription(
                            tempo,
                            EClickType.DEFAULT,
                            ClickDescription.DEFAULT_DIVISION_COUNT,
                            ClickDescription.DEFAULT_BEAT_COUNT,
                            ClickDescription.DEFAULT_COUNT_OFF
                        )
                    )
                fPlaylist?.addSong(fStore, song)
                reloadSongs()
            },
            layoutInflater,
            R.string.playlistedit_action_addsong,
            R.drawable.ic_plus_dark,
            R.layout.playlistedit_song,
            R.string.add,
            R.string.cancel
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
                view.findViewById<EditText>(R.id.playlistedit_copy_name).setText(
                    fPlaylist?.name
                        ?: ""
                )
            },
            { view -> copyPlaylist(view.findViewById<EditText>(R.id.playlistedit_copy_name).text.toString()) },
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
        fDeleteAndAboveDialog.setOptions(
            this::deleteSelectedItemsAndAbove,
            R.string.playlistedit_delete_and_above_message,
            R.string.playlistedit_delete_yes,
            R.string.playlistedit_delete_no
        )

        fBinding = PlaylisteditBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fBinding.fabAddSong.setOnClickListener {
            fAddSongDialog.show(supportFragmentManager, "addsong")
        }

        fBinding.playlistedit.adapter = SimpleAdapter(
            this,
            fData,
            R.layout.playlist_entry,
            arrayOf(kKEY_NAME, kKEY_TEMPO),
            intArrayOf(R.id.playlist_entry_name, R.id.playlist_entry_tempo)
        )
        fBinding.playlistedit.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                if (fPlaylist != null && position >= 0 && position < (fPlaylist?.songs?.size
                        ?: 0)
                ) {
                    when (fEditMode) {
                        EditMode.NORMAL -> {
                            fPosition = position
                            editEntry()
                        }

                        EditMode.SEND_TOP -> {
                            fPlaylist?.sendToTop(fStore, position)
                            reloadSongs()
                        }

                        EditMode.SEND_BOTTOM -> {
                            fPlaylist?.sendToBottom(fStore, position)
                            reloadSongs()
                        }
                    }
                }
            }

        fBinding.playlistedit.setMultiChoiceModeListener(object :
            AbsListView.MultiChoiceModeListener {
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

                    R.id.playlistedit_action_delete_and_above -> {
                        fDeleteAndAboveDialog.show(supportFragmentManager, "delete_songs_and_above")
                    }

                    else -> {
                        return false
                    }
                }
                return true
            }

            override fun onItemCheckedStateChanged(
                mode: ActionMode,
                position: Int,
                id: Long,
                checked: Boolean
            ) {
            }
        })

        fBinding.playlisteditSelectfromClose.setOnClickListener {
            fBinding.playlisteditSelectfromLayout.visibility = View.INVISIBLE
        }
    }

    private fun copyPlaylist(name: String) {
        fPlaylist?.let { p ->
            fNewPlaylistId?.let { id ->
                val newPlaylist = Playlist(p, id, name, fStore.nextPlaylistWeight)
                fStore.addPlaylist(newPlaylist)
                fStore.setCurrentPlaylistID(newPlaylist.id)

                PlaylistActivity.startActivity(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getSettingScreenOrientation(this)
        loadIntent()
    }

    @Deprecated("Deprecated in Java")
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
                            val newClick =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    d.getSerializableExtra(
                                        TapSongActivity.kINTENT_DATA_CLICK,
                                        ClickDescription::class.java
                                    )
                                else
                                    d.getSerializableExtra(TapSongActivity.kINTENT_DATA_CLICK) as ClickDescription
                            val newScoreLink =
                                d.getStringExtra(TapSongActivity.kINTENT_DATA_SCORE_LINK)
                                    ?.let { it.ifBlank { null } }

                            val song = playlist.songs[fPosition]
                            val replaceName = newName != null && newName != song.name
                            val replaceClick = newClick != song.click
                            val replaceScoreLink = newScoreLink != song.scoreLink
                            if (replaceName || replaceClick || replaceScoreLink) {
                                if (replaceName && newName != null)
                                    song.name = newName
                                if (replaceClick && newClick != null)
                                    song.click = newClick
                                if (replaceScoreLink)
                                    song.scoreLink = newScoreLink
                                fStore.savePlaylist(playlist)
                                reloadSongs()
                            }
                        }
                    }
                }
            }

            kREQUEST_DOCUMENT_CODE -> fStore.acceptRequestedDocument(data) { id ->
                fNewPlaylistId = id
                val ft = supportFragmentManager.beginTransaction()
                ft.add(fCopyDialog, null)
                ft.commitAllowingStateLoss()
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
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
                startActivityForResult(fStore.createDocumentRequest(), kREQUEST_DOCUMENT_CODE)
            }

            R.id.playlistedit_action_insertfrom -> {
                startInsertFrom()
            }

            R.id.playlistedit_action_normalmode -> {
                fEditMode = EditMode.NORMAL
                reloadSongs()
            }

            R.id.playlistedit_action_sendtop -> {
                fEditMode = EditMode.SEND_TOP
                reloadSongs()
            }

            R.id.playlistedit_action_sendbottom -> {
                fEditMode = EditMode.SEND_BOTTOM
                reloadSongs()
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
        fEditMode = EditMode.NORMAL
        reloadName()
        reloadSongs()
    }

    private fun editEntry() {
        fPlaylist?.let { playlist ->
            val song = playlist.songs[fPosition]
            TapSongActivity.startActivityForResult(
                this,
                song.click,
                song.name,
                song.scoreLink,
                kREQUEST_TEMPO
            )
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
                    var name = song.displayName()
                    when (fEditMode) {
                        EditMode.SEND_TOP -> name = "\u2912 $name"
                        EditMode.SEND_BOTTOM -> name = "\u2913 $name"
                        else -> {}
                    }
                    val tempo = "${song.click.bpm}"
                    mapOf(kKEY_NAME to name, kKEY_TEMPO to tempo)
                }
            )
        }
        (fBinding.playlistedit.adapter as SimpleAdapter).notifyDataSetChanged()
    }

    private fun deleteSelectedItems() {
        val selection = fBinding.playlistedit.checkedItemPositions
        fData.indices.reversed().forEach { i ->
            if (selection.get(i)) {
                fPlaylist?.removeSong(fStore, i)
            }
        }
        fActionMode?.finish()
        reloadSongs()
    }

    private fun deleteSelectedItemsAndAbove() {
        val selection = fBinding.playlistedit.checkedItemPositions
        val highestSelection = fData.indices.reversed().firstOrNull(selection::get)
        if (highestSelection != null) {
            fPlaylist?.removeSongAndAbove(fStore, highestSelection)
            fActionMode?.finish()
            reloadSongs()
        }
    }

    private fun move(up: Boolean) {
        val selection = fBinding.playlistedit.checkedItemPositions
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
                        fBinding.playlistedit.setItemChecked(i, false)
                        fBinding.playlistedit.setItemChecked(otherIndex, true)
                    }
                    fPlaylist?.move(fStore, i, up)
                }
            } else {
                ignore = false
            }
            i += if (up) 1 else -1
        }
        reloadSongs()
    }

    private fun startInsertFrom() {
        fInsertfromPlaylistList.bindToView(this, fBinding.playlisteditSelectfromList)

        fInsertFromPlaylists.clear()
        fInsertFromPlaylists.addAll(fStore.readAllPlaylists())
        fInsertfromPlaylistList.setPlaylists(fInsertFromPlaylists)

        fBinding.playlisteditSelectfromLayout.visibility = View.VISIBLE

        fBinding.playlisteditSelectfromList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                loadInsertFromPlaylist(position)
            }
    }

    private fun loadInsertFromPlaylist(selection: Int) {
        val playlist = fStore.readPlaylist(fInsertFromPlaylists[selection].id)
        if (playlist != null) {
            fInsertfromSongList.bindToView(this, fBinding.playlisteditSelectfromList)

            fInsertFromSongs.clear()
            fInsertFromSongs.addAll(playlist.songs)
            fInsertfromSongList.setSongs(fInsertFromSongs)

            fBinding.playlisteditSelectfromList.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    fBinding.playlisteditSelectfromList.setItemChecked(position, true)

                    val song = fInsertFromSongs[position]
                    fPlaylist?.addSong(fStore, Song(song))
                    reloadSongs()

                    fPlaylist?.songs?.size?.also {
                        val pos = it - 1
                        //fBinding.playlistedit.setItemChecked(pos, true)
                        fBinding.playlistedit.smoothScrollToPosition(pos)
                    }
                }
        }
    }

    companion object {
        private const val kINTENT_DATA_PLAYLIST_ID = "playlist-id"

        private const val kREQUEST_TEMPO = 1
        private const val kREQUEST_DOCUMENT_CODE = 2

        fun startActivity(context: Context, playlistID: Long) =
            Intent(context, PlaylistEditActivity::class.java)
                .also { intent ->
                    intent.putExtra(kINTENT_DATA_PLAYLIST_ID, playlistID)
                }
                .let(context::startActivity)
    }
}
