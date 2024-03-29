package be.t_ars.timekeeper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import be.t_ars.timekeeper.components.PlaylistList
import be.t_ars.timekeeper.data.Playlist
import be.t_ars.timekeeper.data.PlaylistHeader
import be.t_ars.timekeeper.data.PlaylistStore
import be.t_ars.timekeeper.databinding.OverviewBinding


class OverviewActivity : AbstractActivity() {
    private lateinit var fBinding: OverviewBinding
    private val fPlaylistList = PlaylistList()
    private val fAddPlaylistDialog = InputDialog()
    private val fDeleteDialog = ConfirmationDialog().apply {
        setOptions(
            ::deleteSelectedItems,
            R.string.overview_delete_message,
            R.string.overview_delete_yes,
            R.string.overview_delete_no
        )
    }
    private val fStore = PlaylistStore(this)
    private val fPlaylists: MutableList<PlaylistHeader> = ArrayList()
    private var fActionMode: ActionMode? = null
    private var fNewPlaylistId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = OverviewBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fBinding.fabAddPlaylist.setOnClickListener {
            startActivityForResult(fStore.createDocumentRequest(), kREQUEST_DOCUMENT_CODE)
        }

        fAddPlaylistDialog.setOptions(
            null,
            { view -> addPlaylist(view.findViewById<EditText>(R.id.overview_addplaylist_name).text.toString()) },
            layoutInflater,
            R.string.overview_action_addplaylist,
            R.drawable.ic_plus_dark,
            R.layout.overview_addplaylist,
            R.string.overview_addplaylist_add,
            R.string.overview_addplaylist_cancel
        )

        fPlaylistList.bindToView(this, fBinding.overviewList)

        fBinding.overviewList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                trigger(position)
            }
        fBinding.overviewList.setMultiChoiceModeListener(object :
            AbsListView.MultiChoiceModeListener {
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                fActionMode = null
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                fActionMode = mode
                mode.menuInflater.inflate(R.menu.overview_contextactions, menu)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.overview_action_up -> {
                        move(true)
                        return true
                    }
                    R.id.overview_action_down -> {
                        move(false)
                        return true
                    }
                    R.id.overview_action_delete -> {
                        fDeleteDialog.show(supportFragmentManager, "delete_playlists")
                        return true
                    }
                    else -> {
                        return false
                    }
                }
            }

            override fun onItemCheckedStateChanged(
                mode: ActionMode,
                position: Int,
                id: Long,
                checked: Boolean
            ) {
            }
        })

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                1
            )
        }

        if (!canReadFolder()) {
            requestSelectFolder()
        }
    }

    private fun canReadFolder(): Boolean {
        val folder = getSettingFolder(this)
        if (folder != null) {
            val folderUri = Uri.parse(folder)
            val folderDocument = DocumentFile.fromTreeUri(this, folderUri)
            return folderDocument?.canRead() ?: false
        }
        return false
    }

    private fun requestSelectFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        getSettingFolder(this)?.let {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
        }
        startActivityForResult(intent, kREQUEST_FOLDER_CODE)
    }

    private fun addPlaylist(name: String) {
        fNewPlaylistId?.let { id ->
            val weight = if (fPlaylists.isEmpty()) 0 else fPlaylists[fPlaylists.size - 1].weight + 1
            val playlist = Playlist(id, name, weight)
            fStore.addPlaylist(playlist)
            openPlaylist(playlist)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                kREQUEST_FOLDER_CODE -> data?.data?.let(::acceptRequestedFolder)
                kREQUEST_DOCUMENT_CODE -> fStore.acceptRequestedDocument(data) { id ->
                    fNewPlaylistId = id
                    fAddPlaylistDialog.show(supportFragmentManager, "addplaylist")
                }
            }
        }
    }

    private fun acceptRequestedFolder(uri: Uri) {
        setSettingFolder(this, uri.toString())

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)

        reloadList()
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getSettingScreenOrientation(this)
        reloadList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.overview_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.overview_action_tap -> {
                TapActivity.startActivity(this)
            }
            R.id.overview_action_settings -> {
                SettingsActivity.startActivity(this)
            }
            R.id.overview_action_folder -> {
                requestSelectFolder()
            }
            /*R.id.overview_action_merge_audio -> {
                MergeAudioActivity.startActivity(this)
            }*/
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun reloadList() {
        fPlaylists.clear()
        fPlaylists.addAll(fStore.readAllPlaylists())
        fPlaylistList.setPlaylists(fPlaylists)
    }

    private fun trigger(selection: Int) {
        openPlaylist(fPlaylists[selection])
    }

    private fun openPlaylist(playlist: PlaylistHeader) {
        fStore.setCurrentPlaylistID(playlist.id)
        PlaylistActivity.startActivity(this)
    }

    private fun deleteSelectedItems() {
        val selection = fBinding.overviewList.checkedItemPositions
        (0 until fPlaylists.size).forEach { i ->
            if (selection.get(i)) {
                fStore.deletePlaylist(fPlaylists[i])
            }
        }
        fActionMode?.finish()
        reloadList()
    }

    private fun move(up: Boolean) {
        val selection = fBinding.overviewList.checkedItemPositions
        var ignore = false
        var i = if (up) 0 else fPlaylists.size - 1
        while (if (up) i < fPlaylists.size else i >= 0) {
            if (selection.get(i)) {
                if (if (up) i <= 0 else i >= fPlaylists.size - 1) {
                    // leave the first selection group alone
                    ignore = true
                } else if (!ignore) {
                    val otherIndex = if (up) i - 1 else i + 1
                    if (!selection.get(otherIndex)) {
                        fBinding.overviewList.setItemChecked(i, false)
                        fBinding.overviewList.setItemChecked(otherIndex, true)
                    }
                    switchPlaylists(otherIndex, i)
                }
            } else {
                ignore = false
            }
            i += if (up) 1 else -1
        }
        reloadList()
    }

    private fun switchPlaylists(index1: Int, index2: Int) {
        val playlist1 = fPlaylists[index1]
        val playlist2 = fPlaylists[index2]
        val tempWeight = playlist1.weight
        playlist1.weight = playlist2.weight
        playlist2.weight = tempWeight
        fPlaylists[index1] = playlist2
        fPlaylists[index2] = playlist1
        fStore.storePlaylistHeader(playlist1)
        fStore.storePlaylistHeader(playlist2)
    }

    companion object {
        private const val kREQUEST_FOLDER_CODE = 2
        private const val kREQUEST_DOCUMENT_CODE = 3

        fun startActivity(context: Context) =
            Intent(context, OverviewActivity::class.java)
                .let(context::startActivity)
    }
}
