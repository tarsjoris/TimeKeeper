package be.t_ars.timekeeper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import be.t_ars.timekeeper.components.SectionsPartComponent
import be.t_ars.timekeeper.components.TapPartComponent
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.databinding.TapSongBinding

class TapSongActivity : AbstractActivity() {
    private lateinit var fBinding: TapSongBinding
    private lateinit var fSectionsPartComponent: SectionsPartComponent
    private lateinit var fTapPartComponent: TapPartComponent
    private var fTrackPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = TapSongBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fSectionsPartComponent = SectionsPartComponent(this, fBinding.sectionsPart)
        fTapPartComponent = TapPartComponent(fBinding.tapPart) {}

        fBinding.sectionsButton.setOnClickListener {
            fSectionsPartComponent.show()
        }

        fBinding.selectTrackButton.setOnClickListener {
            startActivityForResult(createSelectTrackRequest(), kREQUEST_TRACK_CODE)
        }

        fBinding.clearTrackButton.setOnClickListener {
            setTrack(null)
        }
    }

    private fun createSelectTrackRequest() =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            fTrackPath?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }


    override fun onResume() {
        super.onResume()
        requestedOrientation = getSettingScreenOrientation(this)
        loadIntent()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tap_song_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tap_action_accept -> {
                fBinding.tapPart.tempoSpinner.clearFocus()

                val intent = Intent().also {
                    updateIntent(it, fTrackPath)
                }
                setResult(RESULT_OK, intent)
                finish()
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun updateIntent(intent: Intent, trackPath: String?) {
        val click = fTapPartComponent.getClick()
        fillIntent(
            intent,
            ClickDescription(
                click.bpm,
                click.type,
                click.divisionCount,
                click.beatCount,
                click.countOff,
                fSectionsPartComponent.getSections(),
                trackPath
            ),
            fBinding.name.text.toString(),
            fBinding.scoreLink.text.toString()
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                kREQUEST_TRACK_CODE -> acceptTrack(data)
            }
        }
    }

    private fun acceptTrack(data: Intent?) {
        data?.data?.let { uri ->
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val trackPath = uri.toString()
            updateIntent(intent, trackPath)
            setTrack(trackPath)
        }
    }

    private fun setTrack(trackPath: String?) {
        println(trackPath)
        fTrackPath = trackPath
        fBinding.clearTrackButton.visibility =
            if (trackPath != null) View.VISIBLE else View.INVISIBLE
        val filename = trackPath
            ?.let { Uri.parse(it).path }
            ?.let {
                val lastSlash = it.lastIndexOf('/')
                if (lastSlash != -1)
                    it.substring(lastSlash + 1, it.length - 4)
                else
                    it
            } ?: "-"
        println(filename)
        fBinding.trackText.text = filename
    }

    private fun loadIntent() {
        fBinding.name.setText(intent.getStringExtra(kINTENT_DATA_NAME))

        val newClick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra(kINTENT_DATA_CLICK, ClickDescription::class.java)
        else
            intent.getSerializableExtra(kINTENT_DATA_CLICK) as ClickDescription
        if (newClick != null) {
            fSectionsPartComponent.setSections(newClick.sections)
            fTapPartComponent.setClick(newClick)
            setTrack(newClick.trackPath)
        }

        fBinding.scoreLink.setText(intent.getStringExtra(kINTENT_DATA_SCORE_LINK) ?: "")
    }

    companion object {
        const val kINTENT_DATA_NAME = "name"
        const val kINTENT_DATA_CLICK = "click"
        const val kINTENT_DATA_SCORE_LINK = "score_link"

        private const val kREQUEST_TRACK_CODE = 4

        fun startActivityForResult(
            context: FragmentActivity,
            click: ClickDescription,
            name: String,
            scoreLink: String?,
            requestCode: Int
        ) =
            Intent(context, TapSongActivity::class.java)
                .also { fillIntent(it, click, name, scoreLink) }
                .let { context.startActivityForResult(it, requestCode) }

        private fun fillIntent(
            intent: Intent,
            click: ClickDescription,
            name: String,
            scoreLink: String?
        ) {
            intent.putExtra(kINTENT_DATA_NAME, name)
            intent.putExtra(kINTENT_DATA_CLICK, click)
            if (scoreLink != null)
                intent.putExtra(kINTENT_DATA_SCORE_LINK, scoreLink)
        }
    }
}