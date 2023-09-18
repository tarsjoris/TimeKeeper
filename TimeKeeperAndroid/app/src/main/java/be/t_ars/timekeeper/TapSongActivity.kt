package be.t_ars.timekeeper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import be.t_ars.timekeeper.components.TapPartComponent
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.databinding.TapSongBinding

class TapSongActivity : AbstractActivity() {
    private lateinit var fBinding: TapSongBinding
    private lateinit var fTapPartComponent: TapPartComponent
    private var fTrackPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = TapSongBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fTapPartComponent = TapPartComponent(fBinding.tapPart, this::startSound, this::stopSound)

        fBinding.selectTrackButton.setOnClickListener {
            startActivityForResult(createSelectTrackRequest(), TapSongActivity.kREQUEST_TRACK_CODE)
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
        fillIntent(
            intent,
            ClickDescription(
                fTapPartComponent.getTempo(),
                fTapPartComponent.getClickType(),
                fTapPartComponent.getDivisionCount(),
                fTapPartComponent.getBeatCount(),
                fTapPartComponent.isCountOff(),
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

        val newTempo = intent.getIntExtra(kINTENT_DATA_TEMPO, ClickDescription.DEFAULT_TEMPO)
        fTapPartComponent.setTempo(newTempo)

        val newClickType = intent.getIntExtra(kINTENT_DATA_CLICK_TYPE, EClickType.DEFAULT.value)
            .let(EClickType::of)
        fTapPartComponent.setClickType(newClickType)

        val newDivisionCount = intent.getIntExtra(
            kINTENT_DATA_DIVISION_COUNT,
            ClickDescription.DEFAULT_DIVISION_COUNT
        )
        fTapPartComponent.setDivisionCount(newDivisionCount)

        val newBeatCount = intent.getIntExtra(
            kINTENT_DATA_BEAT_COUNT,
            ClickDescription.DEFAULT_BEAT_COUNT
        )
        fTapPartComponent.setBeatCount(newBeatCount)

        val newCountOff = intent.getBooleanExtra(
            kINTENT_DATA_COUNT_OFF,
            ClickDescription.DEFAULT_COUNT_OFF
        )
        fTapPartComponent.setCountOff(newCountOff)

        val newTrackPath = intent.getStringExtra(kINTENT_DATA_TRACK_PATH)
        setTrack(newTrackPath)

        fBinding.scoreLink.setText(intent.getStringExtra(kINTENT_DATA_SCORE_LINK) ?: "")
    }

    private fun startSound(click: ClickDescription) {
        SoundService.startSound(this, null, click)
    }

    private fun stopSound() {
        SoundService.stopSound(this)
    }


    companion object {
        const val kINTENT_DATA_TEMPO = "tempo"
        const val kINTENT_DATA_CLICK_TYPE = "click_type"
        const val kINTENT_DATA_DIVISION_COUNT = "division_count"
        const val kINTENT_DATA_BEAT_COUNT = "beat_count"
        const val kINTENT_DATA_COUNT_OFF = "count_off"
        const val kINTENT_DATA_NAME = "name"
        const val kINTENT_DATA_TRACK_PATH = "track_path"
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
            intent.putExtra(kINTENT_DATA_TEMPO, click.bpm)
            intent.putExtra(kINTENT_DATA_CLICK_TYPE, click.type.value)
            intent.putExtra(kINTENT_DATA_DIVISION_COUNT, click.divisionCount)
            intent.putExtra(kINTENT_DATA_BEAT_COUNT, click.beatCount)
            intent.putExtra(kINTENT_DATA_COUNT_OFF, click.countOff)
            if (click.trackPath != null)
                intent.putExtra(kINTENT_DATA_TRACK_PATH, click.trackPath)
            intent.putExtra(kINTENT_DATA_NAME, name)
            if (scoreLink != null)
                intent.putExtra(kINTENT_DATA_SCORE_LINK, scoreLink)
        }
    }
}