package be.t_ars.timekeeper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import be.t_ars.timekeeper.components.SectionsPartComponent
import be.t_ars.timekeeper.components.TapPartComponent
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.ClickDetails
import be.t_ars.timekeeper.databinding.TapSongBinding

class TapSongActivity : AbstractActivity() {
    private lateinit var fBinding: TapSongBinding
    private lateinit var fSectionsPartComponent: SectionsPartComponent
    private lateinit var fTapPartComponent: TapPartComponent
    private var fStereoTrackPath: String? = null
    private var fMonoTrackPath: String? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = TapSongBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fSectionsPartComponent = SectionsPartComponent(this, fBinding.sectionsPart)
        fTapPartComponent = TapPartComponent(this, fBinding.tapPart) { _ -> null }

        fBinding.sectionsButton.setOnClickListener {
            fSectionsPartComponent.show()
        }

        fBinding.selectStereoTrackButton.setOnClickListener {
            startActivityForResult(createSelectStereoTrackRequest(), kREQUEST_STEREO_TRACK_CODE)
        }

        fBinding.clearStereoTrackButton.setOnClickListener {
            setStereoTrack(null)
        }

        fBinding.selectMonoTrackButton.setOnClickListener {
            startActivityForResult(createSelectMonoTrackRequest(), kREQUEST_MONO_TRACK_CODE)
        }

        fBinding.clearMonoTrackButton.setOnClickListener {
            setMonoTrack(null)
        }
    }

    private fun createSelectStereoTrackRequest() =
        createSelectTrackRequest(fStereoTrackPath)

    private fun createSelectMonoTrackRequest() =
        createSelectTrackRequest(fMonoTrackPath)

    private fun createSelectTrackRequest(trackPath: String?) =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            trackPath?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
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
                fBinding.tapPart.textTempo.clearFocus()

                val intent = Intent().also {
                    updateIntent(it)
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

    private fun updateIntent(intent: Intent) {
        val click = fTapPartComponent.getClick()
        fillIntent(
            intent,
            ClickDetails(
                ClickDescription(
                    click.clickDescription.bpm,
                    click.clickDescription.type,
                    click.clickDescription.divisionCount,
                    click.clickDescription.beatCount,
                    click.clickDescription.countOff,
                    click.clickDescription.twoBarCountOff,
                    fSectionsPartComponent.getSections(),
                    fStereoTrackPath,
                    fMonoTrackPath
                ),
                click.stereo,
                click.announceTitle
            ),
            fBinding.name.text.toString(),
            fBinding.scoreLink.text.toString()
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                kREQUEST_STEREO_TRACK_CODE -> acceptStereoTrack(data)
                kREQUEST_MONO_TRACK_CODE -> acceptMonoTrack(data)
            }
        }
    }

    private fun acceptStereoTrack(data: Intent?) =
        acceptTrack(data, ::setStereoTrack)

    private fun acceptMonoTrack(data: Intent?) =
        acceptTrack(data, ::setMonoTrack)

    private fun acceptTrack(data: Intent?, setTrack: (String) -> Unit) {
        data?.data?.let { uri ->
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val trackPath = uri.toString()
            setTrack(trackPath)
            updateIntent(intent)
        }
    }

    private fun setStereoTrack(trackPath: String?) {
        fStereoTrackPath = trackPath
        updateTrackUI(trackPath, fBinding.clearStereoTrackButton, fBinding.stereoTrackText)
    }

    private fun setMonoTrack(trackPath: String?) {
        fMonoTrackPath = trackPath
        updateTrackUI(trackPath, fBinding.clearMonoTrackButton, fBinding.monoTrackText)
    }

    private fun updateTrackUI(trackPath: String?, clearTrackButton: Button, trackText: TextView) {
        clearTrackButton.visibility =
            if (trackPath != null) View.VISIBLE else View.INVISIBLE
        @SuppressLint("UseKtx")
        val filename = trackPath
            ?.let { Uri.parse(it).path }
            ?.let {
                val lastSlash = it.lastIndexOf('/')
                if (lastSlash != -1)
                    it.substring(lastSlash + 1, it.length - 4)
                else
                    it
            } ?: "-"
        trackText.text = filename
    }

    @Suppress("DEPRECATION")
    private fun loadIntent() {
        fBinding.name.setText(intent.getStringExtra(kINTENT_DATA_NAME))

        val newClick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra(kINTENT_DATA_CLICK, ClickDetails::class.java)
        else
            intent.getSerializableExtra(kINTENT_DATA_CLICK) as ClickDetails?
        if (newClick != null) {
            fSectionsPartComponent.setSections(newClick.clickDescription.sections)
            fTapPartComponent.setClick(newClick)
            setStereoTrack(newClick.clickDescription.stereoTrackPath)
            setMonoTrack(newClick.clickDescription.monoTrackPath)
        }

        fBinding.scoreLink.setText(intent.getStringExtra(kINTENT_DATA_SCORE_LINK) ?: "")
    }

    companion object {
        const val kINTENT_DATA_NAME = "name"
        const val kINTENT_DATA_CLICK = "click"
        const val kINTENT_DATA_SCORE_LINK = "score_link"

        private const val kREQUEST_STEREO_TRACK_CODE = 4
        private const val kREQUEST_MONO_TRACK_CODE = 5

        @Suppress("DEPRECATION")
        fun startActivityForResult(
            context: FragmentActivity,
            click: ClickDetails,
            name: String,
            scoreLink: String?,
            requestCode: Int
        ) =
            Intent(context, TapSongActivity::class.java)
                .also { fillIntent(it, click, name, scoreLink) }
                .let { context.startActivityForResult(it, requestCode) }

        private fun fillIntent(
            intent: Intent,
            click: ClickDetails,
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